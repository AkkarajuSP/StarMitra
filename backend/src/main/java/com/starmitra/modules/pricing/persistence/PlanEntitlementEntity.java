package com.starmitra.modules.pricing.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "plan_entitlements")
@IdClass(PlanEntitlementEntity.Pk.class)
public class PlanEntitlementEntity {

    public static class Pk implements Serializable {
        private UUID planId;
        private String entitlementCode;
        public Pk() {}
        public Pk(UUID planId, String entitlementCode) {
            this.planId = planId; this.entitlementCode = entitlementCode;
        }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && planId.equals(p.planId)
                    && entitlementCode.equals(p.entitlementCode);
        }
        @Override public int hashCode() { return Objects.hash(planId, entitlementCode); }
    }

    @Id
    @Column(name = "plan_id")
    private UUID planId;

    @Id
    @Column(name = "entitlement_code", length = 60)
    private String entitlementCode;

    /** Raw value; interpreted via the entitlement's value_kind
     *  (BOOLEAN 'true'/'false', INTEGER decimal (negative = unlimited), ENUM literal). */
    @Column(nullable = false, length = 120)
    private String value;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected PlanEntitlementEntity() {}

    public PlanEntitlementEntity(UUID planId, String entitlementCode, String value) {
        this.planId = planId;
        this.entitlementCode = entitlementCode;
        this.value = value;
    }

    public void setValue(String value) {
        this.value = value;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getPlanId() { return planId; }
    public String getEntitlementCode() { return entitlementCode; }
    public String getValue() { return value; }
}
