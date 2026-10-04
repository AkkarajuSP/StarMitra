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

    /** Exact-scope existence (NULL-equal, matching uq_ja_scope NULLS NOT DISTINCT). */
    @Query("select count(a) > 0 from JudgeAssignmentEntity a where a.judgeId = :judgeId " +
           "and a.competitionId = :competitionId " +
           "and (:categoryId is null and a.categoryId is null or a.categoryId = :categoryId) " +
           "and (:roundId is null and a.roundId is null or a.roundId = :roundId)")
    boolean existsByScope(UUID judgeId, UUID competitionId, UUID categoryId, UUID roundId);
}
