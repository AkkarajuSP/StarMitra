package com.starmitra.modules.leaderboard.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LeaderboardSnapshotRepository
        extends JpaRepository<LeaderboardSnapshotEntity, UUID> {}
