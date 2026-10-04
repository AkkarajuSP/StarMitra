package com.starmitra.modules.room.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMemberEntity, ProjectMemberEntity.Pk> {
    List<ProjectMemberEntity> findByRoomId(UUID roomId);
}
