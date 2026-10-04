package com.starmitra.integration;

import com.starmitra.modules.competition.application.CompetitionService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.judge.application.JudgeService;
import com.starmitra.modules.leaderboard.application.LeaderboardService;
import com.starmitra.modules.media.application.MediaService;
import com.starmitra.modules.moderation.application.ModerationService;
import com.starmitra.modules.notification.application.NotificationService;
import com.starmitra.modules.portfolio.application.PortfolioService;
import com.starmitra.modules.profile.application.ProfileService;
import com.starmitra.modules.progression.application.ProgressionService;
import com.starmitra.modules.room.application.RoomService;
import com.starmitra.modules.rubric.application.RubricService;
import com.starmitra.modules.rubric.persistence.EvaluationTemplateVersionRepository;
import com.starmitra.modules.scoring.application.ScoringService;
import com.starmitra.modules.skill.application.SkillService;
import com.starmitra.modules.social.application.SocialService;
import com.starmitra.modules.submission.application.SubmissionService;
import com.starmitra.modules.voting.application.VoteService;
import com.starmitra.platform.error.ApiException;
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

/**
 * Cross-module E2E on real PG17 — the StarMitra golden path:
 * register → profile → skills → portfolio → competition → submission →
 * vote → judge eval → scoring → progression → leaderboard, plus the
 * project/team path and moderation/notification probes.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StarMitraE2EIT {

    @Autowired OtpService otpService;
    @Autowired ProfileService profiles;
    @Autowired SkillService skills;
    @Autowired PortfolioService portfolios;
    @Autowired RoomService rooms;
    @Autowired CompetitionService competitions;
    @Autowired SubmissionService submissions;
    @Autowired VoteService votes;
    @Autowired JudgeService judges;
    @Autowired RubricService rubrics;
    @Autowired EvaluationTemplateVersionRepository rubricVersions;
    @Autowired ScoringService scoring;
    @Autowired ProgressionService progression;
    @Autowired LeaderboardService leaderboards;
    @Autowired NotificationService notifications;
    @Autowired ModerationService moderation;
    @Autowired MediaService media;
    @Autowired SocialService social;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID admin, organizer, c1, c2, voter, judgeUser, member;

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

    private void asJudge(UUID uid) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(uid.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_JUDGE"))));
    }

    private void asUser(UUID uid) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(uid.toString(), null, List.of()));
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        admin = register("e2ead");
        organizer = register("e2eo");
        c1 = register("e2ec1");
        c2 = register("e2ec2");
        voter = register("e2ev");
        judgeUser = register("e2ej");
        member = register("e2em");
        asAdmin();
    }

    private UUID latestDraftId(UUID templateId) {
        return rubricVersions.draftsOf(templateId).stream().findFirst().orElseThrow().getId();
    }

    /** Onboarding: profile + skills + portfolio item (M01→M02→M03→M08). */
    private UUID onboarding(UUID user) {
        profiles.updateMyProfile(user, "user", new ProfileService.UpdateCommand(
                "Artist " + user.toString().substring(0, 4), "bio", "IN", null, null), null);
        var skill = skills.create("Singing-" + user.toString().substring(0, 4), "vocal", null);
        skills.addMySkill(user, skill.id(), "ADVANCED");
        return skill.id();
    }

    @Test
    void goldenPathIndividual() {
        // A. onboarding
        var skillId = onboarding(c1);
        onboarding(c2);
        assertFalse(skills.mySkills(c1).isEmpty());
        var pf = portfolios.myPortfolio(c1);
        var item = portfolios.createItem(c1, new PortfolioService.ItemCommand(
                "Demo reel", "act", skillId, "PUBLIC"));
        assertEquals("PUBLIC", item.visibility());

        // B. competition (M09 via admin orchestration)
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("E2E Idol", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var t = rubrics.createTemplate(admin, "Vocal", List.of(
                new RubricService.CriterionCmd("Technique", null, new BigDecimal("60"), null),
                new RubricService.CriterionCmd("Stage", null, new BigDecimal("40"), null)),
                0, 10, null, null, null);
        var rvId = rubrics.publish(admin, latestDraftId(t.id())).id();
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, rvId, null));

        // C. submissions (M10) — two finalists
        var p1 = competitions.register(c1, compId, "USER", cat.id(), null);
        var p2 = competitions.register(c2, compId, "USER", cat.id(), null);
        var s1 = submissions.create(c1, p1.id(), round.id());
        var s2 = submissions.create(c2, p2.id(), round.id());
        submissions.submit(c1, s1.id());
        submissions.finalize(c1, s1.id());
        submissions.submit(c2, s2.id());
        submissions.finalize(c2, s2.id());
        // finalized = immutable
        assertThrows(ApiException.class, () -> submissions.submit(c1, s1.id()));

        // D. voting (M11 — s1 ahead)
        votes.cast(voter, s1.id(), "e2e-v1");
        votes.cast(c2, s1.id(), "e2e-v2");
        votes.cast(voter, s2.id(), "e2e-v3");
        assertEquals(2, votes.counts(s1.id()).count());
        assertEquals(1, votes.counts(s2.id()).count());

        // E. judge eval (M12 scope → M13 eval)
        var j = judges.createJudge(admin, judgeUser);
        judges.assign(admin, j.id(),
                new JudgeService.AssignmentCmd(compId, cat.id(), round.id()));
        var rubric = rubrics.publishedVersion(rvId).orElseThrow();
        asJudge(judgeUser);
        rubrics.submitEvaluation(judgeUser, s1.id(), rvId, rubric.criteria().stream()
                .map(cr -> new RubricService.ScoreCmd(cr.id(), new BigDecimal("9"), null)).toList());
        rubrics.submitEvaluation(judgeUser, s2.id(), rvId, rubric.criteria().stream()
                .map(cr -> new RubricService.ScoreCmd(cr.id(), new BigDecimal("5"), null)).toList());
        asAdmin();

        // F. scoring (M14 — calculate + seal)
        var cfg = scoring.createScoringConfig(admin, compId,
                new BigDecimal("40"), new BigDecimal("60"));
        scoring.createQualificationConfig(compId, Map.of("type", "TOP_N", "n", 1));
        scoring.calculate(admin, compId, round.id(), cfg);
        scoring.finalize(admin, compId, round.id());
        var rank = scoring.latestRanking(round.id());
        assertEquals(1, rank.get(s1.id()));
        assertEquals(2, rank.get(s2.id()));
        assertTrue(scoring.isSealed(round.id()));

        // G. progression (M15 — M14 truth consumed)
        var pcfg = progression.createConfig(compId, Map.of("maxAdvance", 1));
        progression.activate(admin, compId, round.id());
        progression.calculate(admin, compId, round.id(), pcfg);
        var outcomes = progression.outcomesOf(round.id());
        assertEquals("ADVANCED", outcomes.get(s1.id()));
        assertEquals("ELIMINATED", outcomes.get(s2.id()));

        // H. leaderboard (M16 — M14 rank verbatim, never recalculated)
        assertThrows(ApiException.class, () ->
                leaderboards.leaderboard(compId, cat.id(), round.id(), null, 20));  // hidden
        var pubId = leaderboards.publish(admin, compId, cat.id(), round.id(), "PUBLISH");
        var items = leaderboards.leaderboard(compId, cat.id(), round.id(), null, 20);
        assertEquals(2, items.size());
        assertEquals(1, items.get(0).rank());
        assertEquals(s1.id(), items.get(0).entryRef());
        leaderboards.snapshot(admin, pubId);
        leaderboards.publish(admin, compId, cat.id(), round.id(), "HIDE");
        assertThrows(ApiException.class, () ->
                leaderboards.leaderboard(compId, cat.id(), round.id(), null, 20));

        // I. notifications (M17 sink — deduped)
        assertEquals(1, notifications.notifyEvent("M16", "LEADERBOARD_PUBLISHED",
                pubId.toString(), "1", "LEADERBOARD_PUBLISHED", List.of(c1), Map.of(), null));
        assertEquals(0, notifications.notifyEvent("M16", "LEADERBOARD_PUBLISHED",
                pubId.toString(), "1", "LEADERBOARD_PUBLISHED", List.of(c1), Map.of(), null));
        assertEquals(1, notifications.unreadCount(c1));
    }

    @Test
    void projectTeamSingleEntryPath() {
        // room → member → contribution role → project participant → one submission
        var room = rooms.createRoom(organizer,
                new RoomService.RoomCommand("Band", null, "PUBLIC"));
        var inv = rooms.invite(organizer, room.id(), member);
        rooms.respond(member, inv.id(), "ACCEPT");
        rooms.assignRole(organizer, room.id(), member, "Vocalist");   // contribution role ≠ skill
        var skillId = skills.create("Guitar", null, null).id();
        rooms.addRequiredSkill(organizer, room.id(), skillId);        // requirement ≠ role
        assertFalse(rooms.listMembers(organizer, room.id()).isEmpty());

        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Band Battle", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Teams", null, null));
        var t = rubrics.createTemplate(admin, "Band", List.of(
                new RubricService.CriterionCmd("Performance", null, new BigDecimal("100"), null)),
                0, 10, null, null, null);
        var rvId = rubrics.publish(admin, latestDraftId(t.id())).id();
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, rvId, null));

        var p = competitions.register(organizer, compId, "PROJECT", cat.id(), room.id());
        var s = submissions.create(organizer, p.id(), round.id());
        submissions.submit(organizer, s.id());
        submissions.finalize(organizer, s.id());

        var v = votes.cast(voter, s.id(), "proj-1");
        assertEquals("PROJECT", v.targetType());          // one vote target — never split
        assertEquals(1, votes.counts(s.id()).count());

        var j = judges.createJudge(admin, judgeUser);
        judges.assign(admin, j.id(),
                new JudgeService.AssignmentCmd(compId, cat.id(), round.id()));
        var rubric = rubrics.publishedVersion(rvId).orElseThrow();
        asJudge(judgeUser);
        rubrics.submitEvaluation(judgeUser, s.id(), rvId, List.of(
                new RubricService.ScoreCmd(rubric.criteria().get(0).id(),
                        new BigDecimal("9"), null)));
        asAdmin();

        scoring.calculate(admin, compId, round.id(),
                scoring.createScoringConfig(admin, compId, BigDecimal.ZERO, new BigDecimal("100")));
        scoring.finalize(admin, compId, round.id());
        leaderboards.publish(admin, compId, cat.id(), round.id(), "PUBLISH");
        var items = leaderboards.leaderboard(compId, cat.id(), round.id(), null, 20);
        assertEquals(1, items.size());                    // project = ONE entry
        assertEquals(s.id(), items.get(0).entryRef());
    }

    @Test
    void moderationEnforcementProbe() {
        // M18 creates governance restriction; whether owning modules enforce it
        // is the integration question this probe documents (not silently "fixed").
        var m = media.create(c1, new MediaService.CreateCommand(
                "IMAGE", "image/png", 1024L, "p.png", "PUBLIC"));
        moderation.restrict(admin, "MEDIA", m.id(), "HIDDEN", null);
        assertTrue(moderation.isRestricted("MEDIA", m.id()));     // M18 truth: restricted
        // M04 checks its OWN moderation_state column — M18 restriction is not yet
        // propagated (documented P2 gap; media still deliverable once processed).
        assertNotNull(moderation.activeRestrictions("MEDIA", m.id()));
        // report + case lifecycle still works end-to-end
        var r = moderation.createReport(c2, "MEDIA", m.id(), "INAPPROPRIATE_CONTENT", "flagged");
        assertEquals("OPEN", r.status());
    }
}
