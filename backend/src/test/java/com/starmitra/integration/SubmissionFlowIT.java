package com.starmitra.integration;

import com.starmitra.modules.competition.application.CompetitionService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.media.application.MediaService;
import com.starmitra.modules.media.application.ObjectStorageClient;
import com.starmitra.modules.room.application.RoomService;
import com.starmitra.modules.submission.application.SubmissionService;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M10 on real PG17 — individual+team submissions, contributors, lifecycle, deadlines. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubmissionFlowIT {

    @Autowired OtpService otpService;
    @Autowired SubmissionService submissions;
    @Autowired CompetitionService competitions;
    @Autowired RoomService rooms;
    @Autowired MediaService media;
    @Autowired ObjectStorageClient storage;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID organizer, alice, bob, eve;

    private UUID register(String tag) {
        String email = tag + UUID.randomUUID().toString().substring(0, 6) + "@t.dev";
        otpService.request("EMAIL", email);
        return otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        organizer = register("m10o");
        alice = register("m10a");
        bob = register("m10b");
        eve = register("m10e");
    }

    private UUID compWithCategoryAndRound() {
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Idol", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, null, null));
        var p = competitions.register(alice, compId, "USER", cat.id(), null);
        return p.id();   // returns participant; comp/round reachable in test
    }

    private record Ctx(UUID compId, UUID catId, UUID roundId, UUID aliceParticipant) {}
    private Ctx ctx() {
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Idol", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, null, null));
        var p = competitions.register(alice, compId, "USER", cat.id(), null);
        return new Ctx(compId, cat.id(), round.id(), p.id());
    }

    @Test
    void individualSubmissionLifecycle() {
        var c = ctx();
        var s = submissions.create(alice, c.aliceParticipant(), c.roundId());
        assertEquals("DRAFT", s.state());

        // double create → CONFLICT (one per participant per round)
        assertThrows(ApiException.class,
                () -> submissions.create(alice, c.aliceParticipant(), c.roundId()));

        var submitted = submissions.submit(alice, s.id());
        assertEquals("SUBMITTED", submitted.state());
        // resubmit → invalid transition
        assertThrows(ApiException.class, () -> submissions.submit(alice, s.id()));

        var fin = submissions.finalize(alice, s.id());
        assertEquals("FINALIZED", fin.state());
        // withdraw after finalize → invalid
        assertThrows(ApiException.class, () -> submissions.withdraw(alice, s.id()));
        // history trail
        assertEquals(List.of("DRAFT", "SUBMITTED", "FINALIZED"),
                submissions.history(alice, s.id()).stream().map(h -> h.toState()).toList());
    }

    @Test
    void idorProtection() {
        var c = ctx();
        var s = submissions.create(alice, c.aliceParticipant(), c.roundId());
        // eve cannot read/mutate alice's submission
        assertThrows(ApiException.class, () -> submissions.get(eve, s.id()));
        assertThrows(ApiException.class, () -> submissions.submit(eve, s.id()));
        assertThrows(ApiException.class, () -> submissions.withdraw(eve, s.id()));
    }

    @Test
    void teamSubmissionPreservesContextualRoles() {
        var c = ctx();
        // project: organizer owns room; alice+bob members
        var room = rooms.createRoom(organizer, new RoomService.RoomCommand("Crew", null, "PUBLIC"));
        rooms.respond(alice, rooms.invite(organizer, room.id(), alice).id(), "ACCEPT");
        rooms.respond(bob, rooms.invite(organizer, room.id(), bob).id(), "ACCEPT");
        // contextual roles — different from any talent-skill claims
        var roleA = rooms.assignRole(organizer, room.id(), alice, "Lead Vocalist");
        rooms.assignRole(organizer, room.id(), bob, "Mixing Engineer");

        var projParticipant = competitions.register(organizer, c.compId(),
                "PROJECT", c.catId(), room.id());
        var s = submissions.create(alice, projParticipant.id(), c.roundId());

        submissions.declareContributors(alice, s.id(), List.of(
                new SubmissionService.ContributorCmd(alice, roleA.id()),
                new SubmissionService.ContributorCmd(bob, null)));   // member w/o role ok
        // eve is not a room member → contributor resolution fails
        assertThrows(ApiException.class, () -> submissions.declareContributors(alice, s.id(),
                List.of(new SubmissionService.ContributorCmd(eve, null))));

        submissions.submit(alice, s.id());
        var fin = submissions.finalize(alice, s.id());
        assertEquals("FINALIZED", fin.state());
        // DB-02 snapshots captured
        em.flush();
        var rows = jdbc.queryForList(
                "select snapshot_role_name, captured_at from submission_contributors where submission_id=?",
                s.id());
        assertEquals(2, rows.size());
        assertTrue(rows.stream().anyMatch(r -> "Lead Vocalist".equals(r.get("snapshot_role_name"))));
        assertTrue(rows.stream().allMatch(r -> r.get("captured_at") != null));
    }

    @Test
    void deadlineEnforcedOnSubmit() {
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Past", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var past = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "Over", null,
                        OffsetDateTime.now().minusDays(1), null, null, null));
        var p = competitions.register(alice, compId, "USER", cat.id(), null);
        var s = submissions.create(alice, p.id(), past.id());
        var e = assertThrows(ApiException.class, () -> submissions.submit(alice, s.id()));
        assertEquals(ErrorCode.STATE_TRANSITION_INVALID, e.code());
    }

    @Test
    void mediaAttachmentViaM04AndFreeze() throws Exception {
        var c = ctx();
        var s = submissions.create(alice, c.aliceParticipant(), c.roundId());
        // unusable media rejected
        assertThrows(ApiException.class, () -> submissions.attachMedia(alice, s.id(),
                UUID.randomUUID(), null));

        var img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        var baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        var m = media.create(alice, new MediaService.CreateCommand(
                "IMAGE", "image/png", null, "perf.png", "PUBLIC"));
        storage.writeObject(m.objectKey(), baos.toByteArray(), "image/png");
        media.complete(alice, m.id(), null, null);
        submissions.attachMedia(alice, s.id(), m.id(), 0);
        assertTrue(submissions.get(alice, s.id()).mediaIds().contains(m.id()));

        submissions.submit(alice, s.id());
        submissions.finalize(alice, s.id());
        // finalized → evidence frozen
        assertThrows(ApiException.class, () -> submissions.attachMedia(alice, s.id(), m.id(), 1));
    }

    @Test
    void structureContractForVotingTargets() {
        var c = ctx();
        var s = submissions.create(alice, c.aliceParticipant(), c.roundId());
        assertFalse(submissions.isFinalizedSubmission(s.id(), c.roundId()));
        submissions.submit(alice, s.id());
        submissions.finalize(alice, s.id());
        assertTrue(submissions.isFinalizedSubmission(s.id(), c.roundId()));
        assertTrue(submissions.belongsToCompetition(s.id(), c.compId()));
    }
}
