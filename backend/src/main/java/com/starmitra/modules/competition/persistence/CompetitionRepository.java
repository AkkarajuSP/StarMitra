package com.starmitra.modules.competition.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface CompetitionRepository extends JpaRepository<CompetitionEntity, UUID> {

    @Query("select c from CompetitionEntity c " +
           "where (:status is null or cast(c.participationStatus as string) = :status) " +
           "order by c.createdAt desc, c.id asc")
    List<CompetitionEntity> findListing(@Param("status") String status, Pageable page);
}
