package com.starmitra.integration;

import com.starmitra.modules.identity.application.AuthService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.profile.application.ProfileService;
import com.starmitra.modules.profile.persistence.UserProfileEntity;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M02 slice on real PostgreSQL — lazy init, visibility, ETag, persistence. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProfileFlowIT {

    @Autowired OtpService otpService;
    @Autowired AuthService authService;
    @Autowired ProfileService profileService;
    @Autowired JdbcTemplate jdbc;

    private UUID me;

    @BeforeEach
    void register() {
        TestOtpSender.clear();
        String email = "m02+" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        String otp = otpService.request("EMAIL", email) != null
                ? TestOtpSender.lastOtpFor(email).orElseThrow() : null;
        me = otpService.verify(email, otp);
    }

    @Test
    void profileLazilyCreatedAndPersisted() {
        var v = profileService.myProfile(me, "m02user");
        Integer count = jdbc.queryForObject(
                "select count(*) from user_profiles where user_id=?", Integer.class, me);
        assertEquals(1, count);
        assertEquals("PRIVATE", v.visibilityState());   // default
        assertNotNull(v.etag());
    }

    @Test
    void oneProfilePerUserEnforced() {
        profileService.myProfile(me, "a");
        profileService.myProfile(me, "b");   // second call reuses, not duplicates
        Integer count = jdbc.queryForObject(
                "select count(*) from user_profiles where user_id=?", Integer.class, me);
        assertEquals(1, count);
    }

    @Test
    void updatePersistsAndRotatesEtag() {
        var v1 = profileService.myProfile(me, "x");
        var v2 = profileService.updateMyProfile(me, "x",
                new ProfileService.UpdateCommand("newname", "bio", "City", null, "PUBLIC"), v1.etag());
        assertEquals("newname", v2.displayName());
        assertNotEquals(v1.etag(), v2.etag());
        assertEquals("PUBLIC", jdbc.queryForObject(
                "select visibility_state from user_profiles where user_id=?", String.class, me));
    }

    @Test
    void staleEtagRejected() {
        var v = profileService.myProfile(me, "x");
        profileService.updateMyProfile(me, "x",
                new ProfileService.UpdateCommand("first", null, null, null, null), null);
        var e = assertThrows(ApiException.class, () -> profileService.updateMyProfile(me, "x",
                new ProfileService.UpdateCommand("second", null, null, null, null), v.etag()));
        assertEquals(ErrorCode.CONFLICT_VERSION, e.code());
        assertEquals("first", jdbc.queryForObject(
                "select display_name from user_profiles where user_id=?", String.class, me));
    }

    @Test
    void publicProfileVisibilityLifecycle() {
        var other = UUID.randomUUID();   // no user/profile → NOT_FOUND
        assertThrows(ApiException.class, () -> profileService.publicProfile(me, other));

        profileService.myProfile(me, "x");   // PRIVATE by default
        var self = UUID.randomUUID();
        var e = assertThrows(ApiException.class, () -> profileService.publicProfile(self, me));
        assertEquals(ErrorCode.NOT_FOUND, e.code());

        profileService.updateMyProfile(me, "x",
                new ProfileService.UpdateCommand(null, null, null, null, "PUBLIC"), null);
        var pub = profileService.publicProfile(self, me);
        assertEquals(me, pub.userId());
    }

    @Test
    void auditEventsRecorded() {
        profileService.myProfile(me, "x");
        profileService.updateMyProfile(me, "x",
                new ProfileService.UpdateCommand("n", null, null, null, "PUBLIC"), null);
        Integer events = jdbc.queryForObject(
                "select count(*) from audit_log where actor_id=? and action like 'PROFILE_%'",
                Integer.class, me);
        assertTrue(events >= 2);
    }
}
