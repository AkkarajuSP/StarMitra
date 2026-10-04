package com.starmitra.modules.moderation.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Enforcement action row — owning module applies the actual state via contract. */
@Entity
@Table(name = "moderation_actions")
public class ModerationActionEntity {

    @Id
    private UUID id;

    @Column(name = "decision_id", nullable = false)
    private UUID decisionId;

    @Column(name = "action_type", nullable = false, length = 40)
    private String actionType;

    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(nullable = false, length = 20)
    private String status = "APPLIED";

    @Column(name = "executed_at")
    private OffsetDateTime executedAt = OffsetDateTime.now();

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    protected ModerationActionEntity() {}

    public ModerationActionEntity(UUID decisionId, String actionType, String targetType,
                                  UUID targetId, OffsetDateTime expiresAt) {
        this.id = UUID.randomUUID();
        this.decisionId = decisionId;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.expiresAt = expiresAt;
    }
}
