package com.starmitra.modules.portfolio.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "portfolio_item_contributions")
public class PortfolioItemContributionEntity {

    @Id
    private UUID id;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "project_credit_id", nullable = false)
    private UUID projectCreditId;                 // REF → project_credits (no FK; M07)

    protected PortfolioItemContributionEntity() {}

    public PortfolioItemContributionEntity(UUID itemId, UUID projectCreditId) {
        this.id = UUID.randomUUID();
        this.itemId = itemId;
        this.projectCreditId = projectCreditId;
    }

    public UUID getId() { return id; }
    public UUID getItemId() { return itemId; }
    public UUID getProjectCreditId() { return projectCreditId; }
}
