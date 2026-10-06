package com.starmitra.integration;

import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.pricing.application.EntitlementContract;
import com.starmitra.modules.pricing.application.PricingService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M22 vertical slice on real PostgreSQL — plan catalog, default FREE
 * assignment, entitlement resolution + limits, paid-plan fail-closed,
 * admin configuration. No payment rails are exercised or simulated.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PricingFlowIT {

    @Autowired OtpService otpService;
    @Autowired PricingService pricing;
    @Autowired EntitlementContract entitlements;
    @Autowired JdbcTemplate jdbc;

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
    }

    // ---------- catalog ----------

    @Test
    void approvedMvpCatalogIsPresentAndConfigurable() {
        var catalog = pricing.catalog();
        assertEquals(List.of("FREE", "CREATOR", "CREATOR_PRO", "COMPETITION_ORGANIZER"),
                catalog.stream().map(PricingService.PlanView::code).toList());
        var free = catalog.get(0);
        assertEquals(0, free.price().compareTo(BigDecimal.ZERO));
        assertEquals("INR", free.currency());
        assertEquals("NONE", free.billingPeriod());
        assertEquals("99.00", catalog.get(1).price().toPlainString());
        assertEquals("MONTH", catalog.get(1).billingPeriod());
        assertEquals("199.00", catalog.get(2).price().toPlainString());
        var organizer = catalog.get(3);
        assertEquals("EVENT_PACKAGE", organizer.planType());
        assertEquals("EVENT", organizer.billingPeriod());
        assertEquals("999.00", organizer.price().toPlainString());
    }

    // ---------- default plan ----------

    @Test
    void registrationAssignsFreeByDefault() {
        UUID uid = register("m22d");
        var row = jdbc.queryForMap(
                "select s.status, s.source, p.code from user_subscriptions s " +
                "join plans p on p.id = s.plan_id where s.user_id = ?", uid);
        assertEquals("ACTIVE", row.get("status"));
        assertEquals("SYSTEM_DEFAULT", row.get("source"));
        assertEquals("FREE", row.get("code"));

        var mine = pricing.myPlan(uid);
        assertEquals("FREE", mine.planCode());
        assertEquals("SYSTEM_DEFAULT", mine.source());
    }

    @Test
    void lazyDefaultBackfillsUsersWithoutSubscription() {
        UUID uid = register("m22l");
        jdbc.update("delete from user_subscriptions where user_id = ?", uid);  // simulate pre-M22 user
        var mine = pricing.myPlan(uid);                                       // lazy FREE
        assertEquals("FREE", mine.planCode());
        Integer rows = jdbc.queryForObject(
                "select count(*) from user_subscriptions where user_id=? and status='ACTIVE'",
                Integer.class, uid);
        assertEquals(1, rows);
    }

    // ---------- entitlement contract ----------

    @Test
    void freeEntitlementsResolvePerSeedConfig() {
        UUID uid = register("m22e");
        assertTrue(entitlements.hasEntitlement(uid, "portfolio.items"));
        assertTrue(entitlements.hasEntitlement(uid, "competition.enter"));
        assertFalse(entitlements.hasEntitlement(uid, "competition.create"));   // organizer-only
        assertFalse(entitlements.hasEntitlement(uid, "nonexistent.code"));

        var analytics = entitlements.getEntitlement(uid, "analytics.tier").orElseThrow();
        assertEquals("ENUM", analytics.kind());
        assertEquals("BASIC", analytics.value());
        var items = entitlements.getEntitlement(uid, "portfolio.items").orElseThrow();
        assertEquals(10L, items.limit());
    }

    @Test
    void limitsAccountForReportedUsage() {
        UUID uid = register("m22u");
        assertEquals(10, entitlements.getRemainingLimit(uid, "portfolio.items"));
        assertTrue(entitlements.checkLimit(uid, "portfolio.items", 10));
        assertFalse(entitlements.checkLimit(uid, "portfolio.items", 11));

        entitlements.recordUsage(uid, "portfolio.items", 4);
        assertEquals(6, entitlements.getRemainingLimit(uid, "portfolio.items"));
        assertTrue(entitlements.checkLimit(uid, "portfolio.items", 6));
        assertFalse(entitlements.checkLimit(uid, "portfolio.items", 7));

        entitlements.releaseUsage(uid, "portfolio.items", 4);
        assertEquals(10, entitlements.getRemainingLimit(uid, "portfolio.items"));

        // usage can never go negative (ck_eu_used)
        assertThrows(ApiException.class,
                () -> entitlements.releaseUsage(uid, "portfolio.items", 1));
        // ungranted entitlements have no remaining capacity
        assertEquals(0, entitlements.getRemainingLimit(uid, "competition.create"));
    }

    // ---------- plan selection ----------

    @Test
    void freeSelectionIsIdempotent() {
        UUID uid = register("m22f");
        var mine = pricing.selectPlan(uid, "FREE");      // already FREE — converge
        assertEquals("FREE", mine.planCode());
        Integer active = jdbc.queryForObject(
                "select count(*) from user_subscriptions where user_id=? and status='ACTIVE'",
                Integer.class, uid);
        assertEquals(1, active);
    }

    @Test
    void paidPlanSelectionFailsClosed() {
        UUID uid = register("m22p");
        var ex = assertThrows(ApiException.class, () -> pricing.selectPlan(uid, "CREATOR"));
        assertEquals(ErrorCode.PAYMENT_NOT_ENABLED, ex.code());
        ex = assertThrows(ApiException.class, () -> pricing.selectPlan(uid, "CREATOR_PRO"));
        assertEquals(ErrorCode.PAYMENT_NOT_ENABLED, ex.code());
        ex = assertThrows(ApiException.class, () -> pricing.selectPlan(uid, "COMPETITION_ORGANIZER"));
        assertEquals(ErrorCode.PAYMENT_NOT_ENABLED, ex.code());

        // nothing changed — still on FREE, exactly one ACTIVE row, no payment-source rows
        assertEquals("FREE", pricing.myPlan(uid).planCode());
        Integer rows = jdbc.queryForObject(
                "select count(*) from user_subscriptions where user_id=? " +
                "and (status='ACTIVE' or source='PAYMENT' or status='PENDING_PAYMENT')",
                Integer.class, uid);
        assertEquals(1, rows);   // the original FREE row only
    }

    @Test
    void unknownOrInactivePlanRejected() {
        UUID uid = register("m22n");
        assertThrows(ApiException.class, () -> pricing.selectPlan(uid, "NOPE"));
        assertThrows(ApiException.class, () -> pricing.selectPlan(uid, "  "));
    }

    // ---------- admin configuration ----------

    @Test
    void adminCanRepriceAndDeactivatePlans() {
        UUID admin = register("m22a");
        UUID user = register("m22u2");
        asAdmin(admin);

        var updated = pricing.adminUpdatePlan("CREATOR",
                new PricingService.PlanUpdate(null, null, new BigDecimal("149.00"),
                        null, null, null, null));
        assertEquals("149.00", updated.price().toPlainString());

        updated = pricing.adminUpdatePlan("CREATOR",
                new PricingService.PlanUpdate(null, null, null, "INACTIVE", null, null, null));
        assertEquals("INACTIVE", updated.status());
        assertFalse(pricing.catalog().stream().anyMatch(p -> "CREATOR".equals(p.code())));
        assertThrows(ApiException.class, () -> pricing.selectPlan(user, "CREATOR"));

        pricing.adminUpdatePlan("CREATOR",
                new PricingService.PlanUpdate(null, null, new BigDecimal("99.00"),
                        "ACTIVE", null, null, null));    // restore seed truth for other tests
    }

    @Test
    void adminCanTuneEntitlementValuesWithoutCodeChange() {
        UUID admin = register("m22a2");
        UUID user = register("m22u3");
        asAdmin(admin);

        pricing.adminSetEntitlement("FREE", "portfolio.items", "42");
        assertEquals(42, entitlements.getRemainingLimit(user, "portfolio.items"));

        assertThrows(ApiException.class, () ->
                pricing.adminSetEntitlement("FREE", "portfolio.items", "not-a-number"));
        assertThrows(ApiException.class, () ->
                pricing.adminSetEntitlement("FREE", "missing.code", "1"));

        pricing.adminSetEntitlement("FREE", "portfolio.items", "10");   // restore seed
    }

    @Test
    void nonAdminCannotMutatePlanOrEntitlements() {
        UUID user = register("m22x");
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(user.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThrows(ApiException.class, () -> pricing.adminCatalog());
        assertThrows(ApiException.class, () -> pricing.adminUpdatePlan("FREE",
                new PricingService.PlanUpdate(null, null, null, "INACTIVE", null, null, null)));
        assertThrows(ApiException.class, () ->
                pricing.adminSetEntitlement("FREE", "portfolio.items", "9999"));
        SecurityContextHolder.clearContext();
    }
}
