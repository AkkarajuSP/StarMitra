package com.starmitra.modules.scoring.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface QualificationResultRepository extends JpaRepository<QualificationResultEntity, UUID> {
    List<QualificationResultEntity> findByRoundId(UUID roundId);
}
