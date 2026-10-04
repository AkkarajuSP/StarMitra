package com.starmitra.modules.competition.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EligibilityRuleRepository extends JpaRepository<EligibilityRuleEntity, UUID> {
    List<EligibilityRuleEntity> findByCompetitionId(UUID competitionId);
}
