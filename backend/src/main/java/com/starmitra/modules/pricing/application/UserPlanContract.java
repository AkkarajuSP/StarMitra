package com.starmitra.modules.pricing.application;

import java.util.UUID;

/**
 * M22 user-plan seam used by M01 registration: every new identity receives
 * the default FREE plan — no payment involved. Entitlement resolution also
 * lazily back-fills the default for users who predate M22.
 */
public interface UserPlanContract {

    /** Idempotent — converges if the user already has an ACTIVE subscription. */
    void assignDefaultPlan(UUID userId);
}
