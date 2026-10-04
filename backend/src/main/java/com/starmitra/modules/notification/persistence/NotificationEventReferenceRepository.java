package com.starmitra.modules.notification.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationEventReferenceRepository
        extends JpaRepository<NotificationEventReferenceEntity, UUID> {

    boolean existsBySourceModuleAndEventTypeAndSourceIdAndEventVersion(
            String module, String eventType, String sourceId, String eventVersion);
}
