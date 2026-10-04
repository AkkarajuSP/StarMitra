package com.starmitra.modules.notification.persistence;

import jakarta.persistence.*;
import java.util.UUID;

/** Per-user, per-type, per-channel delivery toggle — affects delivery only. */
@Entity
@Table(name = "notification_preferences")
@IdClass(NotificationPreferenceEntity.Pk.class)
public class NotificationPreferenceEntity {

    public static class Pk implements java.io.Serializable {
        private UUID userId;
        private String type;
        private String channel;
        public Pk() {}
        public Pk(UUID userId, String type, String channel) {
            this.userId = userId; this.type = type; this.channel = channel;
        }
    }

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(length = 40)
    private String type;

    @Id
    @Column(length = 20)
    private String channel;

    @Column(nullable = false)
    private boolean enabled = true;

    protected NotificationPreferenceEntity() {}

    public NotificationPreferenceEntity(UUID userId, String type, String channel, boolean enabled) {
        this.userId = userId;
        this.type = type;
        this.channel = channel;
        this.enabled = enabled;
    }

    public boolean isEnabled() { return enabled; }
    public String getType() { return type; }
    public String getChannel() { return channel; }
}
