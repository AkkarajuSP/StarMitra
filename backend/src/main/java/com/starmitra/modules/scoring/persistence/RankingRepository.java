package com.starmitra.modules.scoring.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RankingRepository extends JpaRepository<RankingEntity, UUID> {
    List<RankingEntity> findByRoundIdAndSnapshotVersionOrderByRankAsc(UUID roundId, int snapshot);

    @org.springframework.data.jpa.repository.Query(
        "select coalesce(max(r.snapshotVersion),0) from RankingEntity r where r.roundId=:r")
    int maxSnapshot(UUID r);
}
