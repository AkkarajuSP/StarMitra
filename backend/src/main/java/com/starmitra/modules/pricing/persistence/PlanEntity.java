package com.starmitra.modules.pricing.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** M22 plan catalog row — pricing is data, never code. */
@Entity
@Table(name = "plans")
public class PlanEntity {

    public enum PlanType { SUBSCRIPTION, EVENT_PACKAGE }
    public enum BillingPeriod { NONE, MONTH, EVENT }
    public enum Status { ACTIVE, INACTIVE }

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 30)
    private PlanType planType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false, length = 20)
    private BillingPeriod billingPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "effective_from")
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected PlanEntity() {}

    /** Purchasable = ACTIVE and inside its effective window. */
    public boolean purchasableAt(OffsetDateTime at) {
        if (status != Status.ACTIVE) return false;
        if (effectiveFrom != null && effectiveFrom.isAfter(at)) return false;
        return effectiveTo == null || effectiveTo.isAfter(at);
    }

    public void update(String displayName, String description, BigDecimal price,
                       Status status, Integer sortOrder,
                       OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo) {
        if (displayName != null) this.displayName = displayName;
        if (description != null) this.description = description;
        if (price != null) this.price = price;
        if (status != null) this.status = status;
        if (sortOrder != null) this.sortOrder = sortOrder;
        if (effectiveFrom != null) this.effectiveFrom = effectiveFrom;
        if (effectiveTo != null) this.effectiveTo = effectiveTo;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public PlanType getPlanType() { return planType; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public BillingPeriod getBillingPeriod() { return billingPeriod; }
    public Status getStatus() { return status; }
    public int getSortOrder() { return sortOrder; }
    public OffsetDateTime getEffectiveFrom() { return effectiveFrom; }
    public OffsetDateTime getEffectiveTo() { return effectiveTo; }
}
