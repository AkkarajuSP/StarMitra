package com.starmitra.modules.submission.application;

import java.util.UUID;

/**
 * M10-owned submission-truth contract — M11 (vote targets), M12/M14
 * (scorable scope), M15 (progression candidates) consume this.
 */
public interface SubmissionTruthContract {

    /** Submission exists, FINALIZED, and belongs to the given round (voteable target). */
    boolean isFinalizedSubmission(UUID submissionId, UUID roundId);

    /** Submission exists and belongs to the competition. */
    boolean belongsToCompetition(UUID submissionId, UUID competitionId);

    /** Full submission context — M11 derives target/scope from this, never M10 internals. */
    java.util.Optional<SubmissionDetails> submissionView(UUID submissionId);

    record SubmissionDetails(UUID id, UUID participantId, String participantType,
                             UUID competitionId, UUID categoryId, UUID roundId, String state) {}
}
