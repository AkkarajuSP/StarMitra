package com.starmitra.integration;

import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.media.application.MediaService;
import com.starmitra.modules.media.application.ObjectStorageClient;
import com.starmitra.modules.portfolio.application.PortfolioService;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M07 on real PG17 — room→invite→member→role→finalize→credit→M08 link. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoomFlowIT {

    @Autowired OtpService otpService;
    @Autowired RoomService rooms;
    @Autowired SkillService skills;
    @Autowired MediaService media;
    @Autowired ObjectStorageClient storage;
    @Autowired PortfolioService portfolios;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID owner, member, outsider;

    private UUID register(String tag) {
        String email = tag + UUID.randomUUID().toString().substring(0, 6) + "@t.dev";
        otpService.request("EMAIL", email);
        return otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        owner = register("m07o");
        member = register("m07m");
        outsider = register("m07x");
    }

    private UUID inviteAndAccept(UUID roomId) {
        var inv = rooms.invite(owner, roomId, member);
        rooms.respond(member, inv.id(), "ACCEPT");
        return inv.id();
    }

    @Test
    void roomLifecycleAndOwnership() {
        var room = rooms.createRoom(owner,
                new RoomService.RoomCommand("Film X", "desc", "PUBLIC"));
        // owner auto-member
        assertEquals(1, rooms.listMembers(owner, room.id()).stream()
                .filter(m -> m.userId().equals(owner)).count());
        // outsider sees PUBLIC room
        assertEquals(room.id(), rooms.getRoom(outsider, room.id()).id());
        // non-owner cannot update (NOT_FOUND — no ownership leak)
        assertThrows(ApiException.class, () -> rooms.updateRoom(member, room.id(),
                new RoomService.RoomCommand("hijack", null, null), null));
        // version/If-Match
        var v1 = rooms.updateRoom(owner, room.id(),
                new RoomService.RoomCommand("Film X2", null, null), String.valueOf(room.version()));
        assertTrue(v1.version() > room.version());
        assertThrows(ApiException.class, () -> rooms.updateRoom(owner, room.id(),
                new RoomService.RoomCommand("stale", null, null), String.valueOf(room.version())));
    }

    @Test
    void privateRoomVisibility() {
        var room = rooms.createRoom(owner,
                new RoomService.RoomCommand("Secret", null, "PRIVATE"));
        assertThrows(ApiException.class, () -> rooms.getRoom(outsider, room.id()));
        assertThrows(ApiException.class, () -> rooms.getRoom(member, room.id()));
        assertEquals(room.id(), rooms.getRoom(owner, room.id()).id());
    }

    @Test
    void invitationLifecycle() {
        var room = rooms.createRoom(owner, new RoomService.RoomCommand("P", null, "PUBLIC"));
        var inv = rooms.invite(owner, room.id(), member);
        // dup pending → CONFLICT
        assertThrows(ApiException.class, () -> rooms.invite(owner, room.id(), member));
        // outsider cannot respond to someone else's invitation
        assertThrows(ApiException.class, () -> rooms.respond(outsider, inv.id(), "ACCEPT"));

        var mine = rooms.myInvitations(member);
        assertEquals(1, mine.size());
        rooms.respond(member, inv.id(), "ACCEPT");
        assertTrue(rooms.isActiveMember(room.id(), member));
        // already-resolved → invalid transition
        assertThrows(ApiException.class, () -> rooms.respond(member, inv.id(), "DECLINE"));
    }

    @Test
    void requiredSkillsValidatedByM03() {
        var room = rooms.createRoom(owner, new RoomService.RoomCommand("P", null, "PUBLIC"));
        assertThrows(ApiException.class,
                () -> rooms.addRequiredSkill(owner, room.id(), UUID.randomUUID()));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("a", null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        var skill = skills.create("VFX", null, null).id();
        SecurityContextHolder.clearContext();
        rooms.addRequiredSkill(owner, room.id(), skill);
        rooms.addRequiredSkill(owner, room.id(), skill);   // idempotent
        rooms.removeRequiredSkill(owner, room.id(), skill);
        assertThrows(ApiException.class,
                () -> rooms.addRequiredSkill(member, room.id(), skill));   // non-owner
    }

    @Test
    void contributionRolesContextualNotPermissions() {
        var room = rooms.createRoom(owner, new RoomService.RoomCommand("P", null, "PUBLIC"));
        inviteAndAccept(room.id());
        // same user, different contextual roles — never tied to their talent skills
        var r1 = rooms.assignRole(owner, room.id(), member, "Lead Actor");
        var r2 = rooms.assignRole(owner, room.id(), member, "Stunt Coordinator");
        assertEquals("Lead Actor", r1.roleName());
        // dup (room,member,role) → CONFLICT
        assertThrows(ApiException.class,
                () -> rooms.assignRole(owner, room.id(), member, "Lead Actor"));
        // non-member cannot hold a role
        assertThrows(ApiException.class,
                () -> rooms.assignRole(owner, room.id(), outsider, "Grip"));
        // non-owner cannot assign
        assertThrows(ApiException.class,
                () -> rooms.assignRole(member, room.id(), member, "X"));
    }

    @Test
    void finalizationMintsVerifiedCreditsLinkedByM08() throws Exception {
        var room = rooms.createRoom(owner, new RoomService.RoomCommand("P", null, "PUBLIC"));
        inviteAndAccept(room.id());
        rooms.assignRole(owner, room.id(), member, "Lead Actor");

        // final output with owned verified media → credits minted
        var img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        var baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        var m = media.create(owner, new MediaService.CreateCommand(
                "IMAGE", "image/png", null, "poster.png", "PUBLIC"));
        storage.writeObject(m.objectKey(), baos.toByteArray(), "image/png");
        media.complete(owner, m.id(), null, null);
        rooms.addFinalOutput(owner, room.id(), m.id(), "POSTER");

        var credits = rooms.listCredits(owner, room.id());
        assertEquals(1, credits.size());
        assertTrue(credits.get(0).verified());
        assertEquals("Lead Actor", credits.get(0).roleName());
        assertEquals(member, credits.get(0).userId());

        // M08 member links their verified credit — authoritative, not manufactured
        var item = portfolios.createItem(member,
                new PortfolioService.ItemCommand("Film work", null, null, "PUBLIC"));
        portfolios.linkCredit(member, item.id(), credits.get(0).id());
        em.flush();
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from portfolio_item_contributions", Integer.class));

        // outsider cannot link a credit that isn't theirs
        var item2 = portfolios.createItem(outsider,
                new PortfolioService.ItemCommand("fake", null, null, "PUBLIC"));
        assertThrows(ApiException.class,
                () -> portfolios.linkCredit(outsider, item2.id(), credits.get(0).id()));
    }

    @Test
    void tasksMembersOnly() {
        var room = rooms.createRoom(owner, new RoomService.RoomCommand("P", null, "PUBLIC"));
        inviteAndAccept(room.id());
        var t = rooms.createTask(member, room.id(), "Shot list", null, member, null);
        assertEquals("OPEN", t.status());
        assertThrows(ApiException.class, () -> rooms.listTasks(outsider, room.id()));
        assertThrows(ApiException.class,
                () -> rooms.createTask(member, room.id(), "x", null, outsider, null)); // bad assignee
    }
}
