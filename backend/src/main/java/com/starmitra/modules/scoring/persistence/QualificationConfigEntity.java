package com.starmitra.modules.scoring.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Versioned qualification rules — JSONB {type:TOP_N|MIN_SCORE|TOP_N_AND_MIN,...}. */
@Entity
@Table(name = "qualification_configurations")
public class QualificationConfigEntity {

    @Id
    private UUID id;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "rule_payload", nullable = false, columnDefinition = "jsonb")
    private String rulePayload;

    @Column(nullable = false, length = 20)
    private String status = "PUBLISHED";

    @Column(name = "published_at")
    private OffsetDateTime publishedAt = OffsetDateTime.now();

    protected QualificationConfigEntity() {}

    public QualificationConfigEntity(UUID competitionId, int versionNo, String rulePayload) {
        this.id = UUID.randomUUID();
        this.competitionId = competitionId;
        this.versionNo = versionNo;
        this.rulePayload = rulePayload;
    }

    public UUID getId() { return id; }
    public String getRulePayload() { return rulePayload; }
}
