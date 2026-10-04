package com.starmitra.integration;

import com.starmitra.modules.competition.application.CompetitionService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.judge.application.JudgeService;
import com.starmitra.modules.rubric.application.RubricService;
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

/** M13 on real PG17 — versioned rubrics, 100% weights, immutability, scoped evaluations. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RubricFlowIT {

    @Autowired OtpService otpService;
    @Autowired RubricService rubrics;
    @Autowired JudgeService judges;
    @Autowired CompetitionService competitions;
    @Autowired SubmissionService submissions;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID admin, judgeUser, otherJudge, organizer, contestant;

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
        admin = register("m13ad");
        judgeUser = register("m13j");
        otherJudge = register("m13j2");
        organizer = register("m13o");
        contestant = register("m13c");
        asAdmin();
    }

    private List<RubricService.CriterionCmd> criteria(String aW, String bW) {
        return List.of(
                new RubricService.CriterionCmd("Technique", null, new BigDecimal(aW), null),
                new RubricService.CriterionCmd("Expression", null, new BigDecimal(bW), null));
    }

    @Test
    void publishRequires100PercentWeights() {
        var t = rubrics.createTemplate(admin, "Vocal", criteria("60", "40"), null, null, null, null, null);
        var v1 = rubrics.publish(admin, latestDraftId(t.id()));
        assertEquals("PUBLISHED", v1.status());
        assertTrue(rubrics.publishedVersion(v1.id()).isPresent());
        assertEquals(2, rubrics.publishedVersion(v1.id()).get().criteria().size());
        assertEquals(10, rubrics.publishedVersion(v1.id()).get().scaleMax());   // default scale
    }

    @Test
    void rejectsBadWeightTotal() {
        var t = rubrics.createTemplate(admin, "Bad", criteria("60", "30"), null, null, null, null, null);
        var e = assertThrows(ApiException.class, () -> rubrics.publish(admin, latestDraftId(t.id())));
        assertEquals(ErrorCode.VALIDATION_FAILED, e.code());
    }

    @Test
    void publishedIsImmutableNewDraftCreated() {
        var t = rubrics.createTemplate(admin, "Vocal", criteria("60", "40"), null, null, null, null, null);
        var v1Id = latestDraftId(t.id());
        rubrics.publish(admin, v1Id);
        // editing a published template → creates NEW DRAFT v2, never mutates v1
        var v2 = rubrics.updateTemplate(t.id(), criteria("50", "50"), null, null, null, null, null);
        assertEquals(2, v2.versionNo());
        assertEquals("DRAFT", v2.status());
        // v1 remains PUBLISHED with original weights
        var v1 = rubrics.publishedVersion(v1Id).orElseThrow();
        assertEquals(0, new BigDecimal("60.00")
                .compareTo(v1.criteria().get(0).weight()));
        // re-publish already published → rejected
        assertThrows(ApiException.class, () -> rubrics.publish(admin, v1Id));
    }

    @Test
    void scopedEvaluationLifecycle() {
        // competition + finalized submission
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Idol", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));

        // rubric scoped to the competition, published
        var t = rubrics.createTemplate(admin, "Vocal", criteria("50", "50"), 1, 10, compId, null, null);
        var vId = latestDraftId(t.id());
        rubrics.publish(admin, vId);
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, vId, null));

        var p = competitions.register(contestant, compId, "USER", cat.id(), null);
        var s = submissions.create(contestant, p.id(), round.id());
        submissions.submit(contestant, s.id());
        submissions.finalize(contestant, s.id());

        var judge = judges.createJudge(admin, judgeUser);
        judges.createJudge(admin, otherJudge);                       // unassigned control judge
        judges.assign(admin, judge.id(),
                new JudgeService.AssignmentCmd(compId, cat.id(), round.id()));
        var rubric = rubrics.publishedVersion(vId).orElseThrow();
        var scoreCmds = rubric.criteria().stream()
                .map(c -> new RubricService.ScoreCmd(c.id(), new BigDecimal("8"), null)).toList();

        // judge in-scope → evaluation SUBMITTED
        asJudge(judgeUser);
        var ev = rubrics.submitEvaluation(judgeUser, s.id(), vId, scoreCmds);
        assertEquals("SUBMITTED", ev.status());
        // replay → same evaluation (DB-09)
        assertEquals(ev.id(), rubrics.submitEvaluation(judgeUser, s.id(), vId, scoreCmds).id());
        // unassigned judge → denied
        asJudge(otherJudge);
        assertThrows(ApiException.class, () -> rubrics.submitEvaluation(otherJudge, s.id(), vId, scoreCmds));
        asAdmin();
    }

    @Test
    void evaluationValidation() {
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Idol", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var t = rubrics.createTemplate(admin, "Vocal", criteria("50", "50"), null, null, compId, null, null);
        var vId = latestDraftId(t.id());
        rubrics.publish(admin, vId);
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, vId, null));
        var p = competitions.register(contestant, compId, "USER", cat.id(), null);
        var s = submissions.create(contestant, p.id(), round.id());
        submissions.submit(contestant, s.id());
        submissions.finalize(contestant, s.id());
        var judge = judges.createJudge(admin, judgeUser);
        judges.assign(admin, judge.id(), new JudgeService.AssignmentCmd(compId, cat.id(), round.id()));

        var rubric = rubrics.publishedVersion(vId).orElseThrow();
        asJudge(judgeUser);
        // score above max (default scale 10) → rejected
        assertThrows(ApiException.class, () -> rubrics.submitEvaluation(judgeUser, s.id(), vId,
                List.of(new RubricService.ScoreCmd(rubric.criteria().get(0).id(),
                        new BigDecimal("99"), null),
                        new RubricService.ScoreCmd(rubric.criteria().get(1).id(),
                                new BigDecimal("8"), null))));
        // missing criterion → rejected
        assertThrows(ApiException.class, () -> rubrics.submitEvaluation(judgeUser, s.id(), vId,
                List.of(new RubricService.ScoreCmd(rubric.criteria().get(0).id(),
                        new BigDecimal("8"), null))));
        // non-finalized target → rejected (round 2; state check precedes scope)
        var round2 = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(2, "R2", null, null, null, vId, null));
        var s2 = submissions.create(contestant, p.id(), round2.id());
        assertThrows(ApiException.class, () -> rubrics.submitEvaluation(judgeUser, s2.id(), vId,
                rubric.criteria().stream()
                        .map(c -> new RubricService.ScoreCmd(c.id(), new BigDecimal("5"), null))
                        .toList()));
    }

    @Test
    void m14ContractSeam() {
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("X", null)).id();
        var t = rubrics.createTemplate(admin, "R", criteria("70", "30"), 1, 5, compId, null, null);
        var vId = latestDraftId(t.id());
        rubrics.publish(admin, vId);
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null, null, null, vId, null));
        // resolveForRound → published version with criteria + scale
        var resolved = rubrics.resolveForRound(round.id()).orElseThrow();
        assertEquals(vId, resolved.id());
        assertEquals(5, resolved.scaleMax());
        assertEquals("PUBLISHED", resolved.status());
    }

    private UUID latestDraftId(UUID templateId) {
        return rubrics.updateTemplate(templateId, null, null, null, null, null, null).id();
    }
}
