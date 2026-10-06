package com.starmitra.modules.pricing.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * M22 usage counters — owning modules report consumption through the
 * EntitlementContract (recordUsage/releaseUsage); M22 never counts their rows.
 */
@Entity
@Table(name = "entitlement_usage")
@IdClass(EntitlementUsageEntity.Pk.class)
public class EntitlementUsageEntity {

    public static class Pk implements Serializable {
        private UUID userId;
        private String entitlementCode;
        public Pk() {}
        public Pk(UUID userId, String entitlementCode) {
            this.userId = userId; this.entitlementCode = entitlementCode;
        }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && userId.equals(p.userId)
                    && entitlementCode.equals(p.entitlementCode);
        }
        @Override public int hashCode() { return Objects.hash(userId, entitlementCode); }
    }

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "entitlement_code", length = 60)
    private String entitlementCode;

    @Column(nullable = false)
    private long used;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected EntitlementUsageEntity() {}

    public EntitlementUsageEntity(UUID userId, String entitlementCode, long used) {
        this.userId = userId;
        this.entitlementCode = entitlementCode;
        this.used = used;
    }

    public void add(long delta) {
        this.used += delta;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getUserId() { return userId; }
    public String getEntitlementCode() { return entitlementCode; }
    public long getUsed() { return used; }
}
