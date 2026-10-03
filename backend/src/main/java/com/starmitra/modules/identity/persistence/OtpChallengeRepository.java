package com.starmitra.modules.identity.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallengeEntity, UUID> {

    @Query("select c from OtpChallengeEntity c where c.userId = :userId and c.consumedAt is null " +
           "order by c.createdAt desc limit 1")
    Optional<OtpChallengeEntity> findLatestActive(UUID userId);

    @Modifying
    @Query("update OtpChallengeEntity c set c.consumedAt = CURRENT_TIMESTAMP " +
           "where c.userId = :userId and c.consumedAt is null")
    int supersedeAllForUser(UUID userId);

    @Query("select count(c) from OtpChallengeEntity c where c.userId = :userId and c.createdAt > :since")
    long countRecentRequests(UUID userId, OffsetDateTime since);
}
