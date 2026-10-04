package com.starmitra.integration;

import com.starmitra.modules.competition.application.CompetitionService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.room.application.RoomService;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M11 on real PG17 — vote validity, dedup, limits, windows, isolation, config. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VoteFlowIT {

    @Autowired OtpService otpService;
    @Autowired VoteService votes;
    @Autowired SubmissionService submissions;
    @Autowired CompetitionService competitions;
    @Autowired RoomService rooms;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID organizer, voter1, voter2, contestant;

    private UUID register(String tag) {
        String email = tag + UUID.randomUUID().toString().substring(0, 6) + "@t.dev";
        otpService.request("EMAIL", email);
        return otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        organizer = register("m11o");
        voter1 = register("m11v1");
        voter2 = register("m11v2");
        contestant = register("m11c");
    }

    /** Finalized individual submission in open-window round → (subId, roundId, compId). */
    private UUID finalizedIndividual() {
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Idol", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, null, null));
        var p = competitions.register(contestant, compId, "USER", cat.id(), null);
        var s = submissions.create(contestant, p.id(), round.id());
        submissions.submit(contestant, s.id());
        submissions.finalize(contestant, s.id());
        return s.id();
    }

    private UUID finalizedProject() {
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Band", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Teams", null, null));
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, null, null));
        var room = rooms.createRoom(organizer, new RoomService.RoomCommand("Band", null, "PUBLIC"));
        rooms.respond(contestant, rooms.invite(organizer, room.id(), contestant).id(), "ACCEPT");
        var p = competitions.register(organizer, compId, "PROJECT", cat.id(), room.id());
        var s = submissions.create(contestant, p.id(), round.id());
        submissions.submit(contestant, s.id());
        submissions.finalize(contestant, s.id());
        return s.id();
    }

    @Test
    void individualAndProjectVoting() {
        var ind = finalizedIndividual();
        var proj = finalizedProject();
        var v1 = votes.cast(voter1, ind, "v-1");
        var v2 = votes.cast(voter1, proj, "v-2");
        assertEquals("INDIVIDUAL", v1.targetType());
        assertEquals("PROJECT", v2.targetType());     // one target — not split
        assertEquals(1, votes.counts(ind).count());
        assertEquals(1, votes.counts(proj).count());
        // auditability — voter + target + comp/round columns on the vote row
        em.flush();
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from votes where submission_id=? and target_type='PROJECT'",
                Integer.class, proj));
    }

    @Test
    void dedupAndIdempotentReplay() {
        var sub = finalizedIndividual();
        var v1 = votes.cast(voter1, sub, "dup-1");
        var v2 = votes.cast(voter1, sub, "dup-2");            // same voter+sub+round → replay
        assertEquals(v1.id(), v2.id());
        var replay = votes.cast(voter1, sub, "dup-1");        // same clientMsgId → same
        assertEquals(v1.id(), replay.id());
        assertEquals(1, votes.counts(sub).count());
        // different voter still counts
        votes.cast(voter2, sub, "dup-3");
        assertEquals(2, votes.counts(sub).count());
    }

    @Test
    void invalidTargetsRejected() {
        assertThrows(ApiException.class, () -> votes.cast(voter1, UUID.randomUUID(), "x1"));
        // non-finalized submission
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("X", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("S", null, null));
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, null, null));
        var p = competitions.register(contestant, compId, "USER", cat.id(), null);
        var draft = submissions.create(contestant, p.id(), round.id());
        var e = assertThrows(ApiException.class, () -> votes.cast(voter1, draft.id(), "x2"));
        assertEquals(ErrorCode.STATE_TRANSITION_INVALID, e.code());
    }

    @Test
    void windowAndConfigLimit() {
        // closed window: round ended
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Closed", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("S", null, null));
        var pastRound = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "Past", null,
                        OffsetDateTime.now().minusDays(1), null, null, null));
        var p = competitions.register(contestant, compId, "USER", cat.id(), null);
        var s = submissions.create(contestant, p.id(), pastRound.id());
        // M10 itself enforces the round deadline — the submission can't even submit
        assertThrows(ApiException.class, () -> submissions.submit(contestant, s.id()));

        // vote window closed by CONFIG (round still open) → finalized sub, vote rejected
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("a", null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        var cfgId = votes.createConfig("default",
                Map.of("maxVotesPerVoter", 1));
        var closedCfg = votes.createConfig("closed-window",
                Map.of("windowEnd", OffsetDateTime.now().minusDays(1).toString()));
        SecurityContextHolder.clearContext();
        var comp2 = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Limited", null)).id();
        var cat2 = competitions.createCategory(organizer, comp2,
                new CompetitionService.CategoryCommand("S", null, null));
        var round2 = competitions.createRound(organizer, comp2,
                new CompetitionService.RoundCommand(1, "R", null,
                        OffsetDateTime.now().plusDays(7), cfgId, null, null));
        var closedRound = competitions.createRound(organizer, comp2,
                new CompetitionService.RoundCommand(2, "ClosedByConfig", null,
                        OffsetDateTime.now().plusDays(7), closedCfg, null, null));
        var p2 = competitions.register(contestant, comp2, "USER", cat2.id(), null);
        var p3 = competitions.register(voter2, comp2, "USER", cat2.id(), null);
        var s2 = submissions.create(contestant, p2.id(), round2.id());
        var s3 = submissions.create(voter2, p3.id(), round2.id());
        submissions.submit(contestant, s2.id());
        submissions.finalize(contestant, s2.id());
        submissions.submit(voter2, s3.id());
        submissions.finalize(voter2, s3.id());
        votes.cast(voter1, s2.id(), "lim-1");                    // voter1's 1 allowed vote
        var e2 = assertThrows(ApiException.class,
                () -> votes.cast(voter1, s3.id(), "lim-2"));     // 2nd in same round → limit
        assertEquals(ErrorCode.STATE_TRANSITION_INVALID, e2.code());
        assertEquals(1, votes.countForSubmission(s2.id()));
        var counts = votes.countsBySubmission(round2.id());
        assertEquals(1, counts.get(s2.id()));

        // closed-by-config round: finalized sub exists (window config only gates votes)
        var pC = competitions.register(voter1, comp2, "USER", cat2.id(), null);
        var sC = submissions.create(voter1, pC.id(), closedRound.id());
        submissions.submit(voter1, sC.id());
        submissions.finalize(voter1, sC.id());
        assertThrows(ApiException.class, () -> votes.cast(voter2, sC.id(), "wc-1"));
    }

    @Test
    void competitionRoundIsolation() {
        var sub1 = finalizedIndividual();
        var sub2 = finalizedIndividual();
        votes.cast(voter1, sub1, "iso-1");
        // counts are per-submission — no cross-round bleed
        assertEquals(1, votes.counts(sub1).count());
        assertEquals(0, votes.counts(sub2).count());
        var roundId = submissions.submissionView(sub1).orElseThrow().roundId();
        var roundId2 = submissions.submissionView(sub2).orElseThrow().roundId();
        assertEquals(1, votes.countsBySubmission(roundId).size());
        assertFalse(votes.countsBySubmission(roundId2).containsKey(sub1));
    }
}
