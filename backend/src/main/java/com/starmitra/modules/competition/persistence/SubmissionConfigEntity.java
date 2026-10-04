package com.starmitra.modules.competition.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "submission_configs")
public class SubmissionConfigEntity {

    @Id
    private UUID id;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "category_id")
    private UUID categoryId;                       // null = competition-wide config

    /** Freeform config JSONB — deadlines, media rules, team config per FRS. */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String config;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected SubmissionConfigEntity() {}

    public SubmissionConfigEntity(UUID competitionId, UUID categoryId, String config) {
        this.id = UUID.randomUUID();
        this.competitionId = competitionId;
        this.categoryId = categoryId;
        this.config = config;
    }

    public void updateConfig(String config) {
        this.config = config;
        this.updatedAt = OffsetDateTime.now();
    }

    public int getVersion() { return version; }
}
