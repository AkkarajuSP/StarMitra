package com.starmitra.modules.judge.application;

import com.starmitra.modules.competition.application.CompetitionStructureContract;
import com.starmitra.modules.identity.application.SystemRoleContract;
import com.starmitra.modules.judge.persistence.*;
import com.starmitra.modules.rubric.application.RubricContract;
import com.starmitra.modules.skill.application.SkillTaxonomyContract;
import com.starmitra.modules.submission.application.SubmissionTruthContract;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.security.SystemRoleGuard;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * M12 — Judge Management. Judge record + expertise (qualification data,
 * never authorization) + scoped assignments (DB-08 inline scope — the judge
 * authz boundary). JUDGE system role ≠ evaluation access — assignments grant
 * scope explicitly. Admin manages judges/assignments.
 */
@Service
public class JudgeService {

    private final JudgeRepository judges;
    private final JudgeExpertiseRepository expertise;
    private final JudgeAssignmentRepository assignments;
    private final JudgeScopeService scope;
    private final CompetitionStructureContract competition;
    private final SubmissionTruthContract submissions;
    private final SkillTaxonomyContract skills;
    private final SystemRoleContract systemRoles;
    private final RubricContract rubrics;
    private final AuditService audit;

    public JudgeService(JudgeRepository judges, JudgeExpertiseRepository expertise,
                        JudgeAssignmentRepository assignments, JudgeScopeService scope,
                        CompetitionStructureContract competition,
                        SubmissionTruthContract submissions, SkillTaxonomyContract skills,
                        SystemRoleContract systemRoles, RubricContract rubrics,
                        AuditService audit) {
        this.judges = judges;
        this.expertise = expertise;
        this.assignments = assignments;
        this.scope = scope;
        this.competition = competition;
        this.submissions = submissions;
        this.skills = skills;
        this.systemRoles = systemRoles;
        this.rubrics = rubrics;
        this.audit = audit;
    }

    public record JudgeView(UUID id, UUID userId, String status) {}
    public record AssignmentView(UUID id, UUID judgeId, UUID competitionId, UUID categoryId,
                                 UUID roundId, String status) {}
    public record ExpertiseCmd(UUID skillId, String domainLabel, Boolean verified) {}
    public record AssignmentCmd(UUID competitionId, UUID categoryId, UUID roundId) {}

    // ---------- admin: judges ----------

    @Transactional
    public JudgeView createJudge(UUID admin, UUID userId) {
        SystemRoleGuard.requireAdmin();
        var existing = judges.findByUserId(userId);
        if (existing.isPresent()) return toJudge(existing.get());   // idempotent
        var j = judges.saveAndFlush(new JudgeEntity(userId));
        systemRoles.grantSystemRole(userId, "JUDGE");   // role ≠ evaluation access
        audit.record("M12", "JUDGE_CREATED", admin, "user", "judge", j.getId().toString(), null);
        return toJudge(j);
    }

    @Transactional(readOnly = true)
    public List<JudgeView> listJudges(int offset, int limit) {
        return judges.findAll(org.springframework.data.domain.PageRequest.of(
                        Math.max(0, offset / Math.max(1, limit)), Math.max(1, Math.min(limit, 100))))
                .stream().map(this::toJudge).toList();
    }

