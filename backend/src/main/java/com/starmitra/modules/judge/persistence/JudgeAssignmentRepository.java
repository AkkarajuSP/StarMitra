package com.starmitra.modules.judge.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JudgeAssignmentRepository extends JpaRepository<JudgeAssignmentEntity, UUID> {

    List<JudgeAssignmentEntity> findByJudgeIdAndStatus(UUID judgeId, String status);

    /** Scope resolution: judge + competition (+ optional category/round match or NULL-wildcard). */
    @Query("select a from JudgeAssignmentEntity a where a.judgeId = :judgeId and a.status = 'ACTIVE' " +
           "and a.revokedAt is null and a.competitionId = :competitionId " +
           "and (a.categoryId is null or a.categoryId = :categoryId) " +
           "and (a.roundId is null or a.roundId = :roundId)")
    List<JudgeAssignmentEntity> resolveScope(UUID judgeId, UUID competitionId, UUID categoryId, UUID roundId);
}
