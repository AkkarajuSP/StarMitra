package com.starmitra.modules.skill.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface UserTalentSkillRepository extends JpaRepository<UserTalentSkillEntity, UserTalentSkillEntity.Pk> {

    List<UserTalentSkillEntity> findByUserId(UUID userId);

    /** M02 read-contract projection — join skill + proficiency, no entity leak. */
    @Query("select s.id, s.name, p.code from UserTalentSkillEntity u " +
           "join TalentSkillEntity s on s.id = u.skillId " +
           "left join SkillProficiencyEntity p on p.id = u.proficiencyId " +
           "where u.userId = :userId and s.status = 'ACTIVE' order by s.name")
    List<Object[]> findSkillViewsByUserId(UUID userId);
}
