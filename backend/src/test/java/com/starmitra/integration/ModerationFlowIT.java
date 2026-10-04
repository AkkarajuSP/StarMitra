package com.starmitra.integration;

import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.moderation.application.ModerationService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M18 on real PG17 — reports, cases, append-only decisions, restrictions. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ModerationFlowIT {

    @Autowired OtpService otpService;
    @Autowired ModerationService moderation;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID admin, reporter, moderator, target;

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

    private void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        clearAuth();
        admin = register("m18ad");
        reporter = register("m18r");
        moderator = register("m18m");
        target = register("m18t");
        asAdmin(admin);
    }

    @Test
    void reportLifecycleWithCase() {
        var r = moderation.createReport(reporter, "PROFILE", target, "HARASSMENT", "slurs");
        assertEquals("OPEN", r.status());
        // duplicate active report → CONFLICT
        assertThrows(ApiException.class, () -> moderation.createReport(
                reporter, "PROFILE", target, "HARASSMENT", null));
        // another reporter → allowed (legitimate independent report)
        var r2 = moderation.createReport(moderator, "PROFILE", target, "SPAM", null);
        assertEquals("OPEN", r2.status());
        em.flush();
        // case created per report, linked as evidence (report never deleted)
        assertEquals(2, jdbc.queryForObject(
                "select count(*) from moderation_cases", Integer.class));
        assertEquals(2, jdbc.queryForObject(
                "select count(*) from moderation_evidence_references where evidence_type='REPORT'",
                Integer.class));
    }

    @Test
    void controlledTaxonomy() {
        assertThrows(ApiException.class, () -> moderation.createReport(
                reporter, "WIDGET", target, "SPAM", null));            // forged target type
        assertThrows(ApiException.class, () -> moderation.createReport(
                reporter, "PROFILE", target, "NOT_A_REASON", null));   // forged reason
    }

    @Test
    void caseQueueAndDecision() {
        moderation.createReport(reporter, "PROFILE", target, "SPAM", null);
        em.flush();
        var caseId = jdbc.queryForObject("select id from moderation_cases order by created_at limit 1", UUID.class);
        moderation.assignAndReview(admin, caseId);
        assertEquals("UNDER_REVIEW", moderation.getCase(caseId).status());
        var dId = moderation.decide(admin, caseId, "USER_RESTRICTED", "policy violation", null,
                List.of(Map.of("actionType", "APPLY", "targetType", "USER",
                        "targetId", target.toString())));
        assertEquals("RESOLVED", moderation.getCase(caseId).status());
        // decisions append-only — never overwritten
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from moderation_decisions", Integer.class));
        // closed case can't regress
        assertThrows(ApiException.class, () -> moderation.assignAndReview(admin, caseId));
    }

    @Test
    void adminOnlyModerationSurface() {
        moderation.createReport(reporter, "PROFILE", target, "SPAM", null);
        em.flush();
        var caseId = jdbc.queryForObject("select id from moderation_cases order by created_at limit 1", UUID.class);
        clearAuth();                                                    // no ADMIN role
        assertThrows(ApiException.class, () -> moderation.listCases(null, 0, 10));
        assertThrows(ApiException.class, () -> moderation.getCase(caseId));
        assertThrows(ApiException.class, () -> moderation.decide(
                UUID.randomUUID(), caseId, "WARNING", "x", null, null));
        assertThrows(ApiException.class, () -> moderation.restrict(
                UUID.randomUUID(), "USER", target, "POSTING", null));
        asAdmin(admin);
    }

    @Test
    void restrictionsAreGovernanceState() {
        moderation.restrict(admin, "USER", target, "POSTING", null);
        moderation.restrict(admin, "USER", target, "COMMENTING", null);
        var active = moderation.activeRestrictions("USER", target);
        assertEquals(2, active.size());
        assertTrue(active.contains("POSTING"));
        // distinct from M21 user_block — separate table/state
        assertTrue(moderation.isRestricted("USER", target));
        assertFalse(moderation.isRestricted("USER", reporter));
    }
}
