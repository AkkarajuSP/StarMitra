package com.starmitra.modules.pricing.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * M22 user-plan assignment. At most one ACTIVE row per user
 * (uq_us_current partial index). Source records how the plan was granted —
 * PAYMENT exists only as a future source; no payment rows are created in MVP.
 */
@Entity
@Table(name = "user_subscriptions")
public class UserSubscriptionEntity {

    public enum Status { ACTIVE, EXPIRED, CANCELLED, PENDING_PAYMENT }
    public enum Source { SYSTEM_DEFAULT, USER_SELECTION, ADMIN_GRANT, PAYMENT }

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Source source;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt = OffsetDateTime.now();

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected UserSubscriptionEntity() {}

    public UserSubscriptionEntity(UUID userId, UUID planId, Source source) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.planId = planId;
        this.source = source;
    }

    /** In-force at 'at': ACTIVE, started, and not yet ended. */
    public boolean inForceAt(OffsetDateTime at) {
        return status == Status.ACTIVE
                && !startsAt.isAfter(at)
                && (endsAt == null || endsAt.isAfter(at));
    }

    public void end(Status terminal, OffsetDateTime at) {
        this.status = terminal;
        this.endsAt = at;
        this.updatedAt = at;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getPlanId() { return planId; }
    public Status getStatus() { return status; }
    public Source getSource() { return source; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
}
