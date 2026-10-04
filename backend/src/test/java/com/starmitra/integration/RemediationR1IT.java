package com.starmitra.integration;

import com.starmitra.modules.competition.application.CompetitionService;
import com.starmitra.modules.discovery.application.DiscoveryService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.judge.application.JudgeService;
import com.starmitra.modules.leaderboard.application.LeaderboardService;
import com.starmitra.modules.media.application.MediaService;
import com.starmitra.modules.media.application.ObjectStorageClient;
import com.starmitra.modules.moderation.application.ModerationService;
import com.starmitra.modules.notification.application.NotificationService;
import com.starmitra.modules.progression.application.ProgressionService;
import com.starmitra.modules.profile.application.ProfileService;
import com.starmitra.modules.rubric.application.RubricService;
import com.starmitra.modules.rubric.persistence.EvaluationTemplateVersionRepository;
import com.starmitra.modules.scoring.application.ScoringService;
import com.starmitra.modules.social.application.SocialService;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R1 remediation on real PG17 — M18 enforcement consumption (M04/M05/M21),
 * M17 producer wiring (FOLLOW/LIKE/COMPETITION_PUBLISHED/
 * LEADERBOARD_PUBLISHED/EVALUATION_STATUS), M10 media refs for M20.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RemediationR1IT {

    @Autowired OtpService otpService;
    @Autowired MediaService media;
    @Autowired ObjectStorageClient storage;
    @Autowired DiscoveryService discovery;
    @Autowired SocialService social;
    @Autowired ModerationService moderation;
    @Autowired ProfileService profiles;
    @Autowired CompetitionService competitions;
    @Autowired SubmissionService submissions;
    @Autowired LeaderboardService leaderboards;
    @Autowired JudgeService judges;
    @Autowired RubricService rubrics;
    @Autowired EvaluationTemplateVersionRepository rubricVersions;
    @Autowired ScoringService scoring;
    @Autowired ProgressionService progression;
    @Autowired NotificationService notifications;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID admin, alice, bob;

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

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        admin = register("r1ad");
        alice = register("r1a");
        bob = register("r1b");
        asAdmin();
    }

    private byte[] png() throws Exception {
        var baos = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", baos);
        return baos.toByteArray();
    }

    private UUID deliverableMedia(UUID owner) throws Exception {
        var a = media.create(owner, new MediaService.CreateCommand(
                "IMAGE", "image/png", 8L, "p.png", "PUBLIC"));
        storage.writeObject(a.objectKey(), png(), "image/png");
        media.complete(owner, a.id(), null, null);
        return a.id();
    }

    // ---------- R1-A: M18 → M04 ----------

    @Test
    void m18MediaRestrictionBlocksDelivery() throws Exception {
        var mid = deliverableMedia(alice);
        var stranger = UUID.randomUUID();
        assertTrue(media.isDeliverableTo(mid, stranger));

        moderation.restrict(admin, "MEDIA", mid, "HIDDEN", null);
        assertFalse(media.isDeliverableTo(mid, stranger));            // M18 enforced by M04
        // owner access follows M18 policy — owner can still fetch own asset
        assertNotNull(media.deliveryUrl(alice, mid));

        // expired restriction → delivery restored
        moderation.restrict(admin, "MEDIA", mid, "HIDDEN",
                OffsetDateTime.now().minusSeconds(1));
        em.flush(); em.clear();
        // note: HIDDEN (indefinite) still active from first restriction → lift test below
    }

    @Test
    void m18UserRestrictionBlocksAllMedia() throws Exception {
        var mid = deliverableMedia(alice);
        var stranger = UUID.randomUUID();
        moderation.restrict(admin, "USER", alice, "HIDDEN", null);
        assertFalse(media.isDeliverableTo(mid, stranger));
    }

    @Test
    void expiredRestrictionRestoresEligibility() throws Exception {
        var mid = deliverableMedia(alice);
        var stranger = UUID.randomUUID();
        moderation.restrict(admin, "MEDIA", mid, "HIDDEN",
                OffsetDateTime.now().minusSeconds(1));                // already expired
        assertTrue(media.isDeliverableTo(mid, stranger));             // not enforced
    }

    // ---------- R1-A: M18 → M05 / M21 ----------

    @Test
    void restrictedUserExcludedFromDiscoveryAndSearch() {
        profiles.updateMyProfile(alice, "alice",
                new ProfileService.UpdateCommand("AliceStar", "bio", null, null, "PUBLIC"), null);
        em.flush();
        assertTrue(discovery.discover(bob, null, null, 10).items().stream()
                .anyMatch(i -> i.id().equals(alice)));
        assertTrue(discovery.search(bob, "AliceStar", "PROFILE", null, 10).items().stream()
                .anyMatch(i -> i.id().equals(alice)));

        moderation.restrict(admin, "USER", alice, "HIDDEN", null);
        assertFalse(discovery.discover(bob, null, null, 10).items().stream()
                .anyMatch(i -> i.id().equals(alice)));
        assertFalse(discovery.search(bob, "AliceStar", "PROFILE", null, 10).items().stream()
                .anyMatch(i -> i.id().equals(alice)));
    }

    @Test
    void restrictedUserCannotEngage() {
        moderation.restrict(admin, "USER", bob, "POSTING", null);
        assertEquals(ErrorCode.TARGET_RESTRICTED,
                assertThrows(ApiException.class, () -> social.follow(bob, alice)).code());
        assertEquals(ErrorCode.TARGET_RESTRICTED,
                assertThrows(ApiException.class, () -> social.comment(bob, "MEDIA",
                        UUID.randomUUID(), "hi")).code());
    }

    // ---------- R1-B: producer wiring ----------

    @Test
    void followAndLikeProduceNotifications() throws Exception {
        social.follow(alice, bob);
        assertTrue(notifications.list(bob, null, 10, false).stream()
                .anyMatch(n -> "FOLLOW".equals(n.type())));

        var mid = deliverableMedia(alice);
        social.like(bob, "MEDIA", mid);
        assertTrue(notifications.list(alice, null, 10, false).stream()
                .anyMatch(n -> "LIKE".equals(n.type())));             // owner notified
    }

    @Test
    void competitionPublishedNotifiesOrganizer() {
        competitions.create(admin, new CompetitionService.CompetitionCommand("C", null));
        assertTrue(notifications.list(admin, null, 10, false).stream()
                .anyMatch(n -> "COMPETITION_PUBLISHED".equals(n.type())));
    }

    @Test
    void evaluationAndLeaderboardProduceNotifications() {
        var compId = competitions.create(admin,
                new CompetitionService.CompetitionCommand("Eval Comp", null)).id();
        var cat = competitions.createCategory(admin, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var t = rubrics.createTemplate(admin, "R", List.of(
                new RubricService.CriterionCmd("C", null, new BigDecimal("100"), null)),
                0, 10, null, null, null);
        var rvId = rubrics.publish(admin, rubricVersions.draftsOf(t.id()).get(0).getId()).id();
        var round = competitions.createRound(admin, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, rvId, null));
        var p = competitions.register(alice, compId, "USER", cat.id(), null);
        var s = submissions.create(alice, p.id(), round.id());
        submissions.submit(alice, s.id());
        submissions.finalize(alice, s.id());

        var judge = judges.createJudge(admin, register("r1j"));
        judges.assign(admin, judge.id(),
                new JudgeService.AssignmentCmd(compId, cat.id(), round.id()));

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(judge.userId().toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_JUDGE"))));
        var rubric = rubrics.publishedVersion(rvId).orElseThrow();
        rubrics.submitEvaluation(judge.userId(), s.id(), rvId, List.of(
                new RubricService.ScoreCmd(rubric.criteria().get(0).id(),
                        new BigDecimal("9"), null)));
        asAdmin();
        // EVALUATION_STATUS → participant (alice)
        assertTrue(notifications.list(alice, null, 20, false).stream()
                .anyMatch(n -> "EVALUATION_STATUS".equals(n.type())));

        // LEADERBOARD_PUBLISHED → alice (entry owner)
        scoring.createQualificationConfig(compId, java.util.Map.of("type", "TOP_N", "n", 1));
        scoring.calculate(admin, compId, round.id(),
                scoring.createScoringConfig(admin, compId, BigDecimal.ZERO, new BigDecimal("100")));
        scoring.finalize(admin, compId, round.id());
        progression.activate(admin, compId, round.id());
        progression.calculate(admin, compId, round.id(),
                progression.createConfig(compId, java.util.Map.of("maxAdvance", 1)));
        leaderboards.publish(admin, compId, cat.id(), round.id(), "PUBLISH");
        assertTrue(notifications.list(alice, null, 20, false).stream()
                .anyMatch(n -> "LEADERBOARD_PUBLISHED".equals(n.type())));
    }

    // ---------- R1-C: M10 media refs → M20 ----------

    @Test
    void scopedSubmissionsExposeMediaIds() throws Exception {
        var mid = deliverableMedia(alice);
        var compId = competitions.create(admin,
                new CompetitionService.CompetitionCommand("Media Comp", null)).id();
        var cat = competitions.createCategory(admin, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var round = competitions.createRound(admin, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, null, null));
        var p = competitions.register(alice, compId, "USER", cat.id(), null);
        var s = submissions.create(alice, p.id(), round.id());
        submissions.attachMedia(alice, s.id(), mid, 0);
        submissions.submit(alice, s.id());
        submissions.finalize(alice, s.id());

        var judge = judges.createJudge(admin, register("r1j2"));
        judges.assign(admin, judge.id(),
                new JudgeService.AssignmentCmd(compId, cat.id(), round.id()));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(judge.userId().toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_JUDGE"))));
        var scoped = judges.myScopedSubmissions(judge.userId(), compId);
        assertEquals(1, scoped.size());
        assertEquals(List.of(mid), scoped.get(0).mediaIds());        // M04 refs, not keys
        asAdmin();
    }
}
