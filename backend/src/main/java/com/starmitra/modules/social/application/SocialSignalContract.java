package com.starmitra.modules.social.application;

import java.util.Set;
import java.util.UUID;

/**
 * M21-owned read contract — feeds M05 ranking signals and later
 * M02 FOLLOWERS visibility. Social signals are derived data, never
 * authoritative for business outcomes.
 */
public interface SocialSignalContract {

    /** IDs of users this follower follows (feed personalization signal). */
    Set<UUID> followeeIdsOf(UUID followerId);

    /** Whether follower follows followee. */
    boolean isFollowing(UUID followerId, UUID followeeId);
}
