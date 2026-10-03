package com.starmitra.modules.identity.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

/** M01-owned authentication event record — append-only semantics. */
@Entity
@Table(name = "authentication_audit_events")
public class AuthenticationAuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected AuthenticationAuditEventEntity() {}

    public AuthenticationAuditEventEntity(UUID userId, String eventType, String metadata) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.eventType = eventType;
        this.metadata = metadata;
    }

    public UUID getId() { return id; }
}
