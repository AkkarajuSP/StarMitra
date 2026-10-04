package com.starmitra.modules.scoring.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Derived/rebuildable judge aggregation (0–100). */
@Entity
@Table(name = "judge_score_aggregations")
public class JudgeScoreAggEntity {

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "round_id", nullable = false)
    private UUID roundId;

    @Column(name = "config_version_id", nullable = false)
    private UUID configVersionId;

    @Column(precision = 10, scale = 4)
    private BigDecimal aggregate;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt = OffsetDateTime.now();

    protected JudgeScoreAggEntity() {}

    public JudgeScoreAggEntity(UUID submissionId, UUID roundId, UUID configVersionId, BigDecimal aggregate) {
        this.id = UUID.randomUUID();
        this.submissionId = submissionId;
        this.roundId = roundId;
        this.configVersionId = configVersionId;
        this.aggregate = aggregate;
    }

    public UUID getSubmissionId() { return submissionId; }
    public UUID getRoundId() { return roundId; }
    public UUID getConfigVersionId() { return configVersionId; }
    public BigDecimal getAggregate() { return aggregate; }
}
