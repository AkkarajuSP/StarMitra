package com.starmitra.integration;

import com.starmitra.modules.competition.application.CompetitionService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.judge.application.JudgeService;
import com.starmitra.modules.leaderboard.application.LeaderboardService;
import com.starmitra.modules.progression.application.ProgressionService;
import com.starmitra.modules.rubric.application.RubricService;
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

/** M16 on real PG17 — publication-gated leaderboard projections over M14/M15 truth. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LeaderboardFlowIT {

    @Autowired OtpService otpService;
    @Autowired LeaderboardService leaderboards;
    @Autowired ProgressionService progression;
    @Autowired ScoringService scoring;
    @Autowired RubricService rubrics;
    @Autowired JudgeService judges;
    @Autowired CompetitionService competitions;
    @Autowired SubmissionService submissions;
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
        admin = register("m16ad");
        judgeUser = register("m16j");
        organizer = register("m16o");
        c1 = register("m16c1");
        c2 = register("m16c2");
        asAdmin();
    }

    /** Sealed scoring for round: s1 rank1 / s2 rank2; TOP_N=1; projection source ready. */
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
        progression.createConfig(compId, Map.of("maxAdvance", 1));
        progression.calculate(admin, compId, round.id(), null);
        return new Env(compId, cat.id(), round.id(), s1.id(), s2.id());
    }

    @Test
    void publicationGatedLeaderboard() {
        var e = env();
        // unpublished → hidden (NOT_FOUND, never leaks)
        assertThrows(ApiException.class, () -> leaderboards.leaderboard(
                e.compId(), e.catId(), e.roundId(), null, 20));
        var pubId = leaderboards.publish(admin, e.compId(), e.catId(), e.roundId(), "PUBLISH");
        var items = leaderboards.leaderboard(e.compId(), e.catId(), e.roundId(), null, 20);
        assertEquals(2, items.size());
        assertEquals(1, items.get(0).rank());            // M14 rank verbatim — never recalculated
        assertEquals(e.s1(), items.get(0).entryRef());
        assertEquals("90.0000", items.get(0).display().get("finalScore"));
        assertEquals("ADVANCED", items.get(0).display().get("progression"));
        assertEquals(2, items.get(1).rank());
        assertEquals("ELIMINATED", items.get(1).display().get("progression"));
        // HIDE → gated again
        leaderboards.publish(admin, e.compId(), e.catId(), e.roundId(), "HIDE");
        assertThrows(ApiException.class, () -> leaderboards.leaderboard(
                e.compId(), e.catId(), e.roundId(), null, 20));
        // snapshot is admin-only but succeeds
        leaderboards.snapshot(admin, pubId);
    }

    @Test
    void m16NeverRecalculatesRank() {
        var e = env();
        leaderboards.publish(admin, e.compId(), e.catId(), e.roundId(), "PUBLISH");
        var items = leaderboards.leaderboard(e.compId(), e.catId(), e.roundId(), null, 20);
        // rank comes straight from M14's snapshot — M16 just projects it
        assertEquals(scoring.latestRanking(e.roundId()).get(e.s1()),
                items.stream().filter(i -> i.entryRef().equals(e.s1()))
                        .findFirst().orElseThrow().rank());
    }

    @Test
    void adminOnlyPublicationAndNoRankingMutation() {
        var e = env();
        SecurityContextHolder.clearContext();          // unauthenticated → denied
        assertThrows(ApiException.class, () -> leaderboards.publish(
                UUID.randomUUID(), e.compId(), e.catId(), e.roundId(), "PUBLISH"));
        asAdmin();
        // wrong competition → round rejected (cross-scope)
        assertThrows(ApiException.class, () -> leaderboards.publish(
                admin, UUID.randomUUID(), e.catId(), e.roundId(), "PUBLISH"));
        // no mutation surface exists on projections (read-only entity)
    }

    @Test
    void compWideScopeAllCategories() {
        var e = env();
        // publish comp-level (cat=null, round set) → all categories' rows surface
        leaderboards.publish(admin, e.compId(), null, e.roundId(), "PUBLISH");
        var items = leaderboards.leaderboard(e.compId(), null, e.roundId(), null, 20);
        assertEquals(2, items.size());
    }

    @Test
    void cursorPaginationDeterministic() {
        var e = env();
        leaderboards.publish(admin, e.compId(), e.catId(), e.roundId(), "PUBLISH");
        var page1 = leaderboards.leaderboard(e.compId(), e.catId(), e.roundId(), null, 1);
        assertEquals(1, page1.size());
        assertEquals(1, page1.get(0).rank());
        // cursor "rank:1" → next rows
        var cursor = com.starmitra.platform.pagination.Cursor.encode("rank", "1");
        var page2 = leaderboards.leaderboard(e.compId(), e.catId(), e.roundId(), cursor, 10);
        assertEquals(1, page2.size());
        assertEquals(2, page2.get(0).rank());
    }
}
