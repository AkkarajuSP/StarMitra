package com.starmitra.modules.notification.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Delivery ledger — PENDING/DELIVERED/FAILED/SKIPPED (bounded retry-ready). */
@Entity
@Table(name = "notification_delivery_attempts")
public class NotificationDeliveryAttemptEntity {

    @Id
    private UUID id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(nullable = false, length = 20)
    private String channel = "IN_APP";

    @Column(nullable = false, length = 20)
    private String status = "DELIVERED";

    @Column(name = "attempted_at", nullable = false)
    private OffsetDateTime attemptedAt = OffsetDateTime.now();

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    protected NotificationDeliveryAttemptEntity() {}

    public NotificationDeliveryAttemptEntity(UUID notificationId, String channel, String status,
                                             String failureReason) {
        this.id = UUID.randomUUID();
        this.notificationId = notificationId;
        this.channel = channel;
        this.status = status;
        this.failureReason = failureReason;
    }
}
