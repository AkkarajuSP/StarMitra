package com.starmitra.modules.moderation.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** User report against a polymorphic target — target is a typed ref, no FK. */
@Entity
@Table(name = "moderation_reports")
public class ModerationReportEntity {

    public enum TargetType { USER, PROFILE, MEDIA, COMMENT, MESSAGE, CREATIVE_ROOM,
                             PORTFOLIO_ITEM, SUBMISSION, COMPETITION }
    public enum Status { OPEN, UNDER_REVIEW, RESOLVED, DISMISSED }

    @Id
    private UUID id;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "reason_code", nullable = false, length = 40)
    private String reasonCode;

    @Column(columnDefinition = "text")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected ModerationReportEntity() {}

    public ModerationReportEntity(UUID reporterId, TargetType targetType, UUID targetId,
                                  String reasonCode, String detail) {
        this.id = UUID.randomUUID();
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reasonCode = reasonCode;
        this.detail = detail;
    }

    public void transition(Status to) { this.status = to; this.updatedAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public Status getStatus() { return status; }
    public TargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public UUID getReporterId() { return reporterId; }
}
