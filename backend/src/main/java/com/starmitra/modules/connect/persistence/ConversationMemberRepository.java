package com.starmitra.modules.connect.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface ConversationMemberRepository
        extends JpaRepository<ConversationMemberEntity, ConversationMemberEntity.Pk> {

    List<ConversationMemberEntity> findByConversationId(UUID conversationId);

    @Query("select m.userId from ConversationMemberEntity m " +
           "where m.conversationId = :conversationId and m.leftAt is null")
    List<UUID> findActiveUserIds(UUID conversationId);
}
