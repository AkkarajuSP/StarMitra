package com.starmitra.integration;

import com.starmitra.modules.competition.application.CompetitionService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.room.application.RoomService;
import com.starmitra.modules.skill.application.SkillService;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M09 on real PG17 — lifecycle, categories, rounds, participants, contract seam. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CompetitionFlowIT {

    @Autowired OtpService otpService;
    @Autowired CompetitionService competitions;
    @Autowired RoomService rooms;
    @Autowired SkillService skills;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID organizer, user2, outsider;

    private UUID register(String tag) {
        String email = tag + UUID.randomUUID().toString().substring(0, 6) + "@t.dev";
        otpService.request("EMAIL", email);
        return otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        organizer = register("m09o");
        user2 = register("m09u");
        outsider = register("m09x");
    }

    private UUID comp() {
        return competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Star Idol", null)).id();
    }

    @Test
    void lifecycleAndOwnerMutations() {
        var c = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Idol", "desc"));
        assertEquals("DRAFT", c.configStatus());
        assertEquals("OPEN", c.participationStatus());
        assertEquals("NOT_STARTED", c.roundState());

        var updated = competitions.update(organizer, c.id(),
                new CompetitionService.CompetitionCommand("Idol 2", null), String.valueOf(c.version()));
        assertTrue(updated.version() > c.version());
        // stale ETag → conflict
        assertThrows(ApiException.class, () -> competitions.update(organizer, c.id(),
                new CompetitionService.CompetitionCommand("stale", null), String.valueOf(c.version())));
        // non-owner → NOT_FOUND
        assertThrows(ApiException.class, () -> competitions.update(outsider, c.id(),
                new CompetitionService.CompetitionCommand("hijack", null), null));
    }

    @Test
    void multiSkillCategoriesValidatedByM03() {
        var compId = comp();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("a", null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        var singing = skills.create("Singing", null, null).id();
        var dancing = skills.create("Dancing", null, null).id();
        SecurityContextHolder.clearContext();

        // one category, MULTIPLE talent skills — no single-skill assumption
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Vocal+Stage", null, List.of(singing, dancing)));
        assertNotNull(cat.id());
        // invalid skill rejected
        assertThrows(ApiException.class, () -> competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Bad", null, List.of(UUID.randomUUID()))));
        // non-owner cannot create category
        assertThrows(ApiException.class, () -> competitions.createCategory(outsider, compId,
                new CompetitionService.CategoryCommand("x", null, null)));
    }

    @Test
    void roundsAndConfigVersioned() {
        var compId = comp();
        var r1 = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "Audition", null, null, null, null, null));
        assertEquals(1, r1.sequence());

        competitions.addEligibilityRule(organizer, compId, "REGION", "{\"regions\":[\"IN\"]}");
        competitions.putSubmissionConfig(organizer, compId,
                "{\"deadline\":\"2026-01-01T00:00:00Z\",\"mediaTypes\":[\"VIDEO\"]}", null);
        var after = competitions.get(organizer, compId);
        assertEquals("CONFIGURED", after.configStatus());       // config applied → CONFIGURED

        // dup sequence → DB UQ (last — violation aborts the test tx)
        assertThrows(Exception.class, () -> { competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "Dup", null, null, null, null, null));
            em.flush(); });
    }

    @Test
    void userAndProjectRegistration() {
        var compId = comp();
        var p1 = competitions.register(user2, compId, "USER", null, null);
        assertEquals("USER", p1.participantType());
        assertEquals(user2, p1.userId());

        // PROJECT participant requires M07 membership
        var room = rooms.createRoom(organizer, new RoomService.RoomCommand("Team", null, "PUBLIC"));
        assertThrows(ApiException.class, () -> competitions.register(outsider, compId,
                "PROJECT", null, room.id()));
        var p2 = competitions.register(organizer, compId, "PROJECT", null, room.id());
        assertEquals("PROJECT", p2.participantType());
        assertEquals(room.id(), p2.projectId());

        // duplicate registration → CONFLICT (UQ; last — violation aborts tx)
        assertThrows(ApiException.class,
                () -> competitions.register(user2, compId, "USER", null, null));
    }

    @Test
    void structureContractAnswersForDownstream() {
        var compId = comp();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null, null, null, null, null));
        var p = competitions.register(user2, compId, "USER", cat.id(), null);

        assertTrue(competitions.roundBelongsTo(compId, round.id()));
        assertTrue(competitions.categoryBelongsTo(compId, cat.id()));
        assertTrue(competitions.isActiveParticipant(compId, p.id()));
        assertTrue(competitions.isOpenForParticipation(compId));
        assertFalse(competitions.roundBelongsTo(compId, UUID.randomUUID()));
        assertFalse(competitions.isActiveParticipant(compId, UUID.randomUUID()));
    }

    @Test
    void registrationRules() {
        var compId = comp();
        // bad category → reject
        assertThrows(ApiException.class, () -> competitions.register(user2, compId,
                "USER", UUID.randomUUID(), null));
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var ok = competitions.register(user2, compId, "USER", cat.id(), null);
        assertNotNull(ok.id());
        assertEquals(1, competitions.listParticipants(organizer, compId, null, 10).items().size());
    }
}
