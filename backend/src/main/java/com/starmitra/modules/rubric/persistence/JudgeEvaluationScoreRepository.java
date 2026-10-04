package com.starmitra.modules.rubric.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface JudgeEvaluationScoreRepository
        extends JpaRepository<JudgeEvaluationScoreEntity, JudgeEvaluationScoreEntity.Pk> {

    List<JudgeEvaluationScoreEntity> findByEvaluationId(UUID evaluationId);

    @Modifying
    @Query("delete from JudgeEvaluationScoreEntity s where s.evaluationId = :evaluationId")
    void deleteByEvaluationId(UUID evaluationId);
}
