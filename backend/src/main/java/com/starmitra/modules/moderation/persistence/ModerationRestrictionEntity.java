package com.starmitra.modules.moderation.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Admin-enforced restriction (≠ M21 user_block) — governance state, not social. */
@Entity
@Table(name = "moderation_restrictions")
public class ModerationRestrictionEntity {

    public enum Status { ACTIVE, EXPIRED, LIFTED }

    @Id
    private UUID id;

    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "restriction_type", nullable = false, length = 40)
    private String restrictionType;                  // POSTING/COMMENTING/MESSAGING/LOGIN/HIDDEN/...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt = OffsetDateTime.now();

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    protected ModerationRestrictionEntity() {}

    public ModerationRestrictionEntity(String targetType, UUID targetId, String restrictionType,
                                       OffsetDateTime expiresAt, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.targetType = targetType;
        this.targetId = targetId;
        this.restrictionType = restrictionType;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public Status getStatus() { return status; }
    public String getRestrictionType() { return restrictionType; }
}
