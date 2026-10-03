package com.starmitra.modules.identity.application;

import com.starmitra.modules.identity.persistence.RefreshTokenEntity;
import com.starmitra.modules.identity.persistence.RefreshTokenRepository;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionServiceTest {

    private RefreshTokenRepository tokens;
    private SessionService service;

    @BeforeEach
    void setUp() {
        tokens = mock(RefreshTokenRepository.class);
        service = new SessionService(tokens, mock(AuthEventService.class));
    }

    private RefreshTokenEntity active(UUID userId, UUID family) {
        return new RefreshTokenEntity(userId, "h" + UUID.randomUUID().toString().substring(0, 8),
                family, OffsetDateTime.now().plusDays(10));
    }

    @Test
    void listsOnlyOwnActiveSessions() {
        UUID me = UUID.randomUUID();
        UUID myFamily = UUID.randomUUID();
        var s1 = active(me, myFamily);
        when(tokens.findActiveByUserId(me)).thenReturn(List.of(s1));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(s1));

        var sessions = service.listForUser(me, "raw");
        assertEquals(1, sessions.size());
        assertTrue(sessions.get(0).current());
    }

    @Test
    void presentedTokenFromOtherFamilyMarksCurrentFalse() {
        UUID me = UUID.randomUUID();
        var mine = active(me, UUID.randomUUID());
        var other = active(me, UUID.randomUUID());
        when(tokens.findActiveByUserId(me)).thenReturn(List.of(mine, other));
        when(tokens.findByTokenHash(RefreshTokenService.hash("raw"))).thenReturn(Optional.of(mine));

        var sessions = service.listForUser(me, "raw");
        assertEquals(1, sessions.stream().filter(SessionService.SessionView::current).count());
    }

    @Test
    void revokeSessionIsIdorSafe() {
        UUID me = UUID.randomUUID();
        var foreign = active(UUID.randomUUID(), UUID.randomUUID());
        when(tokens.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        var e = assertThrows(ApiException.class, () -> service.revokeSession(me, foreign.getId()));
        assertEquals(ErrorCode.NOT_FOUND, e.code());
        verify(tokens, never()).save(any());
    }

    @Test
    void revokeOwnSessionSucceeds() {
        UUID me = UUID.randomUUID();
        var mine = active(me, UUID.randomUUID());
        when(tokens.findById(mine.getId())).thenReturn(Optional.of(mine));

        service.revokeSession(me, mine.getId());
        assertNotNull(mine.getRevokedAt());
        verify(tokens).save(mine);
    }
}
