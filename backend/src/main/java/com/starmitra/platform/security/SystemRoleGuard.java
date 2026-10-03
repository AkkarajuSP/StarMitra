package com.starmitra.platform.security;

import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Centralized role checks — the single authority for SystemRole evaluation.
 * Domain roles (TalentSkill, ProjectContributionRole, JudgeExpertise) are
 * explicitly NOT consulted here.
 */
public final class SystemRoleGuard {

    private SystemRoleGuard() {}

    public static boolean hasRole(UUID userId, String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

    public static void requireRole(String role) {
        if (!hasRole(null, role)) throw new ApiException(ErrorCode.ROLE_REQUIRED);
    }

    public static void requireJudge(UUID userId) {
        requireRole("JUDGE");
    }

    public static void requireAdmin() {
        if (!hasRole(null, "ADMIN") && !hasRole(null, "SUPER_ADMIN")) {
            throw new ApiException(ErrorCode.ROLE_REQUIRED);
        }
    }
}
