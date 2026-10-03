package com.starmitra.modules.skill.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SkillProficiencyRepository extends JpaRepository<SkillProficiencyEntity, UUID> {
    Optional<SkillProficiencyEntity> findByCode(String code);
}
