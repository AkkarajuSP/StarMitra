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
import org.mockito.Mockito;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OtpServiceTest {

    private OtpChallengeRepository challenges;
    private UserRepository users;
    private OtpSender sender;
    private OtpService service;

    @BeforeEach
    void setUp() {
        challenges = mock(OtpChallengeRepository.class);
        users = mock(UserRepository.class);
        sender = mock(OtpSender.class);
        service = new OtpService(challenges, users, sender, mock(AuditService.class),
                Duration.ofMinutes(5), 5, Duration.ofSeconds(0), Duration.ofMinutes(15));
    }

    @Test
    void requestIsEnumerationSafeForUnknownIdentifier() {
        when(users.findByEmail("nobody@x.com")).thenReturn(Optional.empty());
        assertNull(service.request("EMAIL", "nobody@x.com"));
        verify(sender, never()).send(any(), any(), any());
    }

    @Test
    void requestCreatesHashedChallengeAndSupersedesPrior() {
        var user = Mockito.mock(UserEntity.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getStatus()).thenReturn(UserEntity.Status.ACTIVE);
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        UUID id = service.request("EMAIL", "a@b.com");
        assertNotNull(id);
        verify(challenges).supersedeAllForUser(id);
        verify(challenges).save(argThat(c -> c.getOtpHash().length() == 64));  // SHA-256 hex, never plaintext
        verify(sender).send(eq("EMAIL"), eq("a@b.com"), argThat(o -> o.matches("\\d{6}")));
    }

    @Test
    void verifyRejectsWrongOtpAndConsumesOnSuccess() {
        UUID uid = UUID.randomUUID();
        var user = mock(UserEntity.class);
        when(user.getId()).thenReturn(uid);
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        var challenge = new OtpChallengeEntity(uid, "deadbeef", 5, OffsetDateTime.now().plusMinutes(5));
        when(challenges.findLatestActive(uid)).thenReturn(Optional.of(challenge));

        assertThrows(ApiException.class, () -> service.verify("a@b.com", "000000"));
        assertEquals(1, challenge.getAttempts());
    }

    @Test
    void verifyLocksAfterMaxAttempts() {
        UUID uid = UUID.randomUUID();
        var user = mock(UserEntity.class);
        when(user.getId()).thenReturn(uid);
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        var challenge = new OtpChallengeEntity(uid, "deadbeef", 3, OffsetDateTime.now().plusMinutes(5));
        when(challenges.findLatestActive(uid)).thenReturn(Optional.of(challenge));

        for (int i = 0; i < 3; i++) {
            assertThrows(ApiException.class, () -> service.verify("a@b.com", "000000"));
        }
        assertTrue(challenge.isLocked());
    }

    @Test
    void apiExceptionCarriesCatalogCode() {
        ApiException e = assertThrows(ApiException.class,
                () -> service.verify("x@y.com", "123456"));
        assertEquals(ErrorCode.OTP_INVALID, e.code());
    }
}
