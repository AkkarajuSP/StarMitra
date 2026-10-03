package com.starmitra.modules.identity.application;

import com.starmitra.modules.identity.persistence.OtpChallengeEntity;
import com.starmitra.modules.identity.persistence.OtpChallengeRepository;
import com.starmitra.modules.identity.persistence.UserEntity;
import com.starmitra.modules.identity.persistence.UserRepository;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OtpServiceTest {

    private OtpChallengeRepository challenges;
    private UserRepository users;
    private RegistrationService registration;
    private OtpSender sender;
    private AuthEventService authEvents;
    private OtpService service;

    @BeforeEach
    void setUp() {
        challenges = mock(OtpChallengeRepository.class);
        users = mock(UserRepository.class);
        registration = mock(RegistrationService.class);
        sender = mock(OtpSender.class);
        authEvents = mock(AuthEventService.class);
        service = new OtpService(challenges, users, registration, sender,
                mock(AuditService.class), authEvents,
                Duration.ofMinutes(5), 5, Duration.ofSeconds(0), Duration.ofMinutes(15));
    }

    private UserEntity activeUser(UUID id) {
        var u = mock(UserEntity.class);
        lenient().when(u.getId()).thenReturn(id);
        lenient().when(u.getStatus()).thenReturn(UserEntity.Status.ACTIVE);
        return u;
    }

    @Test
    void requestRegistersUnknownIdentifierAndIssuesChallenge() {
        var user = activeUser(UUID.randomUUID());
        when(registration.findOrRegister("EMAIL", "new@x.com")).thenReturn(user);

        UUID id = service.request("EMAIL", "new@x.com");
        assertNotNull(id);
        verify(challenges).supersedeAllForUser(id);
        verify(challenges).save(argThat(c -> c.getOtpHash().length() == 64));  // SHA-256, never plaintext
        verify(sender).send(eq("EMAIL"), eq("new@x.com"), argThat(o -> o.matches("\\d{6}")));
        verify(authEvents).record(id, AuthEventService.OTP_REQUESTED);
    }

    @Test
    void requestRefusesSuspendedIdentitySilently() {
        var user = mock(UserEntity.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getStatus()).thenReturn(UserEntity.Status.SUSPENDED);
        when(registration.findOrRegister(any(), any())).thenReturn(user);

        assertNull(service.request("EMAIL", "sus@x.com"));
        verify(sender, never()).send(any(), any(), any());
    }

    @Test
    void requestThrottlesAtLockout() {
        var user = activeUser(UUID.randomUUID());
        when(registration.findOrRegister(any(), any())).thenReturn(user);
        when(challenges.countRecentRequests(eq(user.getId()), any())).thenReturn(5L);

        assertNull(service.request("EMAIL", "a@b.com"));
        verify(sender, never()).send(any(), any(), any());
        verify(authEvents).record(user.getId(), AuthEventService.OTP_REQUEST_LOCKED);
    }

    @Test
    void requestSupersedesPreviousChallenge() {
        var user = activeUser(UUID.randomUUID());
        when(registration.findOrRegister(any(), any())).thenReturn(user);
        var stale = new OtpChallengeEntity(user.getId(), "x", 5, OffsetDateTime.now().plusMinutes(5));
        when(challenges.findTop1ByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(stale));

        service.request("EMAIL", "a@b.com");
        verify(challenges).supersedeAllForUser(user.getId());
        verify(challenges).save(any(OtpChallengeEntity.class));
    }

    @Test
    void verifyRejectsWrongOtpAndIncrementsAttempts() {
        UUID uid = UUID.randomUUID();
        var user = activeUser(uid);
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        var challenge = new OtpChallengeEntity(uid, "deadbeef", 5, OffsetDateTime.now().plusMinutes(5));
        when(challenges.findTop1ByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(uid))
                .thenReturn(Optional.of(challenge));

        assertThrows(ApiException.class, () -> service.verify("a@b.com", "000000"));
        assertEquals(1, challenge.getAttempts());
    }

    @Test
    void verifyLocksAfterMaxAttempts() {
        UUID uid = UUID.randomUUID();
        var user = activeUser(uid);
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        var challenge = new OtpChallengeEntity(uid, "deadbeef", 3, OffsetDateTime.now().plusMinutes(5));
        when(challenges.findTop1ByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(uid))
                .thenReturn(Optional.of(challenge));

        for (int i = 0; i < 3; i++) {
            assertThrows(ApiException.class, () -> service.verify("a@b.com", "000000"));
        }
        assertTrue(challenge.isLocked());
    }

    @Test
    void verifyRejectsExpiredChallenge() {
        UUID uid = UUID.randomUUID();
        var user = activeUser(uid);
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        var expired = new OtpChallengeEntity(uid, "x", 5, OffsetDateTime.now().minusMinutes(1));
        when(challenges.findTop1ByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(uid))
                .thenReturn(Optional.of(expired));

        var e = assertThrows(ApiException.class, () -> service.verify("a@b.com", "123456"));
        assertEquals(ErrorCode.OTP_INVALID, e.code());
    }

    @Test
    void verifyUnknownIdentifierIsUniformInvalid() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        when(users.findByPhone(any())).thenReturn(Optional.empty());
        var e = assertThrows(ApiException.class, () -> service.verify("ghost@x.com", "123456"));
        assertEquals(ErrorCode.OTP_INVALID, e.code());
    }

    @Test
    void apiExceptionCarriesCatalogCode() {
        ApiException e = assertThrows(ApiException.class,
                () -> service.verify("x@y.com", "123456"));
        assertEquals(ErrorCode.OTP_INVALID, e.code());
    }
}
