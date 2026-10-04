package com.starmitra.modules.admin.api;

import com.starmitra.modules.admin.application.AdminService;
import com.starmitra.platform.pagination.Cursor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
