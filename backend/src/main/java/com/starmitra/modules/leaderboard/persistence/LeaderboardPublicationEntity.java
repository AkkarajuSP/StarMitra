package com.starmitra.modules.leaderboard.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Authoritative visibility state — HIDDEN/PUBLISHED/ARCHIVED (admin-controlled). */
@Entity
@Table(name = "leaderboard_publications")
public class LeaderboardPublicationEntity {

    public enum Status { HIDDEN, PUBLISHED, ARCHIVED }

    @Id
    private UUID id;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "round_id", nullable = false)
    private UUID roundId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.HIDDEN;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected LeaderboardPublicationEntity() {}

    public LeaderboardPublicationEntity(UUID competitionId, UUID categoryId, UUID roundId,
                                        Status status, UUID actor) {
        this.id = UUID.randomUUID();
        this.competitionId = competitionId;
        this.categoryId = categoryId;
        this.roundId = roundId;
        apply(status, actor);
    }

    public void apply(Status to, UUID actor) {
        this.status = to;
        this.publishedBy = actor;
        this.publishedAt = to == Status.PUBLISHED ? OffsetDateTime.now() : publishedAt;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public Status getStatus() { return status; }
    public UUID getCompetitionId() { return competitionId; }
    public UUID getCategoryId() { return categoryId; }
    public UUID getRoundId() { return roundId; }
}
