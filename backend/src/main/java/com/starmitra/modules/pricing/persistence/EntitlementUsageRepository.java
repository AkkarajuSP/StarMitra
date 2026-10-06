package com.starmitra.modules.pricing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EntitlementUsageRepository
        extends JpaRepository<EntitlementUsageEntity, EntitlementUsageEntity.Pk> {
}
