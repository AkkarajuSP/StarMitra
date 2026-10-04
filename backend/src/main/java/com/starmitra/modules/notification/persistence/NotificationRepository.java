package com.starmitra.modules.notification.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    @Query("select n from NotificationEntity n where n.recipientId=:r " +
           "order by n.createdAt desc, n.id desc")
    List<NotificationEntity> inbox(UUID r, org.springframework.data.domain.Pageable p);

    @Query("select n from NotificationEntity n where n.recipientId=:r and n.state='UNREAD' " +
           "order by n.createdAt desc, n.id desc")
    List<NotificationEntity> unreadInbox(UUID r, org.springframework.data.domain.Pageable p);

    @Query("select count(n) from NotificationEntity n where n.recipientId=:r and n.state='UNREAD'")
    long unreadCount(UUID r);

    List<NotificationEntity> findByRecipientIdAndState(UUID recipientId,
                                                     NotificationEntity.State state);
}
