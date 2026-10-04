package com.starmitra.modules.progression.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgressionRecordRepository extends JpaRepository<ProgressionRecordEntity, UUID> {
    List<ProgressionRecordEntity> findByRoundId(UUID roundId);

    Optional<ProgressionRecordEntity> findBySubmissionIdAndRoundId(UUID submissionId, UUID roundId);

    boolean existsByRoundId(UUID roundId);
}
