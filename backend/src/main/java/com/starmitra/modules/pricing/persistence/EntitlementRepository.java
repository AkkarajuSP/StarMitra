package com.starmitra.modules.pricing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntitlementRepository extends JpaRepository<EntitlementEntity, String> {
}
