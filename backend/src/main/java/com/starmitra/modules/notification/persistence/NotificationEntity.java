package com.starmitra.modules.notification.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** M17-owned notification record — never business truth. */
@Entity
@Table(name = "notifications")
public class NotificationEntity {

    public enum State { UNREAD, READ }

    /** Controlled MVP taxonomy (VARCHAR-backed, validated in service). */
    public enum Type {
        AUTHENTICATION, MESSAGE, FOLLOW, LIKE, COMMENT, CREATIVE_ROOM_INVITATION,
        CREATIVE_ROOM_ACTIVITY, COMPETITION_PUBLISHED, COMPETITION_DEADLINE,
        SUBMISSION_STATUS, JUDGE_ASSIGNMENT, EVALUATION_STATUS, SCORE_AVAILABLE,
        QUALIFICATION_RESULT, ROUND_PROGRESS, LEADERBOARD_PUBLISHED,
        MODERATION_ACTION, ADMIN_ANNOUNCEMENT, GENERIC
    }

    @Id
    private UUID id;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Type type;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(columnDefinition = "text")
    private String body;

    @Column(name = "source_ref_type", length = 40)
    private String sourceRefType;

    @Column(name = "source_ref_id")
    private UUID sourceRefId;

    @Column(name = "deep_link", length = 512)
    private String deepLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private State state = State.UNREAD;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected NotificationEntity() {}

    public NotificationEntity(UUID recipientId, Type type, UUID templateId, String body,
                              String sourceRefType, UUID sourceRefId, String deepLink) {
        this.id = UUID.randomUUID();
        this.recipientId = recipientId;
        this.type = type;
        this.templateId = templateId;
        this.body = body;
        this.sourceRefType = sourceRefType;
        this.sourceRefId = sourceRefId;
        this.deepLink = deepLink;
    }

    public void markRead() {
        this.state = State.READ;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getRecipientId() { return recipientId; }
    public Type getType() { return type; }
    public String getBody() { return body; }
    public String getDeepLink() { return deepLink; }
    public State getState() { return state; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getSourceRefType() { return sourceRefType; }
    public UUID getSourceRefId() { return sourceRefId; }
}
