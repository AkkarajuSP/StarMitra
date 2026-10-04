package com.starmitra.modules.moderation.application;

import com.starmitra.modules.moderation.persistence.*;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.security.SystemRoleGuard;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * M18 — Moderation. Decides + audits; owning modules enforce via their own
 * contracts (M18 never mutates M02/M04/M06/M07/M10/M21 resource state).
 *
 * - Reports: user-scoped, controlled target+reason taxonomy; one OPEN/
 *   UNDER_REVIEW report per reporter+target (pre-check; no dup spam)
 * - Cases: one per report; report linked as REPORT evidence (never deleted);
 *   optimistic-locked transitions OPEN→UNDER_REVIEW→RESOLVED/DISMISSED
 * - Decisions: append-only (never overwritten); action rows mark what the
 *   OWNING module must enforce — M18 records, it doesn't mutate resources
 * - Restrictions: admin-enforced governance state (≠ M21 user_block)
 */
@Service
public class ModerationService implements ModerationContract {

    private final ModerationReportRepository reports;
    private final ModerationCaseRepository cases;
    private final ModerationDecisionRepository decisions;
    private final ModerationActionRepository actions;
    private final ModerationEvidenceRepository evidence;
    private final ModerationRestrictionRepository restrictions;
    private final AuditService audit;

    private static final java.util.Set<String> REASONS = java.util.Set.of(
            "SPAM", "HARASSMENT", "ABUSIVE_CONTENT", "INAPPROPRIATE_CONTENT",
            "COPYRIGHT", "IMPERSONATION", "FRAUD", "OTHER");

    public ModerationService(ModerationReportRepository reports,
                             ModerationCaseRepository cases,
                             ModerationDecisionRepository decisions,
                             ModerationActionRepository actions,
                             ModerationEvidenceRepository evidence,
                             ModerationRestrictionRepository restrictions,
                             AuditService audit) {
        this.reports = reports;
        this.cases = cases;
        this.decisions = decisions;
        this.actions = actions;
        this.evidence = evidence;
        this.restrictions = restrictions;
        this.audit = audit;
    }

    public record ReportView(UUID id, String status) {}
    public record CaseView(UUID id, String status, UUID assignedModeratorId) {}

    // ---------- reports (any authenticated user) ----------

    @Transactional
    public ReportView createReport(UUID reporterId, String targetType, UUID targetId,
                                   String reasonCode, String detail) {
        var type = parseTarget(targetType);
        if (!REASONS.contains(reasonCode)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid reason code");
        }
        long open = reports.countByReporterIdAndTargetTypeAndTargetIdAndStatusIn(reporterId, type,
                targetId, List.of(ModerationReportEntity.Status.OPEN,
                        ModerationReportEntity.Status.UNDER_REVIEW));
        if (open > 0) {
            throw new ApiException(ErrorCode.CONFLICT, "Active report already exists");
        }
        var report = reports.save(new ModerationReportEntity(reporterId, type, targetId,
                reasonCode, detail));
        // case per report; report linked as evidence (original never deleted)
        var c = cases.save(new ModerationCaseEntity());
        evidence.save(new ModerationEvidenceEntity(c.getId(), "REPORT",
                "moderation_report", report.getId()));
        audit.record("M18", "REPORT_CREATED", reporterId, "user",
                "report", report.getId().toString(), type + "/" + targetId);
        return new ReportView(report.getId(), report.getStatus().name());
    }

    // ---------- cases (moderator/admin queue) ----------

    @Transactional(readOnly = true)
    public List<CaseView> listCases(String status, int offset, int limit) {
        SystemRoleGuard.requireAdmin();
        var s = status == null ? ModerationCaseEntity.Status.OPEN
                : ModerationCaseEntity.Status.valueOf(status);
        return cases.byStatus(s, PageRequest.of(Math.max(0, offset / Math.max(1, limit)),
                        Math.max(1, Math.min(limit, 100)))).stream()
                .map(c -> new CaseView(c.getId(), c.getStatus().name(),
                        c.getAssignedModeratorId())).toList();
    }

