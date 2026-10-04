package com.starmitra.modules.rubric.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Versioned rubric — DRAFT editable, PUBLISHED **immutable** (app-enforced). */
@Entity
@Table(name = "evaluation_template_versions")
public class EvaluationTemplateVersionEntity {

    public enum Status { DRAFT, VALIDATED, PUBLISHED, RETIRED }

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    /** JSONB: {applicability:{competitionId,categoryId,roundId}, scoreScale:{min,max}}. */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload = "{}";

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected EvaluationTemplateVersionEntity() {}

    public EvaluationTemplateVersionEntity(UUID templateId, int versionNo, String payload) {
        this.id = UUID.randomUUID();
        this.templateId = templateId;
        this.versionNo = versionNo;
        this.payload = payload;
    }

    public boolean isPublished() { return status == Status.PUBLISHED; }

    public void publish() {
        this.status = Status.PUBLISHED;
        this.publishedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void setPayload(String payload) {
        this.payload = payload;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTemplateId() { return templateId; }
    public int getVersionNo() { return versionNo; }
    public Status getStatus() { return status; }
    public String getPayload() { return payload; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
