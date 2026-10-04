package com.starmitra.modules.room.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProjectContributionRoleRepository extends JpaRepository<ProjectContributionRoleEntity, UUID> {
    List<ProjectContributionRoleEntity> findByRoomId(UUID roomId);
}
