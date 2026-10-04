package com.starmitra.modules.notification.api;

import com.starmitra.modules.notification.application.NotificationService;
import com.starmitra.platform.pagination.Cursor;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** /api/v1/notifications + /notification-preferences — M17. */
@RestController
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}
    public record NotificationPage(List<NotificationService.View> items, PageMeta page) {}
    public record NotificationPref(String type, String channel, boolean enabled) {}

    @GetMapping("/api/v1/notifications")
    public ResponseEntity<NotificationPage> listNotifications(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        var uid = SecurityUtils.currentUserId();
        var items = notifications.list(uid, cursor, Cursor.limit(limit) + 1, unreadOnly);
        int lim = Cursor.limit(limit);
        boolean more = items.size() > lim;
        var page = items.stream().limit(lim).toList();
        String next = more && !page.isEmpty() ? page.get(page.size() - 1).id().toString() : null;
        return ResponseEntity.ok(new NotificationPage(page, new PageMeta(next, more, page.size())));
    }

    @PostMapping("/api/v1/notifications/{notificationId}/read")
    public ResponseEntity<Void> markNotificationRead(@PathVariable UUID notificationId) {
        notifications.markRead(SecurityUtils.currentUserId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/notifications/read-all")
    public ResponseEntity<Void> markAllNotificationsRead() {
        notifications.markAllRead(SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/notification-preferences")
    public ResponseEntity<List<NotificationPref>> getNotificationPrefs() {
        return ResponseEntity.ok(notifications.preferences(SecurityUtils.currentUserId()).stream()
                .map(p -> new NotificationPref(p.type(), p.channel(), p.enabled()))
                .toList());
    }

    @PutMapping("/api/v1/notification-preferences")
    public ResponseEntity<Void> putNotificationPrefs(
            @Valid @RequestBody List<NotificationPref> body) {
        notifications.putPreferences(SecurityUtils.currentUserId(), body.stream()
                .map(p -> new NotificationService.PrefView(p.type(), p.channel(), p.enabled()))
                .toList());
        return ResponseEntity.ok().build();
    }
}
