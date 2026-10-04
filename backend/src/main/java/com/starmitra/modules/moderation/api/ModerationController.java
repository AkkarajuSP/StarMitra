package com.starmitra.modules.moderation.api;

import com.starmitra.modules.moderation.application.ModerationService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /api/v1/moderation — M18. Reports user-scoped; cases/decisions/restrictions admin. */
@RestController
public class ModerationController {

    private final ModerationService moderation;

    public ModerationController(ModerationService moderation) {
        this.moderation = moderation;
    }

    public record ReportCreate(@NotBlank String targetType, @NotNull UUID targetId,
                               @NotBlank String reasonCode, String detail) {}
    public record Report(UUID id, String status) {}
    public record ModerationCase(UUID id, String status, UUID assignedModeratorId) {}
    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}
    public record CasePage(List<ModerationCase> items, PageMeta page) {}
    public record DecisionCreate(@NotBlank String decisionType, @NotBlank String reason,
                                 UUID policyVersionId, List<Map<String, Object>> actions) {}
    public record RestrictionCreate(@NotBlank String targetType, @NotNull UUID targetId,
                                    @NotBlank String restrictionType, OffsetDateTime expiresAt) {}

    @PostMapping("/api/v1/moderation/reports")
    public ResponseEntity<Report> createReport(@Valid @RequestBody ReportCreate body) {
        var r = moderation.createReport(SecurityUtils.currentUserId(), body.targetType(),
                body.targetId(), body.reasonCode(), body.detail());
        return ResponseEntity.status(HttpStatus.CREATED).body(new Report(r.id(), r.status()));
    }

    @GetMapping("/api/v1/moderation/cases")
    public ResponseEntity<CasePage> listModerationCases(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        var items = moderation.listCases(status, offset, limit).stream()
                .map(c -> new ModerationCase(c.id(), c.status(), c.assignedModeratorId()))
                .toList();
        return ResponseEntity.ok(new CasePage(items, null));
    }

    @GetMapping("/api/v1/moderation/cases/{caseId}")
    public ResponseEntity<ModerationCase> getModerationCase(@PathVariable UUID caseId) {
        var c = moderation.getCase(caseId);
        return ResponseEntity.ok(new ModerationCase(c.id(), c.status(), c.assignedModeratorId()));
    }

    @PostMapping("/api/v1/moderation/cases/{caseId}/decisions")
    public ResponseEntity<Map<String, String>> createModerationDecision(
            @PathVariable UUID caseId, @Valid @RequestBody DecisionCreate body) {
        var id = moderation.decide(SecurityUtils.currentUserId(), caseId, body.decisionType(),
                body.reason(), body.policyVersionId(), body.actions());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id.toString()));
    }

    @PostMapping("/api/v1/moderation/restrictions")
    public ResponseEntity<Void> createRestriction(@Valid @RequestBody RestrictionCreate body) {
        moderation.restrict(SecurityUtils.currentUserId(), body.targetType(), body.targetId(),
                body.restrictionType(), body.expiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
