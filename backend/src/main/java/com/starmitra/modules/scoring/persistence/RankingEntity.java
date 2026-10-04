package com.starmitra.modules.scoring.persistence;

import jakarta.persistence.*;
import java.util.UUID;

/** Derived ranking snapshot — reproducible, versioned. */
@Entity
@Table(name = "rankings")
public class RankingEntity {

    @Id
    private UUID id;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "round_id", nullable = false)
    private UUID roundId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(nullable = false)
    private int rank;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "tie_break_applied", columnDefinition = "jsonb")
    private String tieBreakApplied;                  // {"tied":true} when policy can't split

    @Column(name = "snapshot_version", nullable = false)
    private int snapshotVersion;

    protected RankingEntity() {}

    public RankingEntity(UUID competitionId, UUID categoryId, UUID roundId, UUID submissionId,
                         int rank, String tieBreakApplied, int snapshotVersion) {
        this.id = UUID.randomUUID();
        this.competitionId = competitionId;
        this.categoryId = categoryId;
        this.roundId = roundId;
        this.submissionId = submissionId;
        this.rank = rank;
        this.tieBreakApplied = tieBreakApplied;
        this.snapshotVersion = snapshotVersion;
    }

    public UUID getSubmissionId() { return submissionId; }
    public int getRank() { return rank; }
}
