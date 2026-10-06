package com.starmitra.modules.pricing.application;

import java.util.Optional;
import java.util.UUID;

/**
 * M22 Entitlement Contract — the ONLY seam business modules use for
 * plan/limit questions. Callers never inspect plans, prices, or payment
 * state; they ask for an entitlement by code.
 *
 *   Business Module → EntitlementContract → M22 Pricing & Entitlements
 *
 * Value semantics (driven by entitlements.value_kind):
 *   BOOLEAN — granted iff configured value is 'true'
 *   INTEGER — a limit; negative = unlimited; usage comes from
 *             entitlement_usage (reported via recordUsage/releaseUsage)
 *   ENUM    — a labelled tier (e.g. analytics.tier = ENHANCED)
 *
 * No payment-provider concepts are exposed here.
 */
public interface EntitlementContract {

    /** Resolved entitlement for the user's current plan, if granted. */
    record EntitlementValue(String code, String kind, Long limit, String value) {}

    /** True when the user's current plan grants this entitlement. */
    boolean hasEntitlement(UUID userId, String entitlementCode);

    /** Resolved value for the user's current plan — empty when not granted. */
    Optional<EntitlementValue> getEntitlement(UUID userId, String entitlementCode);

    /**
     * INTEGER entitlements: true when requestedAmount fits within the
     * remaining limit (limit − reported usage). Non-INTEGER entitlements
     * are not limits — this returns their grant state and ignores the amount.
     */
    boolean checkLimit(UUID userId, String entitlementCode, long requestedAmount);

    /**
     * Remaining capacity for INTEGER entitlements: max(0, limit − used);
     * -1 when the limit is unlimited. Returns 0 when not granted, and -1 for
     * non-INTEGER entitlements that are granted (they have no numeric limit).
     */
    long getRemainingLimit(UUID userId, String entitlementCode);

    /** Report consumption. Owning modules call this; M22 never counts rows. */
    void recordUsage(UUID userId, String entitlementCode, long delta);

    /** Release previously recorded consumption (e.g. item deleted). */
    void releaseUsage(UUID userId, String entitlementCode, long delta);
}
