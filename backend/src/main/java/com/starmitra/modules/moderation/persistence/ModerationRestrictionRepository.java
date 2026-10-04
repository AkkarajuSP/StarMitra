package com.starmitra.modules.moderation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ModerationRestrictionRepository
        extends JpaRepository<ModerationRestrictionEntity, UUID> {

    List<ModerationRestrictionEntity> findByTargetTypeAndTargetIdAndStatus(
            String targetType, UUID targetId, ModerationRestrictionEntity.Status status);
}
