package com.starmitra.modules.identity.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Opaque persisted refresh token — ADR-006 rotation/family/reuse model. */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "reuse_detected", nullable = false)
    private boolean reuseDetected = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected RefreshTokenEntity() {}

    public RefreshTokenEntity(UUID userId, String tokenHash, UUID familyId, OffsetDateTime expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public UUID getFamilyId() { return familyId; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public boolean isReuseDetected() { return reuseDetected; }
    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }
    public void markRotated(UUID replacementId) {
        this.revokedAt = OffsetDateTime.now();
        this.replacedBy = replacementId;
    }
    public void markReuseDetected() { this.reuseDetected = true; }
}
