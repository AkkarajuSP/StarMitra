package com.starmitra.integration;

import com.starmitra.modules.competition.application.CompetitionService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.judge.application.JudgeService;
import com.starmitra.modules.rubric.application.RubricService;
import com.starmitra.modules.scoring.application.ScoringService;
import com.starmitra.modules.submission.application.SubmissionService;
import com.starmitra.modules.voting.application.VoteService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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

/** M14 on real PG17 — weighted scoring, ranking, qualification, overrides. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScoringFlowIT {

    @Autowired OtpService otpService;
    @Autowired ScoringService scoring;
    @Autowired RubricService rubrics;
    @Autowired JudgeService judges;
    @Autowired CompetitionService competitions;
    @Autowired SubmissionService submissions;
    @Autowired VoteService votes;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID admin, judgeUser, organizer, c1, c2, voter;

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
        admin = register("m14ad");
        judgeUser = register("m14j");
        organizer = register("m14o");
        c1 = register("m14c1");
        c2 = register("m14c2");
        voter = register("m14v");
        asAdmin();
    }

    /** Comp + published rubric (50/50) + round + two finalized submissions + judge evals. */
    private record Env(UUID compId, UUID catId, UUID roundId, UUID cfgId,
                       UUID s1, UUID s2, UUID rubricV) {}
    private Env env() {
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Idol", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var t = rubrics.createTemplate(admin, "Vocal", List.of(
                        new RubricService.CriterionCmd("Tech", null, new BigDecimal("50"), null),
                        new RubricService.CriterionCmd("Expr", null, new BigDecimal("50"), null)),
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
        // s1: 10+10 → judge%=100; s2: 5+5 → judge%=50
        rubrics.submitEvaluation(judgeUser, s1.id(), draftId, rubric.criteria().stream()
                .map(c -> new RubricService.ScoreCmd(c.id(), new BigDecimal("10"), null)).toList());
        rubrics.submitEvaluation(judgeUser, s2.id(), draftId, rubric.criteria().stream()
                .map(c -> new RubricService.ScoreCmd(c.id(), new BigDecimal("5"), null)).toList());
        asAdmin();
        var cfgId = scoring.createScoringConfig(admin, compId,
                new BigDecimal("40"), new BigDecimal("60"));
        return new Env(compId, cat.id(), round.id(), cfgId, s1.id(), s2.id(), draftId);
    }

    @Test
    void weightedScoringAndRanking() {
        var e = env();
        // s1 gets 2 votes, s2 gets 1 → audience 100/50; judge 100/50
        votes.cast(voter, e.s1(), "sc-1");
        votes.cast(c1, e.s1(), "sc-2");       // contestants can vote (no restriction configured)
        votes.cast(voter, e.s2(), "sc-3");

        scoring.calculate(admin, e.compId(), e.roundId(), e.cfgId());
        scoring.finalize(admin, e.compId(), e.roundId());

        var rank = scoring.latestRanking(e.roundId());
        assertEquals(1, rank.get(e.s1()));                  // s1 > s2 on both components
        assertEquals(2, rank.get(e.s2()));
        em.flush();
        var fs = jdbc.queryForObject(
                "select final_score from final_scores where submission_id=? order by score_version desc limit 1",
                BigDecimal.class, e.s1());
        assertEquals(0, new BigDecimal("100.0000").compareTo(fs));   // 100*0.4 + 100*0.6
        assertTrue(scoring.isSealed(e.roundId()));
    }

    @Test
    void configWeightsMustTotal100() {
        var e = env();
        assertThrows(ApiException.class, () -> scoring.createScoringConfig(admin, e.compId(),
                new BigDecimal("40"), new BigDecimal("40")));
    }

    @Test
    void qualificationAndTieBreak() {
        var e = env();
        // TOP_N=1 → only rank-1 qualifies
        scoring.createQualificationConfig(e.compId(), Map.of("type", "TOP_N", "n", 1));
        scoring.calculate(admin, e.compId(), e.roundId(), e.cfgId());
        scoring.finalize(admin, e.compId(), e.roundId());
        var qual = scoring.qualificationOf(e.roundId());
        assertEquals(Boolean.TRUE, qual.get(e.s1()));
        assertEquals(Boolean.FALSE, qual.get(e.s2()));
    }

    @Test
    void overrideIsAuditedAppendOnly() {
        var e = env();
        scoring.calculate(admin, e.compId(), e.roundId(), e.cfgId());
        scoring.finalize(admin, e.compId(), e.roundId());
        em.flush();
        var fsId = jdbc.queryForObject(
                "select id from final_scores where submission_id=? order by score_version desc limit 1",
                UUID.class, e.s1());
        scoring.override(admin, fsId, new BigDecimal("90.0000"), "judge appeal upheld");
        em.flush();
        var rows = jdbc.queryForList(
                "select before_value, after_value, reason, actor_id from score_overrides where final_score_id=?",
                fsId);
        assertEquals(1, rows.size());
        assertEquals("judge appeal upheld", rows.get(0).get("reason"));
        // source final_scores row UNTOUCHED (before value preserved, row still sealed)
        var stillSealed = jdbc.queryForObject(
                "select status from final_scores where id=?", String.class, fsId);
        assertEquals("SEALED", stillSealed);
        var effective = scoring.results(e.compId(), e.roundId()).stream()
                .filter(r -> r.submissionId().equals(e.s1())).findFirst().orElseThrow();
        assertTrue(effective.overridden());
    }

    @Test
    void sealedScoresNotRecalculatedInPlace() {
        var e = env();
        scoring.calculate(admin, e.compId(), e.roundId(), e.cfgId());
        scoring.finalize(admin, e.compId(), e.roundId());
        // new config version (50/50) → recalculation produces NEW score_version rows,
        // sealed v1 rows untouched
        var cfg2 = scoring.createScoringConfig(admin, e.compId(),
                new BigDecimal("50"), new BigDecimal("50"));
        scoring.calculate(admin, e.compId(), e.roundId(), cfg2);
        em.flush();
        assertTrue(jdbc.queryForObject(
                "select count(*) from final_scores where round_id=? and status='SEALED'",
                Integer.class, e.roundId()) >= 2);
    }
}
