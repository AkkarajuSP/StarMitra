package com.starmitra.modules.judge.application;

import com.starmitra.modules.judge.persistence.JudgeAssignmentRepository;
import com.starmitra.modules.judge.persistence.JudgeRepository;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.security.SystemRoleGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Judge authorization chain — the ONLY arbiter of judge access:
 *
 *   SystemRole(JUDGE)
 *     → Judge (active)
 *       → JudgeAssignment (ACTIVE, not revoked)
 *         → competition (+ optional category/round scope match)
 *           → permitted evidence access
 *
 * TalentSkill / JudgeExpertise / category-skill matches NEVER grant access.
 * This is the contract other modules call — they never touch judge tables.
 */
@Service
public class JudgeScopeService {

    private final JudgeRepository judges;
    private final JudgeAssignmentRepository assignments;

    public JudgeScopeService(JudgeRepository judges, JudgeAssignmentRepository assignments) {
        this.judges = judges;
        this.assignments = assignments;
    }

    /** Cross-module contract views — portals never see persistence entities. */
    public record JudgeView(UUID judgeId, String status) {}
    public record AssignmentView(UUID id, UUID competitionId, UUID categoryId, UUID roundId, String status) {}

    /** Resolve judge for a user or throw JUDGE_FORBIDDEN. */
    @Transactional(readOnly = true)
    public JudgeView requireJudge(UUID userId) {
        SystemRoleGuard.requireJudge(userId);
        return judges.findByUserId(userId)
                .filter(j -> "ACTIVE".equals(j.getStatus()))
                .map(j -> new JudgeView(j.getId(), j.getStatus()))
                .orElseThrow(() -> new ApiException(ErrorCode.JUDGE_FORBIDDEN));
    }

    /** Judge id for a user — used by M12-internal flows that need the entity key. */
    @Transactional(readOnly = true)
    public UUID requireJudgeId(UUID userId) {
        return requireJudge(userId).judgeId();
    }

    /** True when an ACTIVE assignment covers competition(+category,+round). */
    @Transactional(readOnly = true)
    public boolean coversScope(UUID judgeId, UUID competitionId, UUID categoryId, UUID roundId) {
        return !assignments.resolveScope(judgeId, competitionId, categoryId, roundId).isEmpty();
    }

    /** Throw CROSS_SCOPE_DENIED unless scope covers the target. */
    @Transactional(readOnly = true)
    public void requireScope(UUID userId, UUID competitionId, UUID categoryId, UUID roundId) {
        if (!coversScope(requireJudgeId(userId), competitionId, categoryId, roundId)) {
            throw new ApiException(ErrorCode.CROSS_SCOPE_DENIED);
        }
    }

    @Transactional(readOnly = true)
    public List<AssignmentView> activeAssignments(UUID judgeId) {
        return assignments.findByJudgeIdAndStatus(judgeId, "ACTIVE").stream()
                .map(a -> new AssignmentView(a.getId(), a.getCompetitionId(), a.getCategoryId(), a.getRoundId(), "ACTIVE"))
                .toList();
    }
}
