package com.starmitra.modules.progression.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** M15-owned progression truth — M16 leaderboard/portal consumes status+records. */
public interface ProgressionTruthContract {

    /** Competition's current M09-owned round_state. */
    String roundStateOf(UUID competitionId);

    /** Progression decisions for a round: submissionId → outcome. */
    Map<UUID, String> outcomesOf(UUID roundId);

    /** ADVANCED submissions from a source round to a target round. */
    List<UUID> advancedFrom(UUID roundId);
}
