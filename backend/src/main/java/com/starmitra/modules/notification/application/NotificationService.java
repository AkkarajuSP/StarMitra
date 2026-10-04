package com.starmitra.modules.notification.application;

import com.starmitra.modules.notification.persistence.*;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * M17 — Notifications. In-app inbox; IN_APP channel only (future channels
 * provider-neutral via notification_preferences.channel).
 *
 * Dedup: `notification_event_references` UQ(module,event_type,source_id,version)
 * inserted first — retry/concurrent delivery is a no-op (DB-enforced).
 * Read: `notification_read_states` PK(notification+user) — idempotent.
 * Preferences: delivery toggle only — disabled → notification not created.
 * Templates: versioned server-side {param} templates; GENERIC fallback.
 */
@Service
public class NotificationService implements NotificationContract {

    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;
    private final NotificationTemplateRepository templates;
    private final NotificationDeliveryAttemptRepository attempts;
    private final NotificationReadStateRepository readStates;
    private final NotificationEventReferenceRepository eventRefs;

    public NotificationService(NotificationRepository notifications,
                               NotificationPreferenceRepository preferences,
                               NotificationTemplateRepository templates,
                               NotificationDeliveryAttemptRepository attempts,
                               NotificationReadStateRepository readStates,
                               NotificationEventReferenceRepository eventRefs) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.templates = templates;
        this.attempts = attempts;
        this.readStates = readStates;
        this.eventRefs = eventRefs;
    }

    public record View(UUID id, String type, String body, String deepLink,
                       String state, String createdAt) {}
    public record PrefView(String type, String channel, boolean enabled) {}

    // ---------- event sink (cross-module contract) ----------

    @Override @Transactional
    public int notifyEvent(String sourceModule, String eventType, String sourceId,
                           String eventVersion, String notificationType,
                           List<UUID> recipientIds, Map<String, String> bodyFormat,
                           String deepLink) {
        if (eventRefs.existsBySourceModuleAndEventTypeAndSourceIdAndEventVersion(
                sourceModule, eventType, sourceId, eventVersion)) {
            return 0;                                        // dedup — already delivered
        }
        try {
            eventRefs.saveAndFlush(new NotificationEventReferenceEntity(
                    sourceModule, eventType, sourceId, eventVersion));
        } catch (DataIntegrityViolationException race) {
            return 0;                                        // concurrent dedup winner
        }
        var type = safeType(notificationType);
        var template = templates.latestFor(type.name()).stream().findFirst().orElse(null);
        String body = render(template, bodyFormat);
        int created = 0;
        for (var recipientId : recipientIds) {
            if (recipientId == null) continue;
            boolean enabled = preferences.findByUserIdAndTypeAndChannel(
                    recipientId, type.name(), "IN_APP").map(p -> p.isEnabled()).orElse(true);
            if (!enabled) continue;                          // delivery toggle — not business
            var n = notifications.save(new NotificationEntity(recipientId, type,
                    template == null ? null : template.getId(), body,
                    sourceModule, parseUuid(sourceId), deepLink));
            attempts.save(new NotificationDeliveryAttemptEntity(n.getId(), "IN_APP",
                    "DELIVERED", null));
            created++;
        }
        return created;
    }

    // ---------- inbox ----------

    @Transactional(readOnly = true)
    public List<View> list(UUID userId, String cursor, int limit, boolean unreadOnly) {
        var pageable = PageRequest.of(0, limit + 1);
        var rows = unreadOnly ? notifications.unreadInbox(userId, pageable)
                              : notifications.inbox(userId, pageable);
        return rows.stream()
                .filter(n -> cursor == null || n.getId().toString().compareTo(cursor) < 0)
                .limit(limit)
                .map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notifications.unreadCount(userId);
    }

    /** Idempotent read — PK(notification+user) dedups; IDOR → NOT_FOUND. */
    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        var n = notifications.findById(notificationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!n.getRecipientId().equals(userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND);     // never leak existence
        }
        if (n.getState() == NotificationEntity.State.UNREAD) {
            n.markRead();
            notifications.save(n);
        }
        if (!readStates.existsById(new NotificationReadStateEntity.Pk(notificationId, userId))) {
            readStates.save(new NotificationReadStateEntity(notificationId, userId));
        }
    }

    @Transactional
    public int markAllRead(UUID userId) {
        var unread = notifications.findByRecipientIdAndState(userId,
                NotificationEntity.State.UNREAD);
        unread.forEach(n -> {
            n.markRead();
            if (!readStates.existsById(
                    new NotificationReadStateEntity.Pk(n.getId(), userId))) {
                readStates.save(new NotificationReadStateEntity(n.getId(), userId));
            }
        });
        notifications.saveAll(unread);
        return unread.size();
    }

    // ---------- preferences (delivery toggle only) ----------

    @Transactional(readOnly = true)
    public List<PrefView> preferences(UUID userId) {
        return preferences.findByUserId(userId).stream()
                .map(p -> new PrefView(p.getType(), p.getChannel(), p.isEnabled())).toList();
    }

    @Transactional
    public void putPreferences(UUID userId, List<PrefView> rows) {
        for (var p : rows) {
            // user_id is always the authenticated caller — never client-supplied
            preferences.save(new NotificationPreferenceEntity(userId, p.type(),
                    p.channel() == null ? "IN_APP" : p.channel(), p.enabled()));
        }
    }

    // ---------- internals ----------

    private NotificationEntity.Type safeType(String t) {
        try {
            return NotificationEntity.Type.valueOf(t == null ? "GENERIC" : t);
        } catch (Exception e) {
            return NotificationEntity.Type.GENERIC;
        }
    }

    private String render(NotificationTemplateEntity t, Map<String, String> params) {
        if (t == null) return "";
        String body = t.getBodyTemplate();
        for (var e : params.entrySet()) {
            body = body.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return body;
    }

    private UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    private View toView(NotificationEntity n) {
        return new View(n.getId(), n.getType().name(), n.getBody(), n.getDeepLink(),
                n.getState().name(), n.getCreatedAt().toString());
    }
}
