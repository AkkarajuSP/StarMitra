package com.starmitra.modules.room.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RequiredSkillRepository extends JpaRepository<RequiredSkillEntity, RequiredSkillEntity.Pk> {
    List<RequiredSkillEntity> findByRoomId(UUID roomId);
}
