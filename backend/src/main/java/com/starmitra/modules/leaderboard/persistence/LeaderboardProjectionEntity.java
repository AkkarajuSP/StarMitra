package com.starmitra.modules.leaderboard.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Derived/rebuildable read model — NEVER authoritative ranking truth.
 * Source: M14 final_scores+rankings; M16 only projects.
 */
@Entity
@Table(name = "leaderboard_projections")
public class LeaderboardProjectionEntity {

    @Id
    private UUID id;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "round_id", nullable = false)
    private UUID roundId;

    @Column(name = "entry_ref", nullable = false)
    private UUID entryRef;                           // submission/participant — one unit

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "display_payload", columnDefinition = "jsonb")
    private String displayPayload;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "refreshed_at", nullable = false)
    private OffsetDateTime refreshedAt = OffsetDateTime.now();

    protected LeaderboardProjectionEntity() {}

    public LeaderboardProjectionEntity(UUID competitionId, UUID categoryId, UUID roundId,
                                       UUID entryRef, String displayPayload, Integer rank) {
        this.id = UUID.randomUUID();
        this.competitionId = competitionId;
        this.categoryId = categoryId;
        this.roundId = roundId;
        this.entryRef = entryRef;
        this.displayPayload = displayPayload;
        this.rank = rank;
    }

    public UUID getEntryRef() { return entryRef; }
    public Integer getRank() { return rank; }
    public String getDisplayPayload() { return displayPayload; }
    public UUID getCategoryId() { return categoryId; }
    public UUID getRoundId() { return roundId; }
}
