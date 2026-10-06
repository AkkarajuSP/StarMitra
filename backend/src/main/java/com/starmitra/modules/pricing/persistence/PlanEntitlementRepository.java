package com.starmitra.modules.pricing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PlanEntitlementRepository
        extends JpaRepository<PlanEntitlementEntity, PlanEntitlementEntity.Pk> {

    List<PlanEntitlementEntity> findByPlanId(UUID planId);
}
