package com.starmitra.modules.notification.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository
        extends JpaRepository<NotificationPreferenceEntity, NotificationPreferenceEntity.Pk> {

    List<NotificationPreferenceEntity> findByUserId(UUID userId);

    Optional<NotificationPreferenceEntity> findByUserIdAndTypeAndChannel(
            UUID userId, String type, String channel);
}
