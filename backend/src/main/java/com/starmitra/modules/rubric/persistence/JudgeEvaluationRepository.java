package com.starmitra.modules.rubric.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface JudgeEvaluationRepository extends JpaRepository<JudgeEvaluationEntity, UUID> {

    Optional<JudgeEvaluationEntity> findByJudgeIdAndSubmissionIdAndRoundId(
            UUID judgeId, UUID submissionId, UUID roundId);
}
