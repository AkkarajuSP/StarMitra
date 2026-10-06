package com.starmitra.modules.pricing.application;

import com.starmitra.modules.pricing.persistence.*;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.security.SystemRoleGuard;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * M22 — Pricing & Entitlements. Owns the plan catalog, per-plan entitlement
 * values, and user plan assignments (user_subscriptions).
 *
 * Rules honoured here:
 *  - Every user resolves to a plan; absent any row they lazily receive FREE
 *    (same lazy-init idiom as M08 portfolios — uq_us_current converges races).
 *  - FREE is activatable without payment. Any priced plan selection fails
 *    closed with PAYMENT_NOT_ENABLED — no subscription row is created and no
 *    payment state is simulated. The future provider seam is
 *    {@link PaymentProviderContract} (no implementation in MVP).
 *  - Business modules never see plan/pricing concepts — only entitlement
 *    codes via {@link EntitlementContract}.
 */
@Service
public class PricingService implements EntitlementContract, UserPlanContract {

    public static final String DEFAULT_PLAN_CODE = "FREE";

    private final PlanRepository plans;
    private final EntitlementRepository entitlements;
    private final PlanEntitlementRepository planEntitlements;
    private final UserSubscriptionRepository subscriptions;
    private final EntitlementUsageRepository usage;
    private final AuditService audit;

    public PricingService(PlanRepository plans, EntitlementRepository entitlements,
                          PlanEntitlementRepository planEntitlements,
                          UserSubscriptionRepository subscriptions,
                          EntitlementUsageRepository usage, AuditService audit) {
        this.plans = plans;
        this.entitlements = entitlements;
        this.planEntitlements = planEntitlements;
        this.subscriptions = subscriptions;
        this.usage = usage;
        this.audit = audit;
    }

    // ---------- views ----------

    public record PlanView(UUID id, String code, String displayName, String description,
                           String planType, BigDecimal price, String currency,
                           String billingPeriod, String status, int sortOrder,
                           Map<String, String> entitlements) {}

    public record ResolvedEntitlement(String code, String kind, String value,
                                      Long limit, Long remaining) {}

    public record UserPlanView(String planCode, String displayName, String subscriptionStatus,
                               String source, OffsetDateTime startsAt, OffsetDateTime endsAt,
                               List<ResolvedEntitlement> entitlements) {}

    public record PlanUpdate(String displayName, String description, BigDecimal price,
                             String status, Integer sortOrder,
                             OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo) {}

    // ---------- catalog ----------

    /** Public catalog — ACTIVE plans inside their effective window. */
    @Transactional(readOnly = true)
    public List<PlanView> catalog() {
        var now = OffsetDateTime.now();
        return plans.findAllByOrderBySortOrderAsc().stream()
                .filter(p -> p.purchasableAt(now))
                .map(this::toPlanView)
                .toList();
    }

    /** Admin catalog — every plan regardless of status/window. */
    @Transactional(readOnly = true)
    public List<PlanView> adminCatalog() {
        SystemRoleGuard.requireAdmin();
        return plans.findAllByOrderBySortOrderAsc().stream().map(this::toPlanView).toList();
    }

    // ---------- user plan ----------

    @Override
    @Transactional
    public void assignDefaultPlan(UUID userId) {
        ensureSubscription(userId);
    }

    /**
     * Current plan + resolved entitlements. Lazily assigns FREE when no
     * ACTIVE subscription exists (covers users created before M22 and any
     * path that skipped the registration seam).
     */
    @Transactional
    public UserPlanView myPlan(UUID userId) {
        var sub = ensureSubscription(userId);
        var plan = plans.findById(sub.getPlanId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        var resolved = planEntitlements.findByPlanId(plan.getId()).stream()
                .map(pe -> resolve(pe, userId))
                .flatMap(Optional::stream)
                .toList();
        return new UserPlanView(plan.getCode(), plan.getDisplayName(),
                sub.getStatus().name(), sub.getSource().name(),
                sub.getStartsAt(), sub.getEndsAt(), resolved);
    }

    /**
     * Plan selection. FREE activates immediately (USER_SELECTION); any priced
     * plan — monthly or per-event — fails closed: PAYMENT_NOT_ENABLED, no row
     * created, nothing that could read as a successful purchase.
     */
    @Transactional
    public UserPlanView selectPlan(UUID userId, String planCode) {
        if (planCode == null || planCode.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "planCode required");
        }
        var now = OffsetDateTime.now();
        var plan = plans.findByCode(planCode.trim().toUpperCase())
                .filter(p -> p.purchasableAt(now))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Plan not found"));

        var current = ensureSubscription(userId);
        if (current.getPlanId().equals(plan.getId())) {
            return myPlan(userId);                       // idempotent re-select
        }
        if (plan.getPrice().signum() > 0) {
            audit.record("M22", "PLAN_SELECT_REQUIRES_PAYMENT", userId, "user",
                    "plan", plan.getCode(), null);
            throw new ApiException(ErrorCode.PAYMENT_NOT_ENABLED,
                    "Payment integration coming soon");
        }
        current.end(UserSubscriptionEntity.Status.EXPIRED, now);
        subscriptions.saveAndFlush(current);
        subscriptions.saveAndFlush(new UserSubscriptionEntity(userId, plan.getId(),
                UserSubscriptionEntity.Source.USER_SELECTION));
        audit.record("M22", "PLAN_ACTIVATED", userId, "user", "plan", plan.getCode(), null);
        return myPlan(userId);
    }

