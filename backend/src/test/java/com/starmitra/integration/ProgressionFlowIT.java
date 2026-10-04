package com.starmitra.integration;

import com.starmitra.modules.competition.application.CompetitionService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.judge.application.JudgeService;
import com.starmitra.modules.progression.application.ProgressionService;
import com.starmitra.modules.rubric.application.RubricService;
import com.starmitra.modules.room.application.RoomService;
import com.starmitra.modules.scoring.application.ScoringService;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M15 on real PG17 — round lifecycle, qualification-driven progression, idempotency. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProgressionFlowIT {

    @Autowired OtpService otpService;
    @Autowired ProgressionService progression;
    @Autowired ScoringService scoring;
    @Autowired RubricService rubrics;
    @Autowired JudgeService judges;
    @Autowired CompetitionService competitions;
    @Autowired SubmissionService submissions;
    @Autowired RoomService rooms;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID admin, judgeUser, organizer, c1, c2;

    private UUID register(String tag) {
        String email = tag + UUID.randomUUID().toString().substring(0, 6) + "@t.dev";
        otpService.request("EMAIL", email);
        return otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
    }

    private void asAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(admin.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private void asJudge(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(userId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_JUDGE"))));
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        admin = register("m15ad");
        judgeUser = register("m15j");
        organizer = register("m15o");
        c1 = register("m15c1");
        c2 = register("m15c2");
        asAdmin();
    }

    /** Comp + sealed scoring for round1: s1 (rank1, qualified) and s2 (rank2, not). */
    private record Env(UUID compId, UUID catId, UUID roundId, UUID s1, UUID s2) {}
    private Env env() {
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Idol", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var t = rubrics.createTemplate(admin, "Vocal", List.of(
                        new RubricService.CriterionCmd("Tech", null, new BigDecimal("100"), null)),
                1, 10, compId, null, null);
        var draftId = rubrics.updateTemplate(t.id(), null, null, null, null, null, null).id();
        rubrics.publish(admin, draftId);
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, draftId, null));
        var p1 = competitions.register(c1, compId, "USER", cat.id(), null);
        var p2 = competitions.register(c2, compId, "USER", cat.id(), null);
        var s1 = submissions.create(c1, p1.id(), round.id());
        var s2 = submissions.create(c2, p2.id(), round.id());
        submissions.submit(c1, s1.id());
        submissions.finalize(c1, s1.id());
        submissions.submit(c2, s2.id());
        submissions.finalize(c2, s2.id());

        var judge = judges.createJudge(admin, judgeUser);
        judges.assign(admin, judge.id(), new JudgeService.AssignmentCmd(compId, cat.id(), round.id()));
        var rubric = rubrics.publishedVersion(draftId).orElseThrow();
        asJudge(judgeUser);
        rubrics.submitEvaluation(judgeUser, s1.id(), draftId, List.of(
                new RubricService.ScoreCmd(rubric.criteria().get(0).id(), new BigDecimal("9"), null)));
        rubrics.submitEvaluation(judgeUser, s2.id(), draftId, List.of(
                new RubricService.ScoreCmd(rubric.criteria().get(0).id(), new BigDecimal("4"), null)));
        asAdmin();
        scoring.createScoringConfig(admin, compId, new BigDecimal("0"), new BigDecimal("100"));
        scoring.createQualificationConfig(compId, Map.of("type", "TOP_N", "n", 1));
        scoring.calculate(admin, compId, round.id(), null);
        scoring.finalize(admin, compId, round.id());
        return new Env(compId, cat.id(), round.id(), s1.id(), s2.id());
    }

    @Test
    void roundLifecycleViaM09Contract() {
        var e = env();
        // finalize already seals → round completion requires ACTIVE state
        assertThrows(ApiException.class, () -> progression.complete(admin, e.compId(), e.roundId()));
        progression.activate(admin, e.compId(), e.roundId());
        assertEquals("ACTIVE", progression.roundStateOf(e.compId()));
        progression.complete(admin, e.compId(), e.roundId());
        assertEquals("COMPLETE", progression.roundStateOf(e.compId()));
        // COMPLETE → anything else denied
        assertThrows(ApiException.class, () -> progression.activate(admin, e.compId(), e.roundId()));
    }

    @Test
    void progressionConsumesM14Qualification() {
        var e = env();
        var cfg = progression.createConfig(e.compId(), Map.of("maxAdvance", 1));
        int n = progression.calculate(admin, e.compId(), e.roundId(), cfg);
        assertEquals(2, n);
        var recs = progression.records(e.roundId());
        var bySub = new java.util.HashMap<UUID, String>();
        recs.forEach(r -> bySub.put(r.submissionId(), r.outcome()));
        assertEquals("ADVANCED", bySub.get(e.s1()));       // rank-1 qualified
        assertEquals("ELIMINATED", bySub.get(e.s2()));
        // idempotent — re-run adds nothing
        assertEquals(0, progression.calculate(admin, e.compId(), e.roundId(), cfg));
    }

    @Test
    void finalizeTransitionsRoundAndMarksRecords() {
        var e = env();
        progression.activate(admin, e.compId(), e.roundId());
        progression.createConfig(e.compId(), Map.of("maxAdvance", 1));
        progression.calculate(admin, e.compId(), e.roundId(), null);
        int n = progression.finalize(admin, e.compId(), e.roundId());
        assertEquals(2, n);
        assertEquals("COMPLETE", progression.roundStateOf(e.compId()));
        assertEquals(1, progression.advancedFrom(e.roundId()).size());
        assertEquals(e.s1(), progression.advancedFrom(e.roundId()).get(0));
    }

    @Test
    void completeRequiresSealedScoring() {
        // no scoring calculated → completion denied
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Fresh", null)).id();
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R", null, null, null, null, null));
        progression.activate(admin, compId, round.id());
        assertThrows(ApiException.class, () -> progression.complete(admin, compId, round.id()));
    }

    @Test
    void adminOnlyAndNoClientForgery() {
        var e = env();
        SecurityContextHolder.clearContext();          // no ADMIN role
        assertThrows(ApiException.class, () -> progression.activate(admin, e.compId(), e.roundId()));
        assertThrows(ApiException.class,
                () -> progression.createConfig(e.compId(), Map.of()));
        asAdmin();
        // judge can't progress
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(judgeUser.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_JUDGE"))));
        assertThrows(ApiException.class,
                () -> progression.calculate(admin, e.compId(), e.roundId(), null));
        asAdmin();
        // wrong competition + valid round → rejected (never cross-comp)
        assertThrows(ApiException.class, () -> progression.calculate(admin,
                UUID.randomUUID(), e.roundId(), null));
    }
}
