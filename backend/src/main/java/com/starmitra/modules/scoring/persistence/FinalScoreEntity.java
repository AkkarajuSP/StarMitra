package com.starmitra.modules.scoring.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Sealed, reproducible scoring result — versioned per scoring_config. */
@Entity
@Table(name = "final_scores")
public class FinalScoreEntity {

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "round_id", nullable = false)
    private UUID roundId;

    @Column(name = "scoring_config_id", nullable = false)
    private UUID scoringConfigId;

    @Column(name = "score_version", nullable = false)
    private int scoreVersion;

    @Column(name = "final_score", nullable = false, precision = 10, scale = 4)
    private BigDecimal finalScore;

    @Column(nullable = false, length = 20)
    private String status = "CALCULATED";            // CALCULATED → SEALED

    @Column(name = "sealed_at")
    private OffsetDateTime sealedAt;

    protected FinalScoreEntity() {}

    public FinalScoreEntity(UUID submissionId, UUID competitionId, UUID categoryId, UUID roundId,
                            UUID scoringConfigId, int scoreVersion, BigDecimal finalScore) {
        this.id = UUID.randomUUID();
        this.submissionId = submissionId;
        this.competitionId = competitionId;
        this.categoryId = categoryId;
        this.roundId = roundId;
        this.scoringConfigId = scoringConfigId;
        this.scoreVersion = scoreVersion;
        this.finalScore = finalScore;
    }

    public void seal() {
        this.status = "SEALED";
        this.sealedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getSubmissionId() { return submissionId; }
    public UUID getCompetitionId() { return competitionId; }
    public UUID getCategoryId() { return categoryId; }
    public UUID getRoundId() { return roundId; }
    public int getScoreVersion() { return scoreVersion; }
    public String getStatus() { return status; }
    public BigDecimal getFinalScore() { return finalScore; }
}
