package com.starmitra.modules.room.application;

import java.util.Optional;
import java.util.UUID;

/**
 * M07-owned contributor resolution — M10 submission contributors reference
 * M07's member + contextual role. Returns display data for DB-02 snapshots;
 * never manufactures roles.
 */
public interface ProjectContributionContract {

    record ResolvedContribution(UUID memberUserId, String memberDisplay, UUID roleId, String roleName) {}

    /**
     * memberUserId must be an ACTIVE member of roomId; roleRef (if present)
     * must be an active contribution role of that room assigned to that member.
     */
    Optional<ResolvedContribution> resolve(UUID roomId, UUID memberUserId, UUID roleRef);
}
