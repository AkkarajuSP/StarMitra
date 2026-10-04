package com.starmitra.modules.notification.application;

import java.util.Map;
import java.util.UUID;

/**
 * M17-owned event sink — other modules raise notification-worthy events.
 * The SOURCE module stays authoritative for the business fact; M17 only
 * persists/formats/delivers the user-facing notification.
 */
public interface NotificationContract {

    /**
     * Server-side event: (module, eventType, sourceId, eventVersion) is the
     * dedup key — replay is a no-op. Recipients are always derived by the
     * CALLER's authoritative business rules (never from client input).
     *
     * @param bodyFormat template params (server-computed only)
     * @return number of notifications created (0 when deduped/prefs-disabled)
     */
    int notifyEvent(String sourceModule, String eventType, String sourceId,
                    String eventVersion, String notificationType,
                    java.util.List<UUID> recipientIds,
                    Map<String, String> bodyFormat, String deepLink);
}
