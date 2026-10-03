package com.starmitra.modules.identity.application;

import com.starmitra.modules.identity.persistence.RefreshTokenEntity;
import com.starmitra.modules.identity.persistence.RefreshTokenRepository;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Session model = persisted refresh-token rows (one row per rotated token;
 * an active, unrevoked, unexpired row is a live session). Never exposes token
 * material — only ids/timestamps.
 */
@Service
public class SessionService {

    private final RefreshTokenRepository tokens;
    private final AuthEventService authEvents;

    public SessionService(RefreshTokenRepository tokens, AuthEventService authEvents) {
        this.tokens = tokens;
        this.authEvents = authEvents;
    }

    public record SessionView(UUID id, String createdAt, boolean current) {}

    /** Own active sessions; `current` = the session whose family matches the presented refresh token. */
    @Transactional(readOnly = true)
    public List<SessionView> listForUser(UUID userId, String presentedRefreshToken) {
        UUID currentFamily = presentedRefreshToken == null ? null
                : tokens.findByTokenHash(RefreshTokenService.hash(presentedRefreshToken))
                        .map(RefreshTokenEntity::getFamilyId).orElse(null);
        return tokens.findActiveByUserId(userId).stream()
                .map(t -> new SessionView(t.getId(), t.getCreatedAt().toString(),
                        t.getFamilyId().equals(currentFamily)))
                .toList();
    }

    /** Revoke one own session; IDOR-safe (foreign id → NOT_FOUND). */
    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        RefreshTokenEntity token = tokens.findById(sessionId)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (token.isActive()) {
            token.markRotated(null);
            tokens.save(token);
            authEvents.record(userId, AuthEventService.SESSION_REVOKED,
                    "{\"sessionId\":\"" + sessionId + "\"}");
        }
    }
}
