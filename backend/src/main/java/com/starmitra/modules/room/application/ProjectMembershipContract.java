package com.starmitra.modules.room.application;

import java.util.UUID;

/** M07-owned membership read contract — M06 conversations / M21 seams. */
public interface ProjectMembershipContract {
    boolean isActiveMember(UUID roomId, UUID userId);
}
