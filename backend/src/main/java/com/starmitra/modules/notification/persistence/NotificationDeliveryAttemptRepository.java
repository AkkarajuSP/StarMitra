package com.starmitra.modules.notification.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationDeliveryAttemptRepository
        extends JpaRepository<NotificationDeliveryAttemptEntity, UUID> {}
