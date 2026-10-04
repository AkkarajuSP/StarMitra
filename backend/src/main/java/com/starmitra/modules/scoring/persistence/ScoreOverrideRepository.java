package com.starmitra.modules.scoring.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ScoreOverrideRepository extends JpaRepository<ScoreOverrideEntity, UUID> {
    List<ScoreOverrideEntity> findByFinalScoreId(UUID finalScoreId);
}
