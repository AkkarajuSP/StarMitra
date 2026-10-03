package com.starmitra.modules.moderation.application;

import java.util.UUID;

/**
 * M18-owned contract — reports whether a target profile/user is currently
 * restricted by a moderation decision. M02 consumes this to filter public
 * visibility; it never owns or infers restriction itself.
 */
public interface ProfileRestrictionContract {

    boolean isRestricted(UUID userId);
}
