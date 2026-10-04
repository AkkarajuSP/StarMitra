package com.starmitra.modules.scoring.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Versioned tie-break policy — JSONB {policy:"JUDGE_SCORE"|"AUDIENCE_SCORE"|...}. */
@Entity
@Table(name = "tie_break_configurations")
public class TieBreakConfigEntity {

    @Id
    private UUID id;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String criteria;

    @Column(nullable = false, length = 20)
    private String status = "PUBLISHED";

    @Column(name = "published_at")
    private OffsetDateTime publishedAt = OffsetDateTime.now();

    protected TieBreakConfigEntity() {}

    public TieBreakConfigEntity(UUID competitionId, int versionNo, String criteria) {
        this.id = UUID.randomUUID();
        this.competitionId = competitionId;
        this.versionNo = versionNo;
        this.criteria = criteria;
    }

    public UUID getId() { return id; }
    public String getCriteria() { return criteria; }
}
