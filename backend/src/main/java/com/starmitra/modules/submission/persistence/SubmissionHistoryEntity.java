package com.starmitra.modules.submission.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Append-only state trail — never updated. */
@Entity
@Table(name = "submission_history")
public class SubmissionHistoryEntity {

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "from_state", length = 20)
    private String fromState;

    @Column(name = "to_state", nullable = false, length = 20)
    private String toState;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected SubmissionHistoryEntity() {}

    public SubmissionHistoryEntity(UUID submissionId, String fromState, String toState,
                                   UUID actorId, String reason) {
        this.id = UUID.randomUUID();
        this.submissionId = submissionId;
        this.fromState = fromState;
        this.toState = toState;
        this.actorId = actorId;
        this.reason = reason;
    }

    public String getFromState() { return fromState; }
    public String getToState() { return toState; }
    public UUID getActorId() { return actorId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
