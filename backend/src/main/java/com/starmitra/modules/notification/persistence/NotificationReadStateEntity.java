package com.starmitra.modules.notification.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Read marker per notification+user — PK is the idempotency guard. */
@Entity
@Table(name = "notification_read_states")
@IdClass(NotificationReadStateEntity.Pk.class)
public class NotificationReadStateEntity {

    public static class Pk implements java.io.Serializable {
        private UUID notificationId;
        private UUID userId;
        public Pk() {}
        public Pk(UUID notificationId, UUID userId) {
            this.notificationId = notificationId; this.userId = userId;
        }
    }

    @Id
    @Column(name = "notification_id")
    private UUID notificationId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    protected NotificationReadStateEntity() {}

    public NotificationReadStateEntity(UUID notificationId, UUID userId) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.readAt = OffsetDateTime.now();
    }
}
