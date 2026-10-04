package com.starmitra.modules.rubric.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "evaluation_templates")
public class EvaluationTemplateEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected EvaluationTemplateEntity() {}

    public EvaluationTemplateEntity(String name, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void touch() { this.updatedAt = OffsetDateTime.now(); }
}
