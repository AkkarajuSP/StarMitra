package com.starmitra.modules.submission.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "submissions")
public class SubmissionEntity {

    /** Contract-visible states only: DRAFT→SUBMITTED→FINALIZED; →WITHDRAWN. */
    public enum State { DRAFT, SUBMITTED, FINALIZED, WITHDRAWN }

    @Id
    private UUID id;

    @Column(name = "participant_id", nullable = false)
    private UUID participantId;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;                   // REF → competitions

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;                      // REF → competition_categories

    @Column(name = "round_id", nullable = false)
    private UUID roundId;                         // REF → competition_rounds

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private State state = State.DRAFT;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "finalized_at")
    private OffsetDateTime finalizedAt;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected SubmissionEntity() {}

    public SubmissionEntity(UUID participantId, UUID competitionId, UUID categoryId, UUID roundId) {
        this.id = UUID.randomUUID();
        this.participantId = participantId;
        this.competitionId = competitionId;
        this.categoryId = categoryId;
        this.roundId = roundId;
    }

    public void submit() {
        this.state = State.SUBMITTED;
        this.submittedAt = OffsetDateTime.now();
        touch();
    }

    public void finalize() {
        this.state = State.FINALIZED;
        this.finalizedAt = OffsetDateTime.now();
        touch();
    }

    public void withdraw() {
        this.state = State.WITHDRAWN;
        touch();
    }

    private void touch() { this.updatedAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public UUID getParticipantId() { return participantId; }
    public UUID getCompetitionId() { return competitionId; }
    public UUID getCategoryId() { return categoryId; }
    public UUID getRoundId() { return roundId; }
    public State getState() { return state; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public OffsetDateTime getFinalizedAt() { return finalizedAt; }
    public int getVersion() { return version; }
}
