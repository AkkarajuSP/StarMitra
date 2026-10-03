package com.starmitra.modules.identity.application;

import com.starmitra.modules.identity.persistence.RefreshTokenEntity;
import com.starmitra.modules.identity.persistence.RefreshTokenRepository;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private RefreshTokenRepository tokens;
    private AuditService audit;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        tokens = mock(RefreshTokenRepository.class);
        audit = mock(AuditService.class);
        service = new RefreshTokenService(tokens, audit, mock(AuthEventService.class), Duration.ofDays(30));
    }

    @Test
    void issuePersistsHashNeverRawToken() {
        var issued = service.issue(UUID.randomUUID(), UUID.randomUUID());
        verify(tokens).save(argThat(t -> t.getTokenHash().length() == 64
                && !t.getTokenHash().equals(issued.raw())));
        assertTrue(issued.raw().length() > 40);
    }

    @Test
    void rotateReplacesActiveToken() {
        var existing = new RefreshTokenEntity(UUID.randomUUID(), RefreshTokenService.hash("raw-1"),
                UUID.randomUUID(), OffsetDateTime.now().plusDays(1));
        when(tokens.findByTokenHash(RefreshTokenService.hash("raw-1"))).thenReturn(Optional.of(existing));

        var next = service.rotate("raw-1");
        assertNotNull(next);
        assertNotNull(existing.getRevokedAt());          // rotated out
    }

    @Test
    void reusedRevokedTokenTriggersFamilyRevocation() {
        var revoked = new RefreshTokenEntity(UUID.randomUUID(), RefreshTokenService.hash("stale"),
                UUID.randomUUID(), OffsetDateTime.now().plusDays(1));
        revoked.markRotated(UUID.randomUUID());           // already rotated → revoked_at set
        when(tokens.findByTokenHash(RefreshTokenService.hash("stale"))).thenReturn(Optional.of(revoked));

        assertThrows(ApiException.class, () -> service.rotate("stale"));
        verify(tokens).revokeFamily(revoked.getFamilyId());
        assertTrue(revoked.isReuseDetected());
    }

    @Test
    void expiredTokenRejected() {
        var expired = new RefreshTokenEntity(UUID.randomUUID(), RefreshTokenService.hash("old"),
                UUID.randomUUID(), OffsetDateTime.now().minusDays(1));
        when(tokens.findByTokenHash(RefreshTokenService.hash("old"))).thenReturn(Optional.of(expired));
        assertThrows(ApiException.class, () -> service.rotate("old"));
    }
}
