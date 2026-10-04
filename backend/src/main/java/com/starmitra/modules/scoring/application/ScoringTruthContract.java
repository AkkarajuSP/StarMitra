package com.starmitra.modules.scoring.application;

import java.util.Map;
import java.util.UUID;

/**
 * M14-owned scoring/qualification truth.
 * M15 consumes qualification; M16 consumes rankings.
 */
public interface ScoringTruthContract {

    /** Qualified submissions for a round (M15 progression input). */
    Map<UUID, Boolean> qualificationOf(UUID roundId);

    /** Latest ranking snapshot for a round: submissionId → rank (M16 input). */
    Map<UUID, Integer> latestRanking(UUID roundId);

    /** Whether scoring for the round's latest config is sealed. */
    boolean isSealed(UUID roundId);
}
