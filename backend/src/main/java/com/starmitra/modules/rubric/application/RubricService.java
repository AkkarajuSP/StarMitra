package com.starmitra.modules.rubric.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starmitra.modules.competition.application.CompetitionStructureContract;
import com.starmitra.modules.judge.application.JudgeScopeService;
import com.starmitra.modules.rubric.persistence.*;
import com.starmitra.modules.submission.application.SubmissionTruthContract;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.security.SystemRoleGuard;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * M13 — Judge Rubrics. Dynamic, VERSIONED rubric truth.
 *
 * - DRAFT editable → PUBLISHED **immutable** (never mutated; new version instead)
 * - publish requires criteria weights summing to EXACTLY 100 (app-enforced)
 * - payload JSONB carries applicability scope + configurable scoreScale
 *   (FRS suggests 1–10; NOT hard-coded)
 * - evaluations: one per judge+submission+round (DB-09 UQ), bound to the
 *   PUBLISHED version; scope enforced via M12 assignment + M10 submission truth
 * - scoring aggregation is M14 — M13 provides RubricContract only
 */
@Service
public class RubricService implements RubricContract {

    private final EvaluationTemplateRepository templates;
    private final EvaluationTemplateVersionRepository versions;
    private final EvaluationCriterionRepository criteria;
    private final JudgeEvaluationRepository evaluations;
    private final JudgeEvaluationScoreRepository scores;
    private final JudgeScopeService judgeScope;
    private final SubmissionTruthContract submissions;
    private final CompetitionStructureContract competition;
    private final AuditService audit;
    private final ObjectMapper json;

    public RubricService(EvaluationTemplateRepository templates,
                         EvaluationTemplateVersionRepository versions,
                         EvaluationCriterionRepository criteria,
                         JudgeEvaluationRepository evaluations,
                         JudgeEvaluationScoreRepository scores,
                         JudgeScopeService judgeScope,
                         SubmissionTruthContract submissions,
                         CompetitionStructureContract competition,
                         AuditService audit, ObjectMapper json) {
        this.templates = templates;
        this.versions = versions;
        this.criteria = criteria;
        this.evaluations = evaluations;
        this.scores = scores;
        this.judgeScope = judgeScope;
        this.submissions = submissions;
        this.competition = competition;
        this.audit = audit;
        this.json = json;
    }

    public record CriterionCmd(String name, String description, BigDecimal weight,
                               BigDecimal maxScore) {}
    public record TemplateView(UUID id, String name, String status, Integer latestVersionNo) {}
    public record VersionView(UUID id, UUID templateId, int versionNo, String status,
                              String updatedAt) {}
    public record ScoreCmd(UUID criterionId, BigDecimal score, String comment) {}
    public record EvaluationView(UUID id, UUID judgeId, UUID submissionId,
                                 UUID rubricVersionId, String status, String submittedAt) {}

    // ---------- templates & versions (admin) ----------

    /** Create template + DRAFT v1 with initial criteria (weights don't need 100 until publish). */
    @Transactional
    public TemplateView createTemplate(UUID admin, String name, List<CriterionCmd> initial,
                                       Integer scaleMin, Integer scaleMax,
                                       UUID competitionId, UUID categoryId, UUID roundId) {
        SystemRoleGuard.requireAdmin();
        var t = templates.save(new EvaluationTemplateEntity(name, admin));
        var v = new EvaluationTemplateVersionEntity(t.getId(), 1, payload(scaleMin, scaleMax,
                competitionId, categoryId, roundId));
        versions.save(v);
        writeCriteria(v.getId(), initial == null ? List.of() : initial);
        return new TemplateView(t.getId(), t.getName(), t.getStatus(), 1);
    }

    @Transactional(readOnly = true)
    public List<TemplateView> listTemplates(int offset, int limit) {
        return templates.findAll(org.springframework.data.domain.PageRequest.of(
                        Math.max(0, offset / Math.max(1, limit)), Math.max(1, Math.min(limit, 100))))
                .stream().map(t -> new TemplateView(t.getId(), t.getName(), t.getStatus(),
                        versions.maxVersion(t.getId()))).toList();
    }

