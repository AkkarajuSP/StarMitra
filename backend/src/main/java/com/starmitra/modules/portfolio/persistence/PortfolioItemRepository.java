package com.starmitra.modules.portfolio.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItemEntity, UUID> {

    List<PortfolioItemEntity> findByPortfolioIdOrderBySortOrderAscCreatedAtAsc(UUID portfolioId);

    @Query("select m.mediaId from PortfolioItemMediaEntity m where m.itemId = :itemId order by m.sortOrder")
    List<UUID> findMediaIds(UUID itemId);
}
