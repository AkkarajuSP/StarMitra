package com.starmitra.modules.competition.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionCategorySkillRepository
        extends JpaRepository<CompetitionCategorySkillEntity, CompetitionCategorySkillEntity.Pk> {
}
