package com.starmitra.integration;

import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.notification.application.NotificationService;
import com.starmitra.modules.notification.application.NotificationService.PrefView;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M17 on real PG17 — dedup'd event sink, inbox, read state, prefs, IDOR. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationFlowIT {

    @Autowired OtpService otpService;
    @Autowired NotificationService notifications;
    @Autowired JdbcTemplate jdbc;

    private UUID userA, userB;

    private UUID register(String tag) {
        String email = tag + UUID.randomUUID().toString().substring(0, 6) + "@t.dev";
        otpService.request("EMAIL", email);
        return otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        userA = register("m17a");
        userB = register("m17b");
    }

    @Test
    void eventCreatesNotificationOncePerDedupKey() {
        int n = notifications.notifyEvent("M21", "FOLLOW", UUID.randomUUID().toString(), "1",
                "FOLLOW", List.of(userA), Map.of(), null);
        assertEquals(1, n);
        // same event key → deduped (DB UQ)
        var src = UUID.randomUUID().toString();
        notifications.notifyEvent("M21", "FOLLOW", src, "1", "FOLLOW", List.of(userA), Map.of(), null);
        assertEquals(0, notifications.notifyEvent("M21", "FOLLOW", src, "1",
                "FOLLOW", List.of(userA), Map.of(), null));
        assertEquals(2, notifications.list(userA, null, 10, false).size());
    }

    @Test
    void inboxReadUnreadLifecycle() {
        notifications.notifyEvent("M06", "MESSAGE", UUID.randomUUID().toString(), "1",
                "MESSAGE", List.of(userA), Map.of(), null);
        notifications.notifyEvent("M06", "MESSAGE", UUID.randomUUID().toString(), "1",
                "MESSAGE", List.of(userA), Map.of(), null);
        assertEquals(2, notifications.unreadCount(userA));
        var n = notifications.list(userA, null, 10, false).get(0);
        notifications.markRead(userA, n.id());
        notifications.markRead(userA, n.id());               // idempotent
        assertEquals(1, notifications.unreadCount(userA));
        assertEquals(1, notifications.list(userA, null, 10, true).size());   // unreadOnly
        notifications.markAllRead(userA);
        assertEquals(0, notifications.unreadCount(userA));
    }

    @Test
    void userCannotReadAnothersNotification() {
        notifications.notifyEvent("M21", "FOLLOW", UUID.randomUUID().toString(), "1",
                "FOLLOW", List.of(userA), Map.of(), null);
        var n = notifications.list(userA, null, 10, false).get(0);
        var e = assertThrows(ApiException.class,
                () -> notifications.markRead(userB, n.id()));  // IDOR → NOT_FOUND
        assertEquals(ErrorCode.NOT_FOUND, e.code());
        assertEquals(1, notifications.unreadCount(userA));
        // B's inbox empty regardless
        assertTrue(notifications.list(userB, null, 10, false).isEmpty());
    }

    @Test
    void preferenceDisablesDeliveryNotEvent() {
        notifications.putPreferences(userA, List.of(
                new PrefView("FOLLOW", "IN_APP", false)));
        assertEquals(0, notifications.notifyEvent("M21", "FOLLOW",
                UUID.randomUUID().toString(), "1", "FOLLOW",
                List.of(userA), Map.of(), null));
        assertEquals(0, notifications.unreadCount(userA));
        // re-enable → future events deliver
        notifications.putPreferences(userA, List.of(
                new PrefView("FOLLOW", "IN_APP", true)));
        assertEquals(1, notifications.notifyEvent("M21", "FOLLOW",
                UUID.randomUUID().toString(), "1", "FOLLOW",
                List.of(userA), Map.of(), null));
    }

    @Test
    void templatesRenderServerSide() {
        // seed a template row via jdbc (no API needed for seed)
        jdbc.update("insert into notification_templates(id,code,version_no,channel,body_template,status) " +
                        "values (?,'FOLLOW',1,'IN_APP','{actor} started following you','PUBLISHED')",
                UUID.randomUUID());
        notifications.notifyEvent("M21", "FOLLOW", UUID.randomUUID().toString(), "1",
                "FOLLOW", List.of(userA), Map.of("actor", "Dev"), null);
        var n = notifications.list(userA, null, 10, false).get(0);
        assertEquals("Dev started following you", n.body());  // server-side render
    }
}
