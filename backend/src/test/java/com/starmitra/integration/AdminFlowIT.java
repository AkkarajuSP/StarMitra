package com.starmitra.integration;

import com.starmitra.modules.admin.application.AdminService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.moderation.application.ModerationService;
import com.starmitra.platform.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M19 on real PG17 — admin orchestration: dashboard + kernel audit search. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminFlowIT {

    @Autowired OtpService otpService;
    @Autowired AdminService admin;
    @Autowired ModerationService moderation;

    private UUID adminId, reporter, target;

    private UUID register(String tag) {
        String email = tag + UUID.randomUUID().toString().substring(0, 6) + "@t.dev";
        otpService.request("EMAIL", email);
        return otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
    }

    private void asAdmin(UUID uid) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(uid.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        adminId = register("m19ad");
        reporter = register("m19r");
        target = register("m19t");
        asAdmin(adminId);
    }

    @Test
    void dashboardDerivesFromModuleTruth() {
        moderation.createReport(reporter, "PROFILE", target, "SPAM", null);
        var d = admin.dashboard();
        assertEquals(true, d.get("derived"));                    // never business truth
        assertEquals(1, d.get("openModerationCases"));           // M18-owned count
        assertTrue((Integer) d.get("recentAuditEntries") > 0);
    }

    @Test
    void auditSearchFiltersByModuleAndActor() {
        moderation.createReport(reporter, "PROFILE", target, "HARASSMENT", "abuse");
        var all = admin.audit(null, null, 50);
        assertTrue(all.stream().anyMatch(a -> "M18".equals(a.module())
                && "REPORT_CREATED".equals(a.action())));
        var m18 = admin.audit("M18", null, 50);
        assertFalse(m18.isEmpty());
        assertTrue(m18.stream().allMatch(a -> "M18".equals(a.module())));
        var byActor = admin.audit("M18", reporter, 50);
        assertTrue(byActor.stream().allMatch(a -> reporter.toString().equals(a.actorId())));
    }

    @Test
    void adminOnlySurface() {
        SecurityContextHolder.clearContext();            // no ROLE_ADMIN
        assertThrows(ApiException.class, () -> admin.dashboard());
        assertThrows(ApiException.class, () -> admin.audit(null, null, 10));
        // judge can't admin
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(UUID.randomUUID().toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_JUDGE"))));
        assertThrows(ApiException.class, () -> admin.dashboard());
        asAdmin(adminId);
    }
}
