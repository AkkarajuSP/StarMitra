package com.starmitra.modules.pricing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, UUID> {

    /** uq_us_current guarantees at most one row. */
    Optional<UserSubscriptionEntity> findByUserIdAndStatus(UUID userId,
                                                         UserSubscriptionEntity.Status status);

    List<UserSubscriptionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
