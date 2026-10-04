package com.starmitra.modules.leaderboard.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Sealed version-bound reference — M14 result_version refs captured (reproducible). */
@Entity
@Table(name = "leaderboard_snapshots")
public class LeaderboardSnapshotEntity {

    @Id
    private UUID id;

    @Column(name = "leaderboard_publication_id", nullable = false)
    private UUID publicationId;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "result_version_refs", nullable = false, columnDefinition = "jsonb")
    private String resultVersionRefs;                // {rankingSnapshotVersion, configVersionId,...}

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt = OffsetDateTime.now();

    protected LeaderboardSnapshotEntity() {}

    public LeaderboardSnapshotEntity(UUID publicationId, String refs) {
        this.id = UUID.randomUUID();
        this.publicationId = publicationId;
        this.resultVersionRefs = refs;
    }
}
