package com.starmitra.modules.progression.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Versioned progression rules — JSONB; frozen once progression begins. */
@Entity
@Table(name = "progression_configurations")
public class ProgressionConfigEntity {

    @Id
    private UUID id;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    /** {sourceRoundId?, targetRoundId?, maxAdvance?, tiePolicy?} — data-driven. */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "rule_payload", nullable = false, columnDefinition = "jsonb")
    private String rulePayload;

    @Column(nullable = false, length = 20)
    private String status = "PUBLISHED";

    @Column(name = "frozen_at")
    private OffsetDateTime frozenAt;

    protected ProgressionConfigEntity() {}

    public ProgressionConfigEntity(UUID competitionId, int versionNo, String rulePayload) {
        this.id = UUID.randomUUID();
        this.competitionId = competitionId;
        this.versionNo = versionNo;
        this.rulePayload = rulePayload;
    }

    public void freeze() { this.frozenAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public UUID getCompetitionId() { return competitionId; }
    public String getRulePayload() { return rulePayload; }
}
