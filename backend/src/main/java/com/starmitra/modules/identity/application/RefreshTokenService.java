package com.starmitra.modules.identity.application;

import com.starmitra.modules.identity.persistence.RefreshTokenEntity;
import com.starmitra.modules.identity.persistence.RefreshTokenRepository;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Opaque refresh-token lifecycle: issue → rotate → revoke → reuse-detect.
 * Raw tokens are only ever returned to the client; DB stores SHA-256 hashes.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository tokens;
    private final AuditService audit;
    private final AuthEventService authEvents;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenRepository tokens, AuditService audit,
                               AuthEventService authEvents,
                               @Value("${app.refresh-token.ttl}") Duration ttl) {
        this.tokens = tokens;
        this.audit = audit;
        this.authEvents = authEvents;
        this.ttl = ttl;
    }

    public record IssuedToken(UUID id, UUID userId, String raw) {}

    @Transactional
    public IssuedToken issue(UUID userId, UUID familyId) {
        String raw = generateOpaque();
        RefreshTokenEntity t = new RefreshTokenEntity(userId, hash(raw), familyId,
                OffsetDateTime.now().plus(ttl));
        tokens.save(t);
        return new IssuedToken(t.getId(), userId, raw);
    }

    /** noRollbackFor: reuse detection + family revocation must persist even as the request is rejected. */
    @Transactional(noRollbackFor = ApiException.class)
    public IssuedToken rotate(String presentedRaw) {
        RefreshTokenEntity presented = tokens.findByTokenHash(hash(presentedRaw))
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_REVOKED, "Unknown refresh token"));

        if (!presented.isActive()) {
            // reuse of a revoked/expired token → compromise signal: kill the family
            if (presented.getRevokedAt() != null) {
                presented.markReuseDetected();
                tokens.revokeFamily(presented.getFamilyId());
                authEvents.record(presented.getUserId(), AuthEventService.REFRESH_REUSE_DETECTED,
                        "{\"familyId\":\"" + presented.getFamilyId() + "\"}");
                audit.record("M01", "REFRESH_REUSE_DETECTED", presented.getUserId(), "system",
                        "refresh_token", presented.getId().toString(), "revoked token re-presented");
            }
            throw new ApiException(ErrorCode.SESSION_REVOKED, "Refresh token is no longer valid");
        }

        IssuedToken next = issue(presented.getUserId(), presented.getFamilyId());
        presented.markRotated(next.id());
        authEvents.record(presented.getUserId(), AuthEventService.REFRESH_ROTATED);
        return next;
    }

    @Transactional
    public void revoke(String presentedRaw, UUID actorId) {
        tokens.findByTokenHash(hash(presentedRaw)).ifPresent(t -> {
            if (t.isActive()) {
                t.markRotated(null);
                authEvents.record(t.getUserId(), AuthEventService.SESSION_REVOKED, "{\"reason\":\"logout\"}");
                audit.record("M01", "SESSION_REVOKED", actorId, "user",
                        "refresh_token", t.getId().toString(), "logout");
            }
        });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        tokens.revokeAllForUser(userId);
    }

    public static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String generateOpaque() {
        byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
