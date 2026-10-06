package com.starmitra.modules.admin.api;

import com.starmitra.modules.admin.application.AdminService;
import com.starmitra.modules.pricing.application.PricingService;
import com.starmitra.platform.pagination.Cursor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /api/v1/admin — M19 orchestration + audit read. Admin-only; no domain ownership. */
@RestController
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}
    public record AuditPage(List<Object> items, PageMeta page) {}

    @GetMapping("/api/v1/admin/dashboard")
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        return ResponseEntity.ok(admin.dashboard());
    }

    @GetMapping("/api/v1/admin/audit")
    public ResponseEntity<AuditPage> adminAuditSearch(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        var items = admin.audit(module, actorId, Cursor.limit(limit)).stream()
                .<Object>map(a -> Map.of(
                        "module", a.module(), "action", a.action(),
                        "actorId", a.actorId() == null ? "" : a.actorId(),
                        "targetType", a.targetType() == null ? "" : a.targetType(),
                        "targetId", a.targetId() == null ? "" : a.targetId(),
                        "createdAt", a.createdAt()))
                .toList();
        return ResponseEntity.ok(new AuditPage(items, null));
    }

    // ---------- M22 plan administration (M19 orchestrates; M22 authoritative) ----------

    public record AdminPlan(UUID id, String code, String displayName, String description,
                            String planType, BigDecimal price, String currency,
                            String billingPeriod, String status, int sortOrder,
                            Map<String, String> entitlements) {}

    public record PlanUpdate(String displayName, String description, BigDecimal price,
                             String status, Integer sortOrder,
                             OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo) {}

    public record EntitlementSet(@NotBlank String value) {}

    @GetMapping("/api/v1/admin/plans")
    public ResponseEntity<List<AdminPlan>> adminPlans() {
        return ResponseEntity.ok(admin.plans().stream().map(this::toAdminPlan).toList());
    }

    @PutMapping("/api/v1/admin/plans/{planCode}")
    public ResponseEntity<AdminPlan> updatePlan(@PathVariable String planCode,
                                                @RequestBody PlanUpdate body) {
        return ResponseEntity.ok(toAdminPlan(admin.updatePlan(planCode,
                new PricingService.PlanUpdate(body.displayName(), body.description(), body.price(),
                        body.status(), body.sortOrder(), body.effectiveFrom(), body.effectiveTo()))));
    }

    @PutMapping("/api/v1/admin/plans/{planCode}/entitlements/{entitlementCode}")
    public ResponseEntity<AdminPlan> setPlanEntitlement(@PathVariable String planCode,
                                                      @PathVariable String entitlementCode,
                                                      @Valid @RequestBody EntitlementSet body) {
        return ResponseEntity.ok(toAdminPlan(
                admin.setPlanEntitlement(planCode, entitlementCode, body.value())));
    }

    private AdminPlan toAdminPlan(PricingService.PlanView p) {
        return new AdminPlan(p.id(), p.code(), p.displayName(), p.description(), p.planType(),
                p.price(), p.currency(), p.billingPeriod(), p.status(), p.sortOrder(),
                p.entitlements());
    }
}
