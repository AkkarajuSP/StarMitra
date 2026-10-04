package com.starmitra.modules.competition.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CompetitionCategoryRepository extends JpaRepository<CompetitionCategoryEntity, UUID> {
    List<CompetitionCategoryEntity> findByCompetitionId(UUID competitionId);
}
