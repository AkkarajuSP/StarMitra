package com.starmitra.modules.notification.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository
        extends JpaRepository<NotificationTemplateEntity, UUID> {

    @Query("select t from NotificationTemplateEntity t where t.code=:code and t.channel='IN_APP' " +
           "and t.status='PUBLISHED' and t.locale is null order by t.versionNo desc")
    java.util.List<NotificationTemplateEntity> latestFor(String code);
}
