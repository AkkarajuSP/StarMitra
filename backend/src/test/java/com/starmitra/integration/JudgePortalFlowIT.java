package com.starmitra.integration;

import com.starmitra.modules.competition.application.CompetitionService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.judge.application.JudgeService;
import com.starmitra.modules.rubric.application.RubricService;
import com.starmitra.modules.rubric.persistence.EvaluationTemplateVersionRepository;
import com.starmitra.modules.submission.application.SubmissionService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M20 on real PG17 — judge portal seam: scope-checked rubric + evaluation. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JudgePortalFlowIT {

    @Autowired OtpService otpService;
    @Autowired JudgeService judges;
    @Autowired CompetitionService competitions;
    @Autowired SubmissionService submissions;
    @Autowired RubricService rubrics;
    @Autowired EvaluationTemplateVersionRepository versions;

    private UUID admin, judgeA, judgeB, organizer, contestant;

    private UUID register(String tag) {
        String email = tag + UUID.randomUUID().toString().substring(0, 6) + "@t.dev";
        otpService.request("EMAIL", email);
        return otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
    }

    private void asAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private void asJudge(UUID uid) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(uid.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_JUDGE"))));
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        admin = register("m20ad");
        judgeA = register("m20ja");
        judgeB = register("m20jb");
        organizer = register("m20o");
        contestant = register("m20c");
        asAdmin();
    }

    private UUID latestDraftId(UUID templateId) {
        return versions.draftsOf(templateId).stream().findFirst().orElseThrow().getId();
    }

    private record Env(UUID compId, UUID roundId, UUID subId, UUID rubricVersionId) {}
    private Env env() {
        var t = rubrics.createTemplate(admin, "Vocal", List.of(
                new RubricService.CriterionCmd("Technique", null, new BigDecimal("60"), null),
                new RubricService.CriterionCmd("Expression", null, new BigDecimal("40"), null)),
                0, 10, null, null, null);
        var rvId = rubrics.publish(admin, latestDraftId(t.id())).id();
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Idol", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, rvId, null));
        var p = competitions.register(contestant, compId, "USER", cat.id(), null);
        var s = submissions.create(contestant, p.id(), round.id());
        submissions.submit(contestant, s.id());
        submissions.finalize(contestant, s.id());
        return new Env(compId, round.id(), s.id(), rvId);
    }

    @Test
    void rubricResolvedThroughM13ScopeCheckedByM12() {
        var e = env();
        var ja = judges.createJudge(admin, judgeA);
        judges.assign(admin, ja.id(),
                new JudgeService.AssignmentCmd(e.compId(), null, e.roundId()));

        asJudge(judgeA);
        var r = judges.myRubric(judgeA, e.roundId());
        assertEquals(e.rubricVersionId(), r.id());
        assertEquals("PUBLISHED", r.status());
        assertEquals(2, r.criteria().size());

        // unassigned judge → CROSS_SCOPE_DENIED
        asAdmin();
        judges.createJudge(admin, judgeB);
        asJudge(judgeB);
        var ex = assertThrows(ApiException.class, () -> judges.myRubric(judgeB, e.roundId()));
        assertEquals(ErrorCode.CROSS_SCOPE_DENIED, ex.code());
        asAdmin();
    }

    @Test
    void judgeEvaluatesWithinScopeOnly() {
        var e = env();
        var ja = judges.createJudge(admin, judgeA);
        judges.createJudge(admin, judgeB);
        judges.assign(admin, ja.id(),
                new JudgeService.AssignmentCmd(e.compId(), null, e.roundId()));
        var crit = rubrics.publishedVersion(e.rubricVersionId()).orElseThrow().criteria();

        asJudge(judgeA);
        var ev = rubrics.submitEvaluation(judgeA, e.subId(), e.rubricVersionId(),
                crit.stream().map(c -> new RubricService.ScoreCmd(c.id(),
                        new BigDecimal("8"), null)).toList());
        assertEquals("SUBMITTED", ev.status());
        // duplicate evaluation → idempotent replay (DB-09), same evaluation returned
        var replay = rubrics.submitEvaluation(judgeA, e.subId(), e.rubricVersionId(),
                crit.stream().map(c -> new RubricService.ScoreCmd(c.id(), BigDecimal.ONE, null))
                        .toList());
        assertEquals(ev.id(), replay.id());

        // judge B: out of scope → evaluation denied
        asJudge(judgeB);
        assertThrows(ApiException.class, () -> rubrics.submitEvaluation(judgeB,
                e.subId(), e.rubricVersionId(), crit.stream().map(c ->
                        new RubricService.ScoreCmd(c.id(), BigDecimal.ONE, null)).toList()));
        // judge independence: B cannot read A's evaluation
        assertThrows(ApiException.class, () -> rubrics.getEvaluation(judgeB, ev.id()));
        asAdmin();
    }

    @Test
    void noRubricForRoundIs404() {
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("X", null)).id();
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, null, null));
        var ja = judges.createJudge(admin, judgeA);
        judges.assign(admin, ja.id(),
                new JudgeService.AssignmentCmd(compId, null, round.id()));
        asJudge(judgeA);
        assertThrows(ApiException.class, () -> judges.myRubric(judgeA, round.id()));
        asAdmin();
    }
}
