package com.starmitra.modules.pricing.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** M22 entitlement definition — value_kind dictates how plan_entitlements.value parses. */
@Entity
@Table(name = "entitlements")
public class EntitlementEntity {

    public enum ValueKind { BOOLEAN, INTEGER, ENUM }
    public enum Status { ACTIVE, INACTIVE }

    @Id
    @Column(length = 60)
    private String code;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_kind", nullable = false, length = 20)
    private ValueKind valueKind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected EntitlementEntity() {}

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public ValueKind getValueKind() { return valueKind; }
    public Status getStatus() { return status; }
}
