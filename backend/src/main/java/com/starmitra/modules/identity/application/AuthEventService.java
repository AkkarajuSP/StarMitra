package com.starmitra.modules.identity.application;

import com.starmitra.modules.identity.persistence.AuthenticationAuditEventEntity;
import com.starmitra.modules.identity.persistence.AuthenticationAuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * M01 authentication-event recorder — writes to authentication_audit_events
 * (module-owned). Security-critical escalations also go to the kernel
 * audit_log via AuditService (e.g. refresh-token reuse).
 */
@Service
public class AuthEventService {

    private static final Logger log = LoggerFactory.getLogger(AuthEventService.class);

    private final AuthenticationAuditEventRepository events;

    public AuthEventService(AuthenticationAuditEventRepository events) {
        this.events = events;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID userId, String eventType) {
        record(userId, eventType, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID userId, String eventType, String metadataJson) {
        events.save(new AuthenticationAuditEventEntity(userId, eventType, metadataJson));
        log.debug("auth-event {} user={}", eventType, userId);
    }

    // Canonical event names — keep stable for queries/reporting.
    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String OTP_REQUESTED = "OTP_REQUESTED";
    public static final String OTP_REQUEST_UNKNOWN = "OTP_REQUEST_UNKNOWN";
    public static final String OTP_REQUEST_LOCKED = "OTP_REQUEST_LOCKED";
    public static final String OTP_RESEND_COOLDOWN = "OTP_RESEND_COOLDOWN";
    public static final String OTP_VERIFIED = "OTP_VERIFIED";
    public static final String OTP_VERIFY_FAILED = "OTP_VERIFY_FAILED";
    public static final String OTP_LOCKED = "OTP_LOCKED";
    public static final String SESSION_ESTABLISHED = "SESSION_ESTABLISHED";
    public static final String REFRESH_ROTATED = "REFRESH_ROTATED";
    public static final String REFRESH_REUSE_DETECTED = "REFRESH_REUSE_DETECTED";
    public static final String SESSION_REVOKED = "SESSION_REVOKED";
}
