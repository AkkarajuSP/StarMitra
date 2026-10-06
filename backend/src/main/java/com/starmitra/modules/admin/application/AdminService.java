package com.starmitra.modules.admin.application;

import com.starmitra.modules.moderation.application.ModerationContract;
import com.starmitra.modules.moderation.application.ModerationService;
import com.starmitra.modules.pricing.application.PricingService;
import com.starmitra.platform.audit.AuditQueryService;
import com.starmitra.platform.security.SystemRoleGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * M19 — Admin orchestration/read domain. NO transactional tables, NO domain
 * ownership — every capability delegates to the owning module's contract.
 * Dashboard is a derived operational view, never business truth.
 */
@Service
public class AdminService {

    private final AuditQueryService auditQuery;
    private final ModerationService moderation;
    private final PricingService pricing;

    public AdminService(AuditQueryService auditQuery, ModerationService moderation,
                        PricingService pricing) {
        this.auditQuery = auditQuery;
        this.moderation = moderation;
        this.pricing = pricing;
    }

    /** Derived dashboard — counts/views from owning-module contracts only. */
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        SystemRoleGuard.requireAdmin();
        var m = new LinkedHashMap<String, Object>();
        m.put("openModerationCases",
                moderation.listCases("OPEN", 0, 100).size());       // M18-owned truth
        m.put("recentAuditEntries", auditQuery.search(null, null, 25).size());
        m.put("derived", true);                                    // never business truth
        return m;
    }

    /** Kernel audit search — admin-only read seam. */
    @Transactional(readOnly = true)
    public List<AuditQueryService.AuditView> audit(String module, UUID actorId, int limit) {
        SystemRoleGuard.requireAdmin();
        return auditQuery.search(module, actorId, limit);
    }

    // ---------- M22 plan administration (delegates — M22 stays authoritative) ----------

    /** All plans incl. INACTIVE + their configured entitlement values. */
    @Transactional(readOnly = true)
    public List<PricingService.PlanView> plans() {
        SystemRoleGuard.requireAdmin();
        return pricing.adminCatalog();
    }

    /** Update pricing/display/status of a plan — M22-owned truth. */
    @Transactional
    public PricingService.PlanView updatePlan(String planCode, PricingService.PlanUpdate cmd) {
        SystemRoleGuard.requireAdmin();
        return pricing.adminUpdatePlan(planCode, cmd);
    }

    /** Set one entitlement value on a plan — M22-owned truth. */
    @Transactional
    public PricingService.PlanView setPlanEntitlement(String planCode, String entitlementCode,
                                                      String value) {
        SystemRoleGuard.requireAdmin();
        return pricing.adminSetEntitlement(planCode, entitlementCode, value);
    }
}
