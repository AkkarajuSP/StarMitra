package com.starmitra.modules.social.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<FollowEntity, FollowEntity.Pk> {

    List<FollowEntity> findByFollowerIdOrderByCreatedAtDesc(UUID followerId, Pageable page);

    @Query("select f.followeeId from FollowEntity f where f.followerId = :followerId")
    Set<UUID> findFolloweeIds(UUID followerId);
}
