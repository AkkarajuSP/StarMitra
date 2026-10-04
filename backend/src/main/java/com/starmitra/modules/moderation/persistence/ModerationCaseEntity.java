package com.starmitra.modules.moderation.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Review unit — reports are linked via evidence references (never deleted). */
@Entity
@Table(name = "moderation_cases")
public class ModerationCaseEntity {

    public enum Status { OPEN, UNDER_REVIEW, RESOLVED, DISMISSED }

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "assigned_moderator_id")
    private UUID assignedModeratorId;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt = OffsetDateTime.now();

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public ModerationCaseEntity() { this.id = UUID.randomUUID(); }

    public void transition(Status to, UUID moderator) {
        if (this.status == Status.RESOLVED || this.status == Status.DISMISSED) {
            throw new com.starmitra.platform.error.ApiException(
                    com.starmitra.platform.error.ErrorCode.STATE_TRANSITION_INVALID,
                    "Closed case");
        }
        this.status = to;
        this.assignedModeratorId = moderator;
        if (to == Status.RESOLVED || to == Status.DISMISSED) this.closedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public Status getStatus() { return status; }
    public UUID getAssignedModeratorId() { return assignedModeratorId; }
}
