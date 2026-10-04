package com.starmitra.modules.competition.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "eligibility_rules")
public class EligibilityRuleEntity {

    @Id
    private UUID id;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "rule_type", nullable = false, length = 40)
    private String ruleType;

    /** Freeform rule params (JSONB) — evaluator is pluggable per rule_type. */
    @Column(name = "rule_params", columnDefinition = "jsonb")
    private String ruleParams;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected EligibilityRuleEntity() {}

    public EligibilityRuleEntity(UUID competitionId, String ruleType, String ruleParams) {
        this.id = UUID.randomUUID();
        this.competitionId = competitionId;
        this.ruleType = ruleType;
        this.ruleParams = ruleParams;
    }

    public UUID getId() { return id; }
    public String getRuleType() { return ruleType; }
}
