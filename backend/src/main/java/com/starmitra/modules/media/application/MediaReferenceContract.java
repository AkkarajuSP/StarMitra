package com.starmitra.modules.media.application;

import java.util.UUID;

/**
 * M04-owned cross-module contract — lets business modules (M02 avatar/banner,
 * and later M08/M10/M06/M07) validate a media reference without touching
 * media persistence. M04 stays authoritative for the asset.
 */
public interface MediaReferenceContract {

    /** True when the asset exists, belongs to owner, and is usable (VERIFIED + not rejected). */
    boolean isUsableBy(UUID mediaId, UUID ownerUserId);

    /** True when the caller may view/engage the asset (owner, or PUBLIC+processed+unrestricted). */
    boolean isDeliverableTo(UUID mediaId, UUID callerUserId);

    /** Asset owner — for recipient derivation (e.g., M21 LIKE notification). */
    java.util.Optional<UUID> ownerOf(UUID mediaId);
}
