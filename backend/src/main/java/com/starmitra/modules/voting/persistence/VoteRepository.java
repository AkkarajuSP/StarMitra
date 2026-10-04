package com.starmitra.modules.voting.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<VoteEntity, UUID> {

    Optional<VoteEntity> findByVoterIdAndSubmissionId(UUID voterId, UUID submissionId);

    Optional<VoteEntity> findByClientMsgId(String clientMsgId);

    long countByVoterIdAndRoundId(UUID voterId, UUID roundId);

    long countBySubmissionId(UUID submissionId);

    /** M14 seam — per-submission vote counts for a round. */
    @Query("select v.submissionId as submissionId, count(v) as count from VoteEntity v " +
           "where v.roundId = :roundId group by v.submissionId")
    List<SubmissionCount> countsBySubmission(UUID roundId);

    interface SubmissionCount { UUID getSubmissionId(); Long getCount(); }
}
