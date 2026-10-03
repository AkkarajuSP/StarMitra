package com.starmitra.platform.security;

import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    @SuppressWarnings("unchecked")
    public static CurrentUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED);
        }
        Object roles = jwt.getClaims().get("roles");
        List<String> roleNames = roles instanceof List<?> l ? l.stream().map(Object::toString).toList() : List.of();
        return new CurrentUser(UUID.fromString(jwt.getSubject()), roleNames);
    }

    public static UUID currentUserId() {
        return currentUser().id();
    }

    public static void requireRole(String role) {
        if (!currentUser().hasRole(role)) {
            throw new ApiException(ErrorCode.ROLE_REQUIRED);
        }
    }
}
