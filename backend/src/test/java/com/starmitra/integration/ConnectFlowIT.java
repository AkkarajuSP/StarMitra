package com.starmitra.integration;

import com.starmitra.modules.connect.application.ConnectService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.media.application.MediaService;
import com.starmitra.modules.media.application.ObjectStorageClient;
import com.starmitra.modules.room.application.RoomService;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M06 on real PG17 — convs, sequences, idempotency, receipts, blocks, project convs. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConnectFlowIT {

    @Autowired OtpService otpService;
    @Autowired ConnectService connect;
    @Autowired RoomService rooms;
    @Autowired MediaService media;
    @Autowired ObjectStorageClient storage;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID alice, bob, eve;

    private UUID register(String tag) {
        String email = tag + UUID.randomUUID().toString().substring(0, 6) + "@t.dev";
        otpService.request("EMAIL", email);
        return otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        alice = register("m06a");
        bob = register("m06b");
        eve = register("m06e");
    }

    @Test
    void oneToOneConversationAndMessaging() {
        var conv = connect.create(alice,
                new ConnectService.CreateCommand("ONE_TO_ONE", List.of(bob), null));
        // both members can see it; outsider cannot (NOT_FOUND — hidden)
        assertEquals(conv.id(), connect.get(bob, conv.id()).id());
        assertThrows(ApiException.class, () -> connect.get(eve, conv.id()));

        var m1 = connect.send(alice, conv.id(),
                new ConnectService.SendCommand("hi bob", "cmid-1", null));
        var m2 = connect.send(bob, conv.id(),
                new ConnectService.SendCommand("hi alice", "cmid-2", null));
        assertEquals(1, m1.sequence());                        // per-conversation ordering
        assertEquals(2, m2.sequence());
        assertThrows(ApiException.class, () -> connect.send(eve, conv.id(),
                new ConnectService.SendCommand("intruder", "cmid-3", null)));
    }

    @Test
    void clientMessageIdDedupReplay() {
        var conv = connect.create(alice,
                new ConnectService.CreateCommand("ONE_TO_ONE", List.of(bob), null));
        var a = connect.send(alice, conv.id(),
                new ConnectService.SendCommand("once", "dup-1", null));
        var b = connect.send(alice, conv.id(),
                new ConnectService.SendCommand("once", "dup-1", null));
        assertEquals(a.id(), b.id());                          // same message returned
        em.flush();
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from messages where conversation_id=?",
                Integer.class, conv.id()));
    }

    @Test
    void historyRecoveryAndReadReceipts() {
        var conv = connect.create(alice,
                new ConnectService.CreateCommand("ONE_TO_ONE", List.of(bob), null));
        connect.send(alice, conv.id(), new ConnectService.SendCommand("m1", "s1", null));
        connect.send(alice, conv.id(), new ConnectService.SendCommand("m2", "s2", null));
        connect.send(alice, conv.id(), new ConnectService.SendCommand("m3", "s3", null));

        var missed = connect.history(bob, conv.id(), 1L, null, 10);   // reconnect since seq 1
        assertEquals(List.of(2L, 3L), missed.items().stream().map(m -> m.sequence()).toList());

        connect.markRead(bob, conv.id(), 2);
        em.flush();
        assertEquals(2, jdbc.queryForObject(
                "select count(*) from message_receipts where user_id=? and status='READ'",
                Integer.class, bob));
    }

    @Test
    void blockPreventsConversationAndSend() {
        connect.block(alice, bob);
        connect.block(alice, bob);                                      // idempotent
        assertThrows(ApiException.class, () -> connect.create(alice,
                new ConnectService.CreateCommand("ONE_TO_ONE", List.of(bob), null)));

        // existing conv: bob blocked by alice → send denied both directions
        connect.unblock(alice, bob);
        var conv = connect.create(alice,
                new ConnectService.CreateCommand("ONE_TO_ONE", List.of(bob), null));
        connect.block(bob, alice);
        var e = assertThrows(ApiException.class, () -> connect.send(alice, conv.id(),
                new ConnectService.SendCommand("hi", "b1", null)));
        assertEquals(ErrorCode.BLOCKED, e.code());
    }

    @Test
    void projectConversationUsesM07Membership() {
        var room = rooms.createRoom(alice, new RoomService.RoomCommand("Film", null, "PRIVATE"));
        // eve not a member → cannot create/read project conv
        assertThrows(ApiException.class, () -> connect.create(eve,
                new ConnectService.CreateCommand("PROJECT", null, room.id())));

        var conv = connect.create(alice,
                new ConnectService.CreateCommand("PROJECT", null, room.id()));
        assertThrows(ApiException.class, () -> connect.send(eve, conv.id(),
                new ConnectService.SendCommand("hi", "p1", null)));

        // invite bob into the room → M07 membership unlocks M06 access
        var inv = rooms.invite(alice, room.id(), bob);
        rooms.respond(bob, inv.id(), "ACCEPT");
        var m = connect.send(bob, conv.id(),
                new ConnectService.SendCommand("on set!", "p2", null));
        assertEquals(1, m.sequence());
        assertEquals(conv.id(), connect.get(bob, conv.id()).id());
    }

    @Test
    void attachmentsValidateThroughM04() throws Exception {
        var conv = connect.create(alice,
                new ConnectService.CreateCommand("ONE_TO_ONE", List.of(bob), null));
        // unusable media → rejected
        assertThrows(ApiException.class, () -> connect.send(alice, conv.id(),
                new ConnectService.SendCommand("file", "a1", List.of(UUID.randomUUID()))));

        var img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        var baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        var m = media.create(alice, new MediaService.CreateCommand(
                "IMAGE", "image/png", null, "attach.png", "PRIVATE"));
        storage.writeObject(m.objectKey(), baos.toByteArray(), "image/png");
        media.complete(alice, m.id(), null, null);

        var sent = connect.send(alice, conv.id(),
                new ConnectService.SendCommand("with file", "a2", List.of(m.id())));
        assertEquals(List.of(m.id()), sent.attachmentMediaIds());
    }
}
