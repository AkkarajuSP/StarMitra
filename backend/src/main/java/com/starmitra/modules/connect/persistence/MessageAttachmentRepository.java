package com.starmitra.modules.connect.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachmentEntity, UUID> {

    @Query("select a.mediaId from MessageAttachmentEntity a where a.messageId = :messageId order by a.sortOrder")
    List<UUID> findMediaIds(UUID messageId);
}
