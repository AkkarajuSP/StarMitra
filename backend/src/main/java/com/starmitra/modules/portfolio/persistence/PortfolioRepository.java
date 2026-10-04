package com.starmitra.modules.portfolio.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<PortfolioEntity, UUID> {
    Optional<PortfolioEntity> findByUserId(UUID userId);
}
