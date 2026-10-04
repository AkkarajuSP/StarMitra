package com.starmitra.modules.competition.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompetitionParticipantRepository extends JpaRepository<CompetitionParticipantEntity, UUID> {

    List<CompetitionParticipantEntity> findByCompetitionIdOrderByRegisteredAtAsc(
            UUID competitionId, Pageable page);

    Optional<CompetitionParticipantEntity> findByCompetitionIdAndUserIdAndStatus(
            UUID competitionId, UUID userId, CompetitionParticipantEntity.Status status);
}
