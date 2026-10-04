package com.starmitra.modules.portfolio.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioItemMediaRepository
        extends JpaRepository<PortfolioItemMediaEntity, PortfolioItemMediaEntity.Pk> {
}
