package com.starmitra.modules.portfolio.application;

import java.util.UUID;

/**
 * M08-owned engagement-target contract — lets M21 validate PORTFOLIO
 * likes/comments without touching portfolio persistence.
 * Target = portfolio item (the engageable content unit).
 */
public interface PortfolioTargetContract {

    /** Caller may engage: item exists, ACTIVE, and PUBLIC — or the caller owns it. */
    boolean isEngageableItem(UUID itemId, UUID callerUserId);

    /** Item's portfolio owner — for recipient derivation (e.g., M21 LIKE notification). */
    java.util.Optional<UUID> ownerOf(UUID itemId);
}
