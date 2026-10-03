package com.starmitra.platform.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Kernel audit record — append-only semantics (insert-only DB grants at deployment). */
@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_context", length = 80)
    private String actorContext;

    @Column(nullable = false, length = 20)
    private String module;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "target_type", length = 60)
    private String targetType;

    @Column(name = "target_id", length = 80)
    private String targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_ref", columnDefinition = "jsonb")
    private String beforeRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_ref", columnDefinition = "jsonb")
    private String afterRef;

    private String reason;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected AuditLogEntity() {}

    public AuditLogEntity(String module, String action, UUID actorId, String actorContext,
                          String targetType, String targetId, String reason) {
        this.id = UUID.randomUUID();
        this.module = module;
        this.action = action;
        this.actorId = actorId;
        this.actorContext = actorContext;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        String corr = com.starmitra.platform.correlation.CorrelationIdFilter.current();
        try { this.correlationId = UUID.fromString(corr); } catch (IllegalArgumentException ignored) {}
    }

    public void setBeforeRef(String json) { this.beforeRef = json; }
    public void setAfterRef(String json) { this.afterRef = json; }
    public UUID getId() { return id; }
}
