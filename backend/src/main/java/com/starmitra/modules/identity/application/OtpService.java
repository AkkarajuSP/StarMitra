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
    private final RegistrationService registration;
    private final OtpSender sender;
    private final AuditService audit;
    private final AuthEventService authEvents;
    private final Duration ttl;
    private final int maxAttempts;
    private final Duration resendCooldown;
    private final Duration lockoutWindow;

    public OtpService(OtpChallengeRepository challenges, UserRepository users,
                      RegistrationService registration,
                      OtpSender sender, AuditService audit, AuthEventService authEvents,
                      @Value("${app.otp.ttl}") Duration ttl,
                      @Value("${app.otp.max-attempts}") int maxAttempts,
                      @Value("${app.otp.resend-cooldown}") Duration resendCooldown,
                      @Value("${app.otp.lockout-window}") Duration lockoutWindow) {
        this.challenges = challenges;
        this.users = users;
        this.registration = registration;
        this.sender = sender;
        this.audit = audit;
        this.authEvents = authEvents;
        this.ttl = ttl;
        this.maxAttempts = maxAttempts;
        this.resendCooldown = resendCooldown;
        this.lockoutWindow = lockoutWindow;
    }

    /**
     * Request an OTP. Always "accepted" externally — OTP-first registration:
     * an unknown identifier registers a User (USER role) then issues a
     * challenge; a suspended/blocked identity gets no challenge
     * (enumeration-safe per API-ERROR-CATALOG). Returns userId, null if refused.
     */
    @Transactional
    public UUID request(String channel, String identifier) {
        identifier = normalize(identifier);
        UserEntity user = registration.findOrRegister(channel, identifier);
        if (user.getStatus() != UserEntity.Status.ACTIVE) {
            authEvents.record(user.getId(), AuthEventService.OTP_REQUEST_UNKNOWN);
            return null;
        }
        UUID userId = user.getId();

        // throttle: too many recent requests → lockout window
        if (challenges.countRecentRequests(userId, OffsetDateTime.now().minus(lockoutWindow)) >= maxAttempts) {
            authEvents.record(userId, AuthEventService.OTP_REQUEST_LOCKED);
            return null;
        }
        // resend cooldown from latest challenge
        Optional<OtpChallengeEntity> latest = challenges.findTop1ByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(userId);
        if (latest.isPresent() && latest.get().getCreatedAt().plus(resendCooldown).isAfter(OffsetDateTime.now())) {
            authEvents.record(userId, AuthEventService.OTP_RESEND_COOLDOWN);
            return userId;      // accepted but no new challenge yet
        }

        challenges.supersedeAllForUser(userId);          // older challenges → SUPERSEDED

        String otp = generate();
        challenges.save(new OtpChallengeEntity(userId, hash(otp), maxAttempts,
                OffsetDateTime.now().plus(ttl)));
        sender.send(channel, identifier, otp);
        authEvents.record(userId, AuthEventService.OTP_REQUESTED);
        return userId;
    }

    /**
     * Verify OTP; consumes the challenge on success. Channel inferred from
     * identifier. dontRollbackOn: failed-attempt/lockout state must persist
     * even though the ApiException propagates.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public UUID verify(String identifier, String otp) {
        UserEntity user = findUserByIdentifier(normalize(identifier))
                .orElseThrow(() -> new ApiException(ErrorCode.OTP_INVALID));

        OtpChallengeEntity challenge = challenges.findTop1ByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId())
                .filter(OtpChallengeEntity::isUsable)
                .orElseThrow(() -> {
                    authEvents.record(user.getId(), AuthEventService.OTP_VERIFY_FAILED);
                    return new ApiException(ErrorCode.OTP_INVALID);
                });

        if (challenge.isLocked()) {
            authEvents.record(user.getId(), AuthEventService.OTP_LOCKED);
            throw new ApiException(ErrorCode.OTP_LOCKED);
        }

        if (!MessageDigest.isEqual(challenge.getOtpHash().getBytes(StandardCharsets.UTF_8),
                hash(otp).getBytes(StandardCharsets.UTF_8))) {
            challenge.recordFailedAttempt();
            challenges.save(challenge);
            if (challenge.isLocked()) {
                authEvents.record(user.getId(), AuthEventService.OTP_LOCKED, "{\"reason\":\"max_attempts\"}");
                throw new ApiException(ErrorCode.OTP_LOCKED);
            }
            authEvents.record(user.getId(), AuthEventService.OTP_VERIFY_FAILED);
            throw new ApiException(ErrorCode.OTP_INVALID);
        }

        challenge.consume();
        challenges.save(challenge);
        authEvents.record(user.getId(), AuthEventService.OTP_VERIFIED);
        return user.getId();
    }

    /** Contract has no channel on verify — resolve by email first, then phone. */
    private Optional<UserEntity> findUserByIdentifier(String identifier) {
        return users.findByEmail(identifier).or(() -> users.findByPhone(identifier));
    }

    /** Emails are case-insensitive; normalize so request/verify/registration agree. */
    private static String normalize(String identifier) {
        String v = identifier == null ? null : identifier.trim();
        return v != null && v.contains("@") ? v.toLowerCase() : v;
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
