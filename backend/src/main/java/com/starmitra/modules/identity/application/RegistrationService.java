package com.starmitra.modules.identity.application;

import com.starmitra.modules.identity.persistence.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * OTP-first registration — the contract exposes no /auth/register; a new
 * identity is established the first time an OTP is successfully requested.
 * Duplicate protection is the database UQ(email)/UQ(phone) — concurrent
 * first-registrations converge on the same row (idempotent).
 *
 * Default capability: USER system role only. ADMIN / SUPER_ADMIN / JUDGE can
 * never be self-granted through any M01 path.
 */
@Service
public class RegistrationService {

    private final UserRepository users;
    private final SystemRoleRepository roles;
    private final UserSystemRoleRepository userRoles;
    private final AuthEventService authEvents;

    public RegistrationService(UserRepository users, SystemRoleRepository roles,
                               UserSystemRoleRepository userRoles, AuthEventService authEvents) {
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
        this.authEvents = authEvents;
    }

    /** Find-or-create by identifier; assigns USER role when creating. */
    @Transactional
    public UserEntity findOrRegister(String channel, String identifier) {
        return findByIdentifier(channel, identifier).orElseGet(() -> register(channel, identifier));
    }

    private UserEntity register(String channel, String identifier) {
        UserEntity user = "SMS".equalsIgnoreCase(channel)
                ? new UserEntity(null, identifier)
                : new UserEntity(identifier, null);
        try {
            user = users.saveAndFlush(user);
        } catch (DataIntegrityViolationException dup) {
            // another request registered the same identifier concurrently — converge
            return findByIdentifier(channel, identifier)
                    .orElseThrow(() -> dup);
        }
        final UUID newUserId = user.getId();
        roles.findByName(SystemRoleEntity.USER).ifPresent(role ->
                userRoles.save(new UserSystemRoleEntity(newUserId, role.getId())));
        authEvents.record(newUserId, AuthEventService.USER_REGISTERED);
        return user;
    }

    private java.util.Optional<UserEntity> findByIdentifier(String channel, String identifier) {
        return "SMS".equalsIgnoreCase(channel)
                ? users.findByPhone(identifier)
                : users.findByEmail(identifier);
    }
}
