package com.starmitra.modules.social.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtAsc(
            String targetType, UUID targetId, CommentEntity.Status status, Pageable page);
}
