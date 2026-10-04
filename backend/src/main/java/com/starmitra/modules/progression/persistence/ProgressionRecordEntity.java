package com.starmitra.modules.progression.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** One decision per submission+round+config_version (UQ) — idempotent by DB. */
@Entity
@Table(name = "progression_records")
public class ProgressionRecordEntity {

    public enum Outcome { ADVANCED, ELIMINATED, PENDING }

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "round_id", nullable = false)
    private UUID roundId;                            // decision round

    @Column(name = "source_round_id")
    private UUID sourceRoundId;

    @Column(name = "target_round_id")
    private UUID targetRoundId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Outcome outcome;

    @Column(name = "config_version_id", nullable = false)
    private UUID configVersionId;

    @Column(name = "finalized_at")
    private OffsetDateTime finalizedAt;

    protected ProgressionRecordEntity() {}

    public ProgressionRecordEntity(UUID submissionId, UUID roundId, UUID sourceRoundId,
                                   UUID targetRoundId, Outcome outcome, UUID configVersionId) {
        this.id = UUID.randomUUID();
        this.submissionId = submissionId;
        this.roundId = roundId;
        this.sourceRoundId = sourceRoundId;
        this.targetRoundId = targetRoundId;
        this.outcome = outcome;
        this.configVersionId = configVersionId;
    }

    public void finalize() { this.finalizedAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public UUID getSubmissionId() { return submissionId; }
    public UUID getRoundId() { return roundId; }
    public UUID getSourceRoundId() { return sourceRoundId; }
    public UUID getTargetRoundId() { return targetRoundId; }
    public Outcome getOutcome() { return outcome; }
    public UUID getConfigVersionId() { return configVersionId; }
}
