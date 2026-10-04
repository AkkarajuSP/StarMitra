package com.starmitra.modules.moderation.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Append-only decision — never overwritten. */
@Entity
@Table(name = "moderation_decisions")
public class ModerationDecisionEntity {

    public enum Type { NO_ACTION, WARNING, CONTENT_RESTRICTED, CONTENT_HIDDEN,
                       CONTENT_REMOVED, USER_RESTRICTED, USER_SUSPENDED }

    @Id
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "moderator_id", nullable = false)
    private UUID moderatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 40)
    private Type decisionType;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "policy_version_id")
    private UUID policyVersionId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected ModerationDecisionEntity() {}

    public ModerationDecisionEntity(UUID caseId, UUID moderatorId, Type decisionType,
                                    String reason, UUID policyVersionId) {
        this.id = UUID.randomUUID();
        this.caseId = caseId;
        this.moderatorId = moderatorId;
        this.decisionType = decisionType;
        this.reason = reason;
        this.policyVersionId = policyVersionId;
    }

    public UUID getId() { return id; }
    public Type getDecisionType() { return decisionType; }
}
