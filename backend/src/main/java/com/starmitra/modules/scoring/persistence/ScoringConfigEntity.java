package com.starmitra.modules.scoring.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Versioned weight config — ck_sc_weights enforces audience+judge = 100. */
@Entity
@Table(name = "scoring_configurations")
public class ScoringConfigEntity {

    @Id
    private UUID id;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "weight_audience", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightAudience;

    @Column(name = "weight_judge", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightJudge;

    @Column(nullable = false, length = 20)
    private String status = "PUBLISHED";

    @Column(name = "published_at")
    private OffsetDateTime publishedAt = OffsetDateTime.now();

    protected ScoringConfigEntity() {}

    public ScoringConfigEntity(UUID competitionId, int versionNo,
                               BigDecimal weightAudience, BigDecimal weightJudge) {
        this.id = UUID.randomUUID();
        this.competitionId = competitionId;
        this.versionNo = versionNo;
        this.weightAudience = weightAudience;
        this.weightJudge = weightJudge;
    }

    public UUID getId() { return id; }
    public UUID getCompetitionId() { return competitionId; }
    public BigDecimal getWeightAudience() { return weightAudience; }
    public BigDecimal getWeightJudge() { return weightJudge; }
}
