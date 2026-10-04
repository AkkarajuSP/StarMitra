package com.starmitra.modules.leaderboard.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaderboardPublicationRepository
        extends JpaRepository<LeaderboardPublicationEntity, UUID> {

    Optional<LeaderboardPublicationEntity> findByCompetitionIdAndCategoryIdAndRoundId(
            UUID competitionId, UUID categoryId, UUID roundId);

    @Query("select p from LeaderboardPublicationEntity p where p.competitionId=:c and p.roundId=:r " +
           "order by p.updatedAt desc")
    List<LeaderboardPublicationEntity> forContext(UUID c, UUID r);
}
