package com.starmitra.modules.room.application;

import java.util.UUID;

/**
 * M07-owned authoritative credit contract — M08 portfolio links ONLY credits
 * that pass this check. M08 never manufactures contribution.
 */
public interface ProjectCreditContract {

    /** Credit exists, is verified, and belongs to the linking user. */
    boolean isLinkableCredit(UUID projectCreditId, UUID userId);
}
