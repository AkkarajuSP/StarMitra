package com.starmitra.modules.competition.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CompetitionRoundRepository extends JpaRepository<CompetitionRoundEntity, UUID> {
    List<CompetitionRoundEntity> findByCompetitionIdOrderBySequenceAsc(UUID competitionId);
}
