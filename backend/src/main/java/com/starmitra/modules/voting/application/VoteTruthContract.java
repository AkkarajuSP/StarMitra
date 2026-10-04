package com.starmitra.modules.voting.application;

import java.util.Map;
import java.util.UUID;

/** M11-owned vote-truth contract — M14 scoring/ranking consumes this. */
public interface VoteTruthContract {

    /** Derived count for one submission (rebuildable). */
    long countForSubmission(UUID submissionId);

    /** Per-submission vote counts for a round — M14 input. */
    Map<UUID, Long> countsBySubmission(UUID roundId);
}
