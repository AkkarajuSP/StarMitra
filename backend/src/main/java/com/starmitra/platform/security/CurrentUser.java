package com.starmitra.platform.security;

import java.util.List;
import java.util.UUID;

/** Authenticated principal — id + system roles only. Never carries domain roles. */
public record CurrentUser(UUID id, List<String> systemRoles) {

    public boolean hasRole(String role) { return systemRoles.contains(role); }
}
