package com.starmitra.modules.voting.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** M11-owned behavior config — versioned by (series_key, version_no). */
@Entity
@Table(name = "vote_configs")
public class VoteConfigEntity {

    @Id
    private UUID id;

    @Column(name = "series_key", nullable = false, length = 80)
    private String seriesKey;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    /** Freeform payload: {maxVotesPerVoter, windowStart, windowEnd, ...} — data-driven. */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected VoteConfigEntity() {}

    public VoteConfigEntity(String seriesKey, int versionNo, String payload) {
        this.id = UUID.randomUUID();
        this.seriesKey = seriesKey;
        this.versionNo = versionNo;
        this.payload = payload;
        this.publishedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getPayload() { return payload; }
}
