package com.starmitra.modules.scoring.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JudgeScoreAggRepository extends JpaRepository<JudgeScoreAggEntity, UUID> {
    List<JudgeScoreAggEntity> findByRoundId(UUID roundId);
}
