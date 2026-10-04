package com.starmitra.modules.portfolio.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PortfolioItemContributionRepository
        extends JpaRepository<PortfolioItemContributionEntity, UUID> {
}
