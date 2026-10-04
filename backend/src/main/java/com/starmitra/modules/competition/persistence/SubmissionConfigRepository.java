package com.starmitra.modules.competition.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionConfigRepository extends JpaRepository<SubmissionConfigEntity, UUID> {
    Optional<SubmissionConfigEntity> findByCompetitionId(UUID competitionId);
}
