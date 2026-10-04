package com.starmitra.modules.connect.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface MessageReceiptRepository
        extends JpaRepository<MessageReceiptEntity, MessageReceiptEntity.Pk> {

    /** markRead: upgrade DELIVERED→READ, insert READ where absent, up to sequence. */
    @Modifying
    @Query(value = "insert into message_receipts (message_id, user_id, status, updated_at) " +
            "select m.id, :userId, 'READ', now() from messages m " +
            "where m.conversation_id = :convId and m.sequence <= :seq and m.sender_id <> :userId " +
            "on conflict (message_id, user_id) do update set status='READ', updated_at=now()",
            nativeQuery = true)
    void markReadUpTo(@Param("convId") UUID convId, @Param("seq") long seq, @Param("userId") UUID userId);
}
