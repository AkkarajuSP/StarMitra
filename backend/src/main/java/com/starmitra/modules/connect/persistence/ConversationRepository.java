package com.starmitra.modules.connect.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    @Query("select c from ConversationEntity c where c.id in " +
           "(select m.conversationId from ConversationMemberEntity m " +
           " where m.userId = :userId and m.leftAt is null) order by c.updatedAt desc, c.id asc")
    List<ConversationEntity> findMemberConversations(@Param("userId") UUID userId, Pageable page);

    /** Serialize message sends per conversation for sequence assignment. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConversationEntity c where c.id = :id")
    Optional<ConversationEntity> findByIdForUpdate(@Param("id") UUID id);
}
