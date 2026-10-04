package com.starmitra.modules.scoring.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinalScoreRepository extends JpaRepository<FinalScoreEntity, UUID> {
    List<FinalScoreEntity> findByRoundId(UUID roundId);

    List<FinalScoreEntity> findByRoundIdAndScoringConfigId(UUID roundId, UUID configId);

    @Query("select coalesce(max(f.scoreVersion),0) from FinalScoreEntity f " +
           "where f.submissionId=:s and f.roundId=:r and f.scoringConfigId=:c")
    int maxScoreVersion(UUID s, UUID r, UUID c);

    Optional<FinalScoreEntity> findBySubmissionIdAndRoundId(UUID submissionId, UUID roundId);
}
