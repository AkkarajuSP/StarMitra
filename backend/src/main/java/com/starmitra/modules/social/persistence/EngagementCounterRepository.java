package com.starmitra.modules.social.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EngagementCounterRepository extends JpaRepository<EngagementCounterEntity, EngagementCounterEntity.Pk> {
}
