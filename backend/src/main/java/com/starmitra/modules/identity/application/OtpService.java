package com.starmitra.modules.identity.application;

import com.starmitra.modules.identity.persistence.OtpChallengeEntity;
import com.starmitra.modules.identity.persistence.OtpChallengeRepository;
import com.starmitra.modules.identity.persistence.UserEntity;
import com.starmitra.modules.identity.persistence.UserRepository;
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
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * OTP lifecycle: REQUESTED → VERIFIED/CONSUMED (supersede on resend).
 * Enumeration-safe: callers always receive the same acceptance signal whether
 * or not the identifier exists. Raw OTP is never stored or logged.
 */
@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpChallengeRepository challenges;
    private final UserRepository users;
    private final OtpSender sender;
    private final AuditService audit;
    private final Duration ttl;
    private final int maxAttempts;
    private final Duration resendCooldown;
    private final Duration lockoutWindow;

    public OtpService(OtpChallengeRepository challenges, UserRepository users,
                      OtpSender sender, AuditService audit,
                      @Value("${app.otp.ttl}") Duration ttl,
                      @Value("${app.otp.max-attempts}") int maxAttempts,
                      @Value("${app.otp.resend-cooldown}") Duration resendCooldown,
                      @Value("${app.otp.lockout-window}") Duration lockoutWindow) {
        this.challenges = challenges;
        this.users = users;
        this.sender = sender;
        this.audit = audit;
        this.ttl = ttl;
        this.maxAttempts = maxAttempts;
        this.resendCooldown = resendCooldown;
        this.lockoutWindow = lockoutWindow;
    }

    /**
     * Request an OTP. Always "accepted" externally — internally no-ops for
     * unknown/locked identifiers (enumeration-safe per API-ERROR-CATALOG).
     * Returns userId when a challenge was created (null otherwise).
     */
    @Transactional
    public UUID request(String channel, String identifier) {
        Optional<UserEntity> user = findUser(channel, identifier);
        if (user.isEmpty() || user.get().getStatus() != UserEntity.Status.ACTIVE) {
            audit.record("M01", "OTP_REQUEST_UNKNOWN", null, "anon", "identifier", "redacted", "unknown identifier");
            return null;
        }
        UUID userId = user.get().getId();

        // throttle: too many recent requests → lockout window
        if (challenges.countRecentRequests(userId, OffsetDateTime.now().minus(lockoutWindow)) >= maxAttempts) {
            audit.record("M01", "OTP_REQUEST_LOCKED", userId, "user", "user", userId.toString(), "resend lockout");
            return null;
        }
        // resend cooldown from latest challenge
        Optional<OtpChallengeEntity> latest = challenges.findLatestActive(userId);
        if (latest.isPresent() && latest.get().getCreatedAt().plus(resendCooldown).isAfter(OffsetDateTime.now())) {
            return userId;      // accepted but no new challenge yet
        }

        challenges.supersedeAllForUser(userId);          // older challenges → SUPERSEDED

        String otp = generate();
        challenges.save(new OtpChallengeEntity(userId, hash(otp), maxAttempts,
                OffsetDateTime.now().plus(ttl)));
        sender.send(channel, identifier, otp);
        audit.record("M01", "OTP_REQUESTED", userId, "user", "otp_challenge", null, null);
        return userId;
    }

    /** Verify OTP; consumes the challenge on success. Channel inferred from identifier. */
    @Transactional
    public UUID verify(String identifier, String otp) {
        UserEntity user = findUserByIdentifier(identifier)
                .orElseThrow(() -> new ApiException(ErrorCode.OTP_INVALID));

        OtpChallengeEntity challenge = challenges.findLatestActive(user.getId())
                .filter(OtpChallengeEntity::isUsable)
                .orElseThrow(() -> {
                    audit.record("M01", "OTP_VERIFY_NO_ACTIVE", user.getId(), "user", null, null, null);
                    return new ApiException(ErrorCode.OTP_INVALID);
                });

        if (challenge.isLocked()) {
            audit.record("M01", "OTP_VERIFY_LOCKED", user.getId(), "user", null, null, null);
            throw new ApiException(ErrorCode.OTP_LOCKED);
        }

        if (!MessageDigest.isEqual(challenge.getOtpHash().getBytes(StandardCharsets.UTF_8),
                hash(otp).getBytes(StandardCharsets.UTF_8))) {
            challenge.recordFailedAttempt();
            challenges.save(challenge);
            if (challenge.isLocked()) {
                audit.record("M01", "OTP_LOCKED", user.getId(), "user", null, null, "max attempts");
                throw new ApiException(ErrorCode.OTP_LOCKED);
            }
            audit.record("M01", "OTP_VERIFY_FAILED", user.getId(), "user", null, null, null);
            throw new ApiException(ErrorCode.OTP_INVALID);
        }

        challenge.consume();
        challenges.save(challenge);
        audit.record("M01", "OTP_VERIFIED", user.getId(), "user", null, null, null);
        return user.getId();
    }

    private Optional<UserEntity> findUser(String channel, String identifier) {
        return "SMS".equalsIgnoreCase(channel)
                ? users.findByPhone(identifier)
                : users.findByEmail(identifier);
    }

    /** Contract has no channel on verify — resolve by email first, then phone. */
    private Optional<UserEntity> findUserByIdentifier(String identifier) {
        return users.findByEmail(identifier).or(() -> users.findByPhone(identifier));
    }

    private static String generate() {
        return "%06d".formatted(RANDOM.nextInt(1_000_000));
    }

    private static String hash(String otp) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(otp.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
