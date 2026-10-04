package com.starmitra.modules.admin.application;

import com.starmitra.modules.moderation.application.ModerationContract;
import com.starmitra.modules.moderation.application.ModerationService;
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

    public AdminService(AuditQueryService auditQuery, ModerationService moderation) {
        this.auditQuery = auditQuery;
        this.moderation = moderation;
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
}
