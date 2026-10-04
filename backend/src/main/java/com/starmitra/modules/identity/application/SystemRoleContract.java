package com.starmitra.modules.identity.application;

import java.util.UUID;

/** M01-owned system-role contract — role grants are identity-truth, not domain data. */
public interface SystemRoleContract {
    /** Grant a system role (e.g. JUDGE) to a user. Idempotent. */
    void grantSystemRole(UUID userId, String roleName);
}
