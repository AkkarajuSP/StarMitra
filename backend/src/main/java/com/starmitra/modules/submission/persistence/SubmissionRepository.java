package com.starmitra.modules.submission.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<SubmissionEntity, UUID> {

    boolean existsByParticipantIdAndRoundIdAndStateNot(
            UUID participantId, UUID roundId, SubmissionEntity.State state);

    @Query("select s.id from SubmissionEntity s where s.competitionId = :compId " +
           "and s.state = 'FINALIZED' " +
           "and (:categoryId is null or s.categoryId = :categoryId) " +
           "and (:roundId is null or s.roundId = :roundId)")
    List<UUID> findFinalizedIds(@Param("compId") UUID compId,
                                @Param("categoryId") UUID categoryId,
                                @Param("roundId") UUID roundId);

    @Query("select s from SubmissionEntity s where s.competitionId = :compId " +
           "and (:roundId is null or s.roundId = :roundId) " +
           "and (:categoryId is null or s.categoryId = :categoryId) " +
           "order by s.createdAt asc, s.id asc")
    List<SubmissionEntity> findListing(@Param("compId") UUID compId,
                                       @Param("roundId") UUID roundId,
                                       @Param("categoryId") UUID categoryId,
                                       Pageable page);
}