    // ---------- entitlement contract ----------

    @Override
    @Transactional
    public boolean hasEntitlement(UUID userId, String entitlementCode) {
        return getEntitlement(userId, entitlementCode).isPresent();
    }

    @Override
    @Transactional
    public Optional<EntitlementValue> getEntitlement(UUID userId, String entitlementCode) {
        var plan = currentPlan(userId);
        return planEntitlements.findByPlanId(plan.getId()).stream()
                .filter(pe -> pe.getEntitlementCode().equals(entitlementCode))
                .findFirst()
                .flatMap(pe -> resolve(pe, userId))
                .map(r -> new EntitlementValue(r.code(), r.kind(), r.limit(), r.value()));
    }

    @Override
    @Transactional
    public boolean checkLimit(UUID userId, String entitlementCode, long requestedAmount) {
        if (requestedAmount < 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "requestedAmount must be >= 0");
        }
        var e = getEntitlement(userId, entitlementCode).orElse(null);
        if (e == null) return false;
        if (!"INTEGER".equals(e.kind())) return true;      // granted non-limit entitlement
        if (e.limit() == null || e.limit() < 0) return true;  // unlimited
        return requestedAmount <= Math.max(0, e.limit() - usedFor(userId, entitlementCode));
    }

    @Override
    @Transactional
    public long getRemainingLimit(UUID userId, String entitlementCode) {
        var e = getEntitlement(userId, entitlementCode).orElse(null);
        if (e == null) return 0;
        if (!"INTEGER".equals(e.kind())) return -1;        // not a limit
        if (e.limit() == null || e.limit() < 0) return -1; // unlimited
        return Math.max(0, e.limit() - usedFor(userId, entitlementCode));
    }

    @Override
    @Transactional
    public void recordUsage(UUID userId, String entitlementCode, long delta) {
        adjustUsage(userId, entitlementCode, delta);
    }

    @Override
    @Transactional
    public void releaseUsage(UUID userId, String entitlementCode, long delta) {
        adjustUsage(userId, entitlementCode, -delta);
    }

    // ---------- admin (M19 orchestrates; M22 stays authoritative) ----------

    @Transactional
    public PlanView adminUpdatePlan(String planCode, PlanUpdate cmd) {
        SystemRoleGuard.requireAdmin();
        var plan = plans.findByCode(planCode)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Plan not found"));
        var status = cmd.status() == null ? null : parseStatus(cmd.status());
        if (cmd.price() != null && cmd.price().signum() < 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "price must be >= 0");
        }
        plan.update(cmd.displayName(), cmd.description(), cmd.price(), status,
                cmd.sortOrder(), cmd.effectiveFrom(), cmd.effectiveTo());
        audit.record("M22", "PLAN_UPDATED", null, "admin", "plan", plan.getCode(), null);
        return toPlanView(plans.saveAndFlush(plan));
    }

    @Transactional
    public PlanView adminSetEntitlement(String planCode, String entitlementCode, String value) {
        SystemRoleGuard.requireAdmin();
        var plan = plans.findByCode(planCode)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Plan not found"));
        var def = entitlements.findById(entitlementCode)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Entitlement not found"));
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "value required");
        }
        validateValue(def, value);
        var key = new PlanEntitlementEntity.Pk(plan.getId(), entitlementCode);
        var row = planEntitlements.findById(key)
                .orElseGet(() -> new PlanEntitlementEntity(plan.getId(), entitlementCode, value));
        row.setValue(value);
        planEntitlements.saveAndFlush(row);
        audit.record("M22", "PLAN_ENTITLEMENT_SET", null, "admin",
                "plan", plan.getCode() + ":" + entitlementCode, null);
        return toPlanView(plan);
    }

    // ---------- internals ----------

    /** Current ACTIVE subscription, creating the default FREE one when absent. */
    private UserSubscriptionEntity ensureSubscription(UUID userId) {
        return subscriptions.findByUserIdAndStatus(userId, UserSubscriptionEntity.Status.ACTIVE)
                .orElseGet(() -> {
                    var free = plans.findByCode(DEFAULT_PLAN_CODE)
                            .filter(p -> p.getStatus() == PlanEntity.Status.ACTIVE)
                            .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                                    "Default plan unavailable"));
                    try {
                        return subscriptions.saveAndFlush(new UserSubscriptionEntity(
                                userId, free.getId(), UserSubscriptionEntity.Source.SYSTEM_DEFAULT));
                    } catch (DataIntegrityViolationException race) {
                        return subscriptions
                                .findByUserIdAndStatus(userId, UserSubscriptionEntity.Status.ACTIVE)
                                .orElseThrow(() -> race);
                    }
                });
    }

    private PlanEntity currentPlan(UUID userId) {
        var sub = ensureSubscription(userId);
        return plans.findById(sub.getPlanId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    /** Resolve a plan_entitlement row against its definition; empty = not granted. */
    private Optional<ResolvedEntitlement> resolve(PlanEntitlementEntity pe, UUID userId) {
        var def = entitlements.findById(pe.getEntitlementCode()).orElse(null);
        if (def == null || def.getStatus() != EntitlementEntity.Status.ACTIVE) {
            return Optional.empty();
        }
        String raw = pe.getValue();
        return switch (def.getValueKind()) {
            case BOOLEAN -> "true".equalsIgnoreCase(raw)
                    ? Optional.of(new ResolvedEntitlement(pe.getEntitlementCode(), "BOOLEAN",
                                                          "true", null, null))
                    : Optional.<ResolvedEntitlement>empty();
            case INTEGER -> {
                long limit;
                try {
                    limit = Long.parseLong(raw.trim());
                } catch (NumberFormatException bad) {
                    yield Optional.<ResolvedEntitlement>empty();
                }
                if (limit == 0) yield Optional.<ResolvedEntitlement>empty();   // zero capacity
                Long remaining = limit < 0 ? -1L
                        : Math.max(0, limit - usedFor(userId, pe.getEntitlementCode()));
                yield Optional.of(new ResolvedEntitlement(pe.getEntitlementCode(), "INTEGER",
                                                          null, limit, remaining));
            }
            case ENUM -> raw == null || raw.isBlank()
                    ? Optional.<ResolvedEntitlement>empty()
                    : Optional.of(new ResolvedEntitlement(pe.getEntitlementCode(), "ENUM",
                                                          raw, null, null));
        };
    }

    private long usedFor(UUID userId, String entitlementCode) {
        return usage.findById(new EntitlementUsageEntity.Pk(userId, entitlementCode))
                .map(EntitlementUsageEntity::getUsed).orElse(0L);
    }

    private void adjustUsage(UUID userId, String entitlementCode, long delta) {
        if (delta == 0) return;
        if (!entitlements.existsById(entitlementCode)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Entitlement not found");
        }
        var key = new EntitlementUsageEntity.Pk(userId, entitlementCode);
        var row = usage.findById(key)
                .orElseGet(() -> new EntitlementUsageEntity(userId, entitlementCode, 0));
        // Pre-validate — a violated CHECK aborts the whole PG transaction;
        // ck_eu_used remains the authoritative backstop for concurrent races.
        if (row.getUsed() + delta < 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "usage cannot go below zero");
        }
        row.add(delta);
        usage.saveAndFlush(row);
    }

    private PlanEntity.Status parseStatus(String status) {
        try {
            return PlanEntity.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid plan status");
        }
    }

    private void validateValue(EntitlementEntity def, String value) {
        switch (def.getValueKind()) {
            case BOOLEAN -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new ApiException(ErrorCode.VALIDATION_FAILED,
                            "BOOLEAN entitlement expects true/false");
                }
            }
            case INTEGER -> {
                try {
                    Long.parseLong(value.trim());
                } catch (NumberFormatException e) {
                    throw new ApiException(ErrorCode.VALIDATION_FAILED,
                            "INTEGER entitlement expects a number");
                }
            }
            case ENUM -> { /* any non-blank literal accepted — enum sets stay configurable */ }
        }
    }

    private PlanView toPlanView(PlanEntity p) {
        var values = planEntitlements.findByPlanId(p.getId()).stream()
                .collect(Collectors.toMap(PlanEntitlementEntity::getEntitlementCode,
                        PlanEntitlementEntity::getValue));
        return new PlanView(p.getId(), p.getCode(), p.getDisplayName(), p.getDescription(),
                p.getPlanType().name(), p.getPrice(), p.getCurrency(),
                p.getBillingPeriod().name(), p.getStatus().name(), p.getSortOrder(), values);
    }
}
