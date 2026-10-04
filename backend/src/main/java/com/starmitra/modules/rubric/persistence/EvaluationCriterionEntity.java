package com.starmitra.modules.rubric.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "evaluation_criteria")
public class EvaluationCriterionEntity {

    @Id
    private UUID id;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "max_score", precision = 6, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "sort_order")
    private Integer sortOrder;

    protected EvaluationCriterionEntity() {}

    public EvaluationCriterionEntity(UUID versionId, String name, String description,
                                     BigDecimal weight, BigDecimal maxScore, Integer sortOrder) {
        this.id = UUID.randomUUID();
        this.versionId = versionId;
        this.name = name;
        this.description = description;
        this.weight = weight;
        this.maxScore = maxScore;
        this.sortOrder = sortOrder;
    }

    public UUID getId() { return id; }
    public UUID getVersionId() { return versionId; }
    public String getName() { return name; }
    public BigDecimal getWeight() { return weight; }
    public BigDecimal getMaxScore() { return maxScore; }
    public Integer getSortOrder() { return sortOrder; }
}
