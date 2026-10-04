package com.starmitra.modules.leaderboard.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LeaderboardProjectionRepository
        extends JpaRepository<LeaderboardProjectionEntity, UUID> {

    List<LeaderboardProjectionEntity> findByCompetitionIdAndCategoryIdAndRoundIdOrderByRankAsc(
            UUID competitionId, UUID categoryId, UUID roundId);

    List<LeaderboardProjectionEntity> findByCompetitionIdAndRoundIdOrderByRankAsc(
            UUID competitionId, UUID roundId);

    void deleteByCompetitionIdAndRoundId(UUID competitionId, UUID roundId);
}
