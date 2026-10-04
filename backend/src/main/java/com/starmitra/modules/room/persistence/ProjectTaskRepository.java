package com.starmitra.modules.room.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProjectTaskRepository extends JpaRepository<ProjectTaskEntity, UUID> {
    List<ProjectTaskEntity> findByRoomId(UUID roomId);
}