    /** Expertise = qualification data; skill refs M03-validated. Replaces set. */
    @Transactional
    public void setExpertise(UUID admin, UUID judgeId, List<ExpertiseCmd> rows) {
        SystemRoleGuard.requireAdmin();
        requireJudge(judgeId);
        expertise.deleteByJudgeId(judgeId);
        for (var e : rows) {
            if (e.skillId() != null && !skills.isActiveSkill(e.skillId())) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Skill not found or inactive");
            }
            expertise.save(new JudgeExpertiseEntity(judgeId, e.skillId(), e.domainLabel(),
                    e.verified() != null && e.verified()));
        }
    }

    // ---------- admin: assignments (the authz boundary) ----------

    /** Scope validated through M09 — category/round must belong to the comp. */
    @Transactional
    public AssignmentView assign(UUID admin, UUID judgeId, AssignmentCmd cmd) {
        SystemRoleGuard.requireAdmin();
        requireJudge(judgeId);
        if (cmd.categoryId() != null
                && !competition.categoryBelongsTo(cmd.competitionId(), cmd.categoryId())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Category not in competition");
        }
        if (cmd.roundId() != null
                && !competition.roundBelongsTo(cmd.competitionId(), cmd.roundId())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Round not in competition");
        }
        if (assignments.existsByScope(judgeId, cmd.competitionId(), cmd.categoryId(), cmd.roundId())) {
            throw new ApiException(ErrorCode.CONFLICT, "Scope already assigned");
        }
        try {
            var a = assignments.saveAndFlush(new JudgeAssignmentEntity(
                    judgeId, cmd.competitionId(), cmd.categoryId(), cmd.roundId()));
            audit.record("M12", "JUDGE_ASSIGNED", admin, "user",
                    "judge_assignment", a.getId().toString(), null);
            return toAssignment(a);
        } catch (DataIntegrityViolationException dup) {
            throw new ApiException(ErrorCode.CONFLICT, "Scope already assigned");
        }
    }

    @Transactional
    public void revokeAssignment(UUID admin, UUID assignmentId) {
        SystemRoleGuard.requireAdmin();
        var a = assignments.findById(assignmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        a.revoke();
        assignments.save(a);
        audit.record("M12", "JUDGE_ASSIGNMENT_REVOKED", admin, "user",
                "judge_assignment", assignmentId.toString(), null);
    }

    // ---------- judge self (M20 surface lives here as contract ops) ----------

    @Transactional(readOnly = true)
    public JudgeView myProfile(UUID userId) {
        var j = scope.requireJudge(userId);
        return toJudge(judges.findById(j.judgeId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<AssignmentView> myAssignments(UUID userId) {
        var j = scope.requireJudge(userId);
        return scope.activeAssignments(j.judgeId()).stream()
                .map(a -> new AssignmentView(a.id(), j.judgeId(), a.competitionId(),
                        a.categoryId(), a.roundId(), a.status()))
                .toList();
    }

    /**
     * Scope-resolved submissions — server-side: union of FINALIZED submissions
     * covered by the judge's ACTIVE assignments (null category/round = wildcard
     * within that assignment). Judge NEVER sees unassigned scope.
     */
    public record ScopedSubmission(UUID id, UUID participantId, UUID competitionId,
                                   UUID roundId, String state, List<UUID> mediaIds) {}

    @Transactional(readOnly = true)
    public List<ScopedSubmission> myScopedSubmissions(UUID userId, UUID competitionId) {
        var j = scope.requireJudge(userId);
        return scope.activeAssignments(j.judgeId()).stream()
                .filter(a -> competitionId == null || a.competitionId().equals(competitionId))
                .flatMap(a -> submissions.finalizedInScope(
                        a.competitionId(), a.categoryId(), a.roundId()).stream())
                .distinct()
                .map(id -> submissions.submissionView(id).orElseThrow())
                .map(d -> new ScopedSubmission(d.id(), d.participantId(), d.competitionId(),
                        d.roundId(), d.state(), submissions.mediaIdsOf(d.id())))
                .toList();
    }

    /**
     * M20 rubric seam — contextId is the ROUND id. Scope-checked through M12
     * (judge must hold an assignment covering that round's competition/round),
     * resolved through M13's published-version contract — never frontend-derived.
     */
    @Transactional(readOnly = true)
    public RubricContract.RubricVersionView myRubric(UUID userId, UUID roundId) {
        var w = competition.roundWindow(roundId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        scope.requireScope(userId, w.competitionId(), null, roundId);
        return rubrics.resolveForRound(roundId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND,
                        "No published rubric for context"));
    }

    // ---------- internals ----------

    private JudgeEntity requireJudge(UUID judgeId) {
        return judges.findById(judgeId)
                .filter(j -> "ACTIVE".equals(j.getStatus()))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private JudgeView toJudge(JudgeEntity j) {
        return new JudgeView(j.getId(), j.getUserId(), j.getStatus());
    }

    private AssignmentView toAssignment(JudgeAssignmentEntity a) {
        return new AssignmentView(a.getId(), a.getJudgeId(), a.getCompetitionId(),
                a.getCategoryId(), a.getRoundId(), "ACTIVE");
    }
}
