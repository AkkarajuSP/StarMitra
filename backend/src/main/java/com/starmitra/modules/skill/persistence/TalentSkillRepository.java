package com.starmitra.modules.skill.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TalentSkillRepository extends JpaRepository<TalentSkillEntity, UUID> {

    Optional<TalentSkillEntity> findByName(String name);

    @Query("select s from TalentSkillEntity s where s.status = 'ACTIVE' " +
           "and (:afterId is null or s.id > :afterId) order by s.id")
    List<TalentSkillEntity> findActiveAfter(@Param("afterId") UUID afterId,
                                            org.springframework.data.domain.Pageable page);
}
