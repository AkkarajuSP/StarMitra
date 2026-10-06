package com.starmitra.modules.identity.application;

import com.starmitra.modules.identity.persistence.*;
import com.starmitra.modules.pricing.application.UserPlanContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {

    private UserRepository users;
    private SystemRoleRepository roles;
    private UserSystemRoleRepository userRoles;
    private UserPlanContract userPlans;
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        roles = mock(SystemRoleRepository.class);
        userRoles = mock(UserSystemRoleRepository.class);
        userPlans = mock(UserPlanContract.class);
        service = new RegistrationService(users, roles, userRoles, mock(AuthEventService.class),
                userPlans);
    }

    @Test
    void registersNewUserWithUserRoleOnly() {
        when(users.findByEmail("new@x.com")).thenReturn(Optional.empty());
        when(users.saveAndFlush(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));
        var role = mock(SystemRoleEntity.class);
        when(role.getId()).thenReturn(UUID.randomUUID());
        when(roles.findByName("USER")).thenReturn(Optional.of(role));

        var user = service.findOrRegister("EMAIL", "new@x.com");
        assertNotNull(user);
        verify(userRoles).save(any(UserSystemRoleEntity.class));
        verify(roles).findByName("USER");                      // never JUDGE/ADMIN
        verify(roles, never()).findByName("ADMIN");
        verify(roles, never()).findByName("JUDGE");
        verify(userPlans).assignDefaultPlan(any());      // M22 default FREE seam
    }

    @Test
    void existingUserIsReturnedNotDuplicated() {
        var existing = mock(UserEntity.class);
        when(users.findByEmail("me@x.com")).thenReturn(Optional.of(existing));
        assertSame(existing, service.findOrRegister("EMAIL", "me@x.com"));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void smsChannelRegistersByPhone() {
        when(users.findByPhone("+155501")).thenReturn(Optional.empty());
        when(users.saveAndFlush(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));
        var role = mock(SystemRoleEntity.class);
        when(role.getId()).thenReturn(UUID.randomUUID());
        when(roles.findByName("USER")).thenReturn(Optional.of(role));

        var user = service.findOrRegister("SMS", "+155501");
        verify(users).saveAndFlush(argThat(u -> "+155501".equals(u.getPhone()) && u.getEmail() == null));
    }
}