    /**
     * Update = edit the latest DRAFT criteria (in-place replace) OR, when the
     * head is PUBLISHED, create the next DRAFT version (FRS version lifecycle:
     * never mutate a published version).
     */
    @Transactional
    public VersionView updateTemplate(UUID templateId, List<CriterionCmd> newCriteria,
                                      Integer scaleMin, Integer scaleMax,
                                      UUID competitionId, UUID categoryId, UUID roundId) {
        SystemRoleGuard.requireAdmin();
        templates.findById(templateId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        var draft = versions.draftsOf(templateId).stream().findFirst().orElse(null);
        EvaluationTemplateVersionEntity target;
        if (draft != null) {
            target = draft;                       // edit the draft head
        } else {
            target = new EvaluationTemplateVersionEntity(templateId,
                    versions.maxVersion(templateId) + 1,
                    payload(scaleMin, scaleMax, competitionId, categoryId, roundId));
            versions.save(target);                // published → new DRAFT v(n+1)
        }
        if (newCriteria != null) {
            criteria.deleteByVersionId(target.getId());
            writeCriteria(target.getId(), newCriteria);
        }
        return new VersionView(target.getId(), templateId, target.getVersionNo(),
                target.getStatus().name(), target.getUpdatedAt().toString());
    }

    /** DRAFT → PUBLISHED (immutable). Requires: criteria present + weights Σ=100. */
    @Transactional
    public VersionView publish(UUID admin, UUID versionId) {
        SystemRoleGuard.requireAdmin();
        var v = requireVersion(versionId);
        if (v.isPublished()) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Already published");
        }
        var list = criteria.findByVersionIdOrderBySortOrderAsc(versionId);
        if (list.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "No criteria");
        }
        var sum = list.stream().map(EvaluationCriterionEntity::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.setScale(2, RoundingMode.HALF_UP).compareTo(new BigDecimal("100.00")) != 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Criterion weights must sum to 100");
        }
        v.publish();
        versions.saveAndFlush(v);
        audit.record("M13", "RUBRIC_PUBLISHED", admin, "user",
                "rubric_version", versionId.toString(), null);
        return new VersionView(v.getId(), v.getTemplateId(), v.getVersionNo(),
                v.getStatus().name(), v.getUpdatedAt().toString());
    }

    // ---------- evaluations (judge) ----------

    /**
     * Judge submits scores for the FINALIZED submission, bound to the round's
     * PUBLISHED rubric version. Scope via M12 (assignment covers comp/cat/round).
     * One per judge+submission+round (DB-09); replay → idempotent return.
     */
    @Transactional
    public EvaluationView submitEvaluation(UUID userId, UUID submissionId,
                                           UUID rubricVersionId, List<ScoreCmd> scoreCmds) {
        var judge = judgeScope.requireJudge(userId);                       // ROLE_JUDGE + ACTIVE
        var s = submissions.submissionView(submissionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!"FINALIZED".equals(s.state())) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Submission not evaluable");
        }
        var window = competition.roundWindow(s.roundId()).orElseThrow();
        if (window.rubricVersionId() != null && !window.rubricVersionId().equals(rubricVersionId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Rubric not configured for round");
        }
        var v = requireVersion(rubricVersionId);
        if (!v.isPublished()) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Rubric not published");
        }
        enforceApplicability(v, s);                                        // payload scope
        var assignment = judgeScope.requireScopedAssignment(judge.judgeId(), s.competitionId(),
                s.categoryId(), s.roundId());

        var rubric = publishedVersion(rubricVersionId).orElseThrow();
        var byCriterion = rubric.criteria().stream()
                .collect(Collectors.toMap(CriterionView::id, c -> c));
        for (var sc : scoreCmds) {
            var c = byCriterion.get(sc.criterionId());
            if (c == null) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown criterion");
            }
            var cap = c.maxScore() != null ? c.maxScore() : BigDecimal.valueOf(rubric.scaleMax());
            if (sc.score() == null || sc.score().compareTo(BigDecimal.ZERO) < 0
                    || sc.score().compareTo(cap) > 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Score out of range");
            }
        }
        var missing = byCriterion.keySet().stream()
                .filter(id -> scoreCmds.stream().noneMatch(sc -> sc.criterionId().equals(id)))
                .toList();
        if (!missing.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Score required per criterion");
        }

        var existing = evaluations
                .findByJudgeIdAndSubmissionIdAndRoundId(judge.judgeId(), submissionId, s.roundId());
        JudgeEvaluationEntity ev;
        if (existing.isPresent()) {
            ev = existing.get();
            if (ev.getStatus() == JudgeEvaluationEntity.Status.SUBMITTED) {
                return toView(ev);                                          // DB-09 replay
            }
            scores.deleteByEvaluationId(ev.getId());
            ev.resubmit();                                                  // REOPENED → resubmit
        } else {
            ev = new JudgeEvaluationEntity(judge.judgeId(), assignment.id(), submissionId,
                    rubricVersionId, s.competitionId(), s.categoryId(), s.roundId());
            evaluations.save(ev);
        }
        for (var sc : scoreCmds) {
            scores.save(new JudgeEvaluationScoreEntity(ev.getId(), sc.criterionId(),
                    sc.score(), sc.comment()));
        }
        evaluations.saveAndFlush(ev);
        audit.record("M13", "EVALUATION_SUBMITTED", userId, "user",
                "judge_evaluation", ev.getId().toString(), null);
        return toView(ev);
    }

    @Transactional(readOnly = true)
    public EvaluationView getEvaluation(UUID userId, UUID evaluationId) {
        var ev = evaluations.findById(evaluationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        var judge = judgeScope.requireJudge(userId);
        if (!ev.getJudgeId().equals(judge.judgeId()) && !SystemRoleGuard.hasRole(null, "ADMIN")) {
            throw new ApiException(ErrorCode.NOT_FOUND);                    // IDOR-safe
        }
        return toView(ev);
    }

    /** Authorized reopen (admin) — clears SUBMITTED so the judge can amend. */
    @Transactional
    public void reopenEvaluation(UUID admin, UUID evaluationId) {
        SystemRoleGuard.requireAdmin();
        var ev = evaluations.findById(evaluationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        ev.reopen();
        evaluations.save(ev);
        audit.record("M13", "EVALUATION_REOPENED", admin, "user",
                "judge_evaluation", evaluationId.toString(), null);
    }

    // ---------- RubricContract (M14) ----------

    @Override @Transactional(readOnly = true)
    public Optional<RubricVersionView> publishedVersion(UUID versionId) {
        return versions.findById(versionId).filter(EvaluationTemplateVersionEntity::isPublished)
                .map(this::toRubricView);
    }

    @Override @Transactional(readOnly = true)
    public Optional<RubricVersionView> resolveForRound(UUID roundId) {
        return competition.roundWindow(roundId)
                .flatMap(w -> Optional.ofNullable(w.rubricVersionId()))
                .flatMap(this::publishedVersion);
    }

    @Override @Transactional(readOnly = true)
    public List<EvaluationDetail> submittedEvaluations(UUID submissionId) {
        return evaluations
                .findBySubmissionIdAndStatus(submissionId, JudgeEvaluationEntity.Status.SUBMITTED)
                .stream()
                .map(e -> new EvaluationDetail(e.getId(), e.getJudgeId(), e.getRubricVersionId(),
                        scores.findByEvaluationId(e.getId()).stream().collect(Collectors.toMap(
                                JudgeEvaluationScoreEntity::getCriterionId,
                                JudgeEvaluationScoreEntity::getScore))))
                .toList();
    }

    // ---------- internals ----------

    private void writeCriteria(UUID versionId, List<CriterionCmd> cmds) {
        int i = 0;
        for (var c : cmds) {
            if (c.name() == null || c.name().isBlank()
                    || c.weight() == null || c.weight().signum() < 0
                    || c.weight().compareTo(new BigDecimal("100")) > 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid criterion");
            }
            criteria.save(new EvaluationCriterionEntity(versionId, c.name(), c.description(),
                    c.weight(), c.maxScore(), i++));
        }
    }

    /** Payload applicability scope must match the submission's comp/cat/round. */
    private void enforceApplicability(EvaluationTemplateVersionEntity v,
                                      SubmissionTruthContract.SubmissionDetails s) {
        var app = readPayload(v).path("applicability");
        requireMatch(app, "competitionId", s.competitionId());
        requireMatch(app, "categoryId", s.categoryId());
        requireMatch(app, "roundId", s.roundId());
    }

    private void requireMatch(JsonNode app, String key, UUID actual) {
        if (app.hasNonNull(key) && !app.get(key).asText().equals(actual.toString())) {
            throw new ApiException(ErrorCode.CROSS_SCOPE_DENIED, "Rubric not applicable");
        }
    }

    private EvaluationTemplateVersionEntity requireVersion(UUID versionId) {
        return versions.findById(versionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private String payload(Integer min, Integer max, UUID compId, UUID catId, UUID roundId) {
        var root = json.createObjectNode();
        var app = root.putObject("applicability");
        if (compId != null) app.put("competitionId", compId.toString());
        if (catId != null) app.put("categoryId", catId.toString());
        if (roundId != null) app.put("roundId", roundId.toString());
        var scale = root.putObject("scoreScale");
        scale.put("min", min == null ? 1 : min);                            // FRS suggests 1–10;
        scale.put("max", max == null ? 10 : max);                           // configurable, not hard-coded
        return root.toString();
    }

    private JsonNode readPayload(EvaluationTemplateVersionEntity v) {
        try {
            return json.readTree(v.getPayload());
        } catch (Exception e) {
            return json.createObjectNode();
        }
    }

    private RubricVersionView toRubricView(EvaluationTemplateVersionEntity v) {
        var p = readPayload(v).path("scoreScale");
        return new RubricVersionView(v.getId(), v.getTemplateId(), v.getVersionNo(), v.getStatus().name(),
                criteria.findByVersionIdOrderBySortOrderAsc(v.getId()).stream()
                        .map(c -> new CriterionView(c.getId(), c.getName(), c.getWeight(),
                                c.getMaxScore(), c.getSortOrder())).toList(),
                p.has("min") ? p.get("min").asInt() : null,
                p.has("max") ? p.get("max").asInt() : null);
    }

    private EvaluationView toView(JudgeEvaluationEntity e) {
        return new EvaluationView(e.getId(), e.getJudgeId(), e.getSubmissionId(),
                e.getRubricVersionId(), e.getStatus().name(),
                e.getSubmittedAt() == null ? null : e.getSubmittedAt().toString());
    }
}
