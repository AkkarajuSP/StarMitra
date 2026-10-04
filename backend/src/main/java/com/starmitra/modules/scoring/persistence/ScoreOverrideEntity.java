package com.starmitra.modules.scoring.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Append-only override — never mutates final_scores or source votes/evals. */
@Entity
@Table(name = "score_overrides")
public class ScoreOverrideEntity {

    @Id
    private UUID id;

    @Column(name = "final_score_id", nullable = false)
    private UUID finalScoreId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "before_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal beforeValue;

    @Column(name = "after_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal afterValue;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected ScoreOverrideEntity() {}

    public ScoreOverrideEntity(UUID finalScoreId, UUID actorId, BigDecimal before,
                               BigDecimal after, String reason) {
        this.id = UUID.randomUUID();
        this.finalScoreId = finalScoreId;
        this.actorId = actorId;
        this.beforeValue = before;
        this.afterValue = after;
        this.reason = reason;
    }
}
