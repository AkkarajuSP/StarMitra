package com.starmitra.modules.social.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface LikeRepository extends JpaRepository<LikeEntity, UUID> {

    Optional<LikeEntity> findByUserIdAndTargetTypeAndTargetId(UUID userId, String targetType, UUID targetId);
}
