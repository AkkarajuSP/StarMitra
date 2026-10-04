package com.starmitra.modules.connect.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    @Query("select coalesce(max(m.sequence), 0) from MessageEntity m where m.conversationId = :convId")
    long maxSequence(@Param("convId") UUID convId);

    @Query("select m from MessageEntity m where m.conversationId = :convId " +
           "and m.sequence > :since order by m.sequence asc")
    List<MessageEntity> findSince(@Param("convId") UUID convId, @Param("since") long since,
                                  Pageable page);
}
