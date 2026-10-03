package com.starmitra.integration;

import com.starmitra.modules.identity.application.*;
import com.starmitra.modules.identity.persistence.*;
import com.starmitra.platform.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M01 vertical slice on real PostgreSQL — OTP-first registration, session
 * establishment, rotation, reuse detection, revocation, constraints.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IdentityFlowIT {

    @Autowired OtpService otpService;
    @Autowired AuthService authService;
    @Autowired RefreshTokenService refreshTokenService;
    @Autowired SessionService sessionService;
    @Autowired UserRepository users;
    @Autowired OtpChallengeRepository challenges;
    @Autowired AuthenticationAuditEventRepository authEvents;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;

    @BeforeEach
    void setUp() { TestOtpSender.clear(); }

    private String registerAndGetOtp(String email) {
        UUID userId = otpService.request("EMAIL", email);
        assertNotNull(userId);
        return TestOtpSender.lastOtpFor(email).orElseThrow();
    }

    @Test
    void fullOtpFirstJourney_registerVerifyRefreshRotateLogout() {
        String email = "flow+" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        String otp = registerAndGetOtp(email);

        // verify → session (JWT + opaque refresh, new family)
        AuthService.Session s1 = authService.completeOtpLogin(email, otp);
        assertNotNull(s1.accessToken());
        assertTrue(s1.accessToken().split("\\.").length == 3);
        assertNotNull(s1.refreshToken());
        assertNotEquals(s1.refreshToken(), s1.accessToken());

        // USER role assigned on registration
        var roles = jdbc.queryForList(
                "select r.name from system_roles r join user_system_roles ur on ur.role_id=r.id where ur.user_id=?",
                String.class, s1.userId());
        assertEquals(java.util.List.of("USER"), roles);

        // challenge consumed — replay fails
        assertThrows(ApiException.class, () -> authService.completeOtpLogin(email, otp));

        // rotate → new pair, old token invalidated
        AuthService.Session s2 = authService.refresh(s1.refreshToken());
        assertNotEquals(s1.refreshToken(), s2.refreshToken());

        // reuse of rotated token → family revoked, next refresh fails
        assertThrows(ApiException.class, () -> authService.refresh(s1.refreshToken()));
        assertThrows(ApiException.class, () -> authService.refresh(s2.refreshToken()));

        // audit events recorded
        Integer events = jdbc.queryForObject(
                "select count(*) from authentication_audit_events where user_id=?",
                Integer.class, s1.userId());
        assertTrue(events >= 3);   // REGISTERED + REQUESTED + VERIFIED + SESSION + REUSE…
    }

    @Test
    void refreshTokenReuseRevokesEntireFamily() {
        String email = "reuse+" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        String otp = registerAndGetOtp(email);
        var s = authService.completeOtpLogin(email, otp);

        var s2 = authService.refresh(s.refreshToken());
        assertThrows(ApiException.class, () -> authService.refresh(s.refreshToken()));  // replay → reuse

        Integer active = jdbc.queryForObject(
                "select count(*) from refresh_tokens where user_id=? and revoked_at is null",
                Integer.class, s.userId());
        assertEquals(0, active);   // whole family dead
    }

    @Test
    void wrongOtpIncrementsAttempts() {
        String email = "bad+" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        registerAndGetOtp(email);
        UUID uid = users.findByEmail(email).orElseThrow().getId();

        assertThrows(ApiException.class, () -> otpService.verify(email, "000000"));
        em.flush();    // failed-attempt counter must survive the ApiException (noRollbackFor)
        Integer attempts = jdbc.queryForObject(
                "select attempts from otp_challenges where user_id=? order by created_at desc limit 1",
                Integer.class, uid);
        assertEquals(1, attempts);
    }

    @Test
    void newRequestSupersedesOlderChallenge() {
        String email = "sup+" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        registerAndGetOtp(email);
        String second = registerAndGetOtp(email);   // resend → supersede

        UUID uid = users.findByEmail(email).orElseThrow().getId();
        Integer active = jdbc.queryForObject(
                "select count(*) from otp_challenges where user_id=? and consumed_at is null",
                Integer.class, uid);
        assertEquals(1, active);   // only the latest is live
        assertNotNull(authService.completeOtpLogin(email, second).accessToken());
    }

    @Test
    void sessionRevocationKillsRefresh() {
        String email = "rev+" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        String otp = registerAndGetOtp(email);
        var s = authService.completeOtpLogin(email, otp);

        var sessions = sessionService.listForUser(s.userId(), s.refreshToken());
        assertEquals(1, sessions.size());
        assertTrue(sessions.get(0).current());

        sessionService.revokeSession(s.userId(), sessions.get(0).id());
        assertThrows(ApiException.class, () -> authService.refresh(s.refreshToken()));
    }

    @Test
    void cannotRevokeAnotherUsersSession() {
        String e1 = "u1+" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        String e2 = "u2+" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        var s1 = authService.completeOtpLogin(e1, registerAndGetOtp(e1));
        var s2 = authService.completeOtpLogin(e2, registerAndGetOtp(e2));

        var otherSession = sessionService.listForUser(s2.userId(), null).get(0);
        assertThrows(ApiException.class,
                () -> sessionService.revokeSession(s1.userId(), otherSession.id()));
    }

    @Test
    void constraintsEnforced() {
        // otp_challenges attempts <= max_attempts check
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from pg_constraint where conname='ck_oc_attempts'", Integer.class));
        // refresh family + unique token hash
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from pg_constraint where conname='uq_refresh_tokens_hash'", Integer.class));
        // email uniqueness
        String email = "dup+" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        registerAndGetOtp(email);
        Integer dupes = jdbc.queryForObject("select count(*) from users where email=?", Integer.class, email);
        assertEquals(1, dupes);
    }
}
