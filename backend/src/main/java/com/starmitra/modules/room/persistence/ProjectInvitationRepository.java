package com.starmitra.modules.room.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitationEntity, UUID> {

    List<ProjectInvitationEntity> findByInviteeIdAndStatus(
            UUID inviteeId, ProjectInvitationEntity.Status status);

    boolean existsByRoomIdAndInviteeIdAndStatus(UUID roomId, UUID inviteeId,
                                                ProjectInvitationEntity.Status status);
}