    @Transactional(readOnly = true)
    public CaseView getCase(UUID caseId) {
        SystemRoleGuard.requireAdmin();
        var c = cases.findById(caseId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        return new CaseView(c.getId(), c.getStatus().name(), c.getAssignedModeratorId());
    }

    /** Optimistic-locked transition (two moderators can't both resolve). */
    @Transactional
    public void assignAndReview(UUID moderator, UUID caseId) {
        SystemRoleGuard.requireAdmin();
        var c = cases.findById(caseId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        c.transition(ModerationCaseEntity.Status.UNDER_REVIEW, moderator);
        cases.saveAndFlush(c);                               // @Version increments
        audit.record("M18", "CASE_REVIEWED", moderator, "user", "case",
                caseId.toString(), null);
    }

    /** Append-only decision + enforcement action rows; closes the case. */
    @Transactional
    public UUID decide(UUID moderator, UUID caseId, String decisionType, String reason,
                       UUID policyVersionId, List<Map<String, Object>> actionCmds) {
        SystemRoleGuard.requireAdmin();
        var type = switch (decisionType == null ? "" : decisionType) {
            case "NO_ACTION" -> ModerationDecisionEntity.Type.NO_ACTION;
            case "WARNING" -> ModerationDecisionEntity.Type.WARNING;
            case "CONTENT_RESTRICTED" -> ModerationDecisionEntity.Type.CONTENT_RESTRICTED;
            case "CONTENT_HIDDEN" -> ModerationDecisionEntity.Type.CONTENT_HIDDEN;
            case "CONTENT_REMOVED" -> ModerationDecisionEntity.Type.CONTENT_REMOVED;
            case "USER_RESTRICTED" -> ModerationDecisionEntity.Type.USER_RESTRICTED;
            case "USER_SUSPENDED" -> ModerationDecisionEntity.Type.USER_SUSPENDED;
            default -> throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid decision");
        };
        var c = cases.findById(caseId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        var d = decisions.save(new ModerationDecisionEntity(caseId, moderator, type, reason,
                policyVersionId));
        if (actionCmds != null) {
            for (var a : actionCmds) {
                actions.save(new ModerationActionEntity(d.getId(),
                        String.valueOf(a.getOrDefault("actionType", "APPLY")),
                        String.valueOf(a.getOrDefault("targetType", "")),
                        UUID.fromString(String.valueOf(a.get("targetId"))),
                        a.get("expiresAt") == null ? null
                                : OffsetDateTime.parse(String.valueOf(a.get("expiresAt")))));
            }
        }
        c.transition(ModerationCaseEntity.Status.RESOLVED, moderator);
        cases.saveAndFlush(c);
        audit.record("M18", "DECISION_MADE", moderator, "user", "decision",
                d.getId().toString(), decisionType);
        return d.getId();
    }

    /** Admin-enforced restriction (governance — never a user-to-user block). */
    @Transactional
    public void restrict(UUID admin, String targetType, UUID targetId, String restrictionType,
                         OffsetDateTime expiresAt) {
        SystemRoleGuard.requireAdmin();
        parseTarget(targetType);
        restrictions.save(new ModerationRestrictionEntity(targetType, targetId, restrictionType,
                expiresAt, admin));
        audit.record("M18", "RESTRICTION_APPLIED", admin, "user", "restriction",
                targetType + "/" + targetId, restrictionType);
    }

    // ---------- ModerationContract (enforcement for owning modules) ----------

    @Override @Transactional(readOnly = true)
    public List<String> activeRestrictions(String targetType, UUID targetId) {
        return restrictions.findByTargetTypeAndTargetIdAndStatus(targetType, targetId,
                        ModerationRestrictionEntity.Status.ACTIVE).stream()
                .map(ModerationRestrictionEntity::getRestrictionType).toList();
    }

    // ---------- internals ----------

    private ModerationReportEntity.TargetType parseTarget(String t) {
        try {
            return ModerationReportEntity.TargetType.valueOf(t == null ? "" : t);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid target type");
        }
    }
}
