package com.starmitra.modules.moderation.application;

import java.util.List;
import java.util.UUID;

/** M18-owned moderation truth — other modules enforce, never decide. */
public interface ModerationContract {

    /** ACTIVE restriction types on a target (empty = unrestricted). */
    List<String> activeRestrictions(String targetType, UUID targetId);

    /** Convenience: any ACTIVE restriction at all? */
    default boolean isRestricted(String targetType, UUID targetId) {
        return !activeRestrictions(targetType, targetId).isEmpty();
    }
}
