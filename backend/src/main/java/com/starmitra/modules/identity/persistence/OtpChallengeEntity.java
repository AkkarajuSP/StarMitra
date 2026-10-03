package com.starmitra.modules.identity.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** OTP challenge — hashes only, never plaintext. */
@Entity
@Table(name = "otp_challenges")
public class OtpChallengeEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "otp_hash", nullable = false, length = 128)
    private String otpHash;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected OtpChallengeEntity() {}

    public OtpChallengeEntity(UUID userId, String otpHash, int maxAttempts, OffsetDateTime expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.otpHash = otpHash;
        this.maxAttempts = maxAttempts;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getOtpHash() { return otpHash; }
    public int getAttempts() { return attempts; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getConsumedAt() { return consumedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public boolean isExpired() { return OffsetDateTime.now().isAfter(expiresAt); }
    public boolean isConsumed() { return consumedAt != null; }
    public boolean isLocked() { return attempts >= maxAttempts; }
    public boolean isUsable() { return !isExpired() && !isConsumed() && !isLocked(); }

    public void recordFailedAttempt() { this.attempts++; }
    public void consume() { this.consumedAt = OffsetDateTime.now(); }
    /** Mark superseded when a newer challenge replaces this one. */
    public void supersede() { this.consumedAt = OffsetDateTime.now(); }
}
