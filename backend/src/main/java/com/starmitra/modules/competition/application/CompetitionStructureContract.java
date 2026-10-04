package com.starmitra.modules.competition.application;

import java.util.UUID;

/**
 * M09-owned structure contract — M10–M16 consume competition truth
 * without touching competition persistence.
 */
public interface CompetitionStructureContract {

    /** Round exists and belongs to the competition. */
    boolean roundBelongsTo(UUID competitionId, UUID roundId);

    /** Category exists and belongs to the competition. */
    boolean categoryBelongsTo(UUID competitionId, UUID categoryId);

    /** Participant exists, ACTIVE, and belongs to the competition. */
    boolean isActiveParticipant(UUID competitionId, UUID participantId);

    /** Competition exists and participation is OPEN (submission-eligible). */
    boolean isOpenForParticipation(UUID competitionId);

    /** Participant's full context for downstream modules (M10 derives comp/category). */
    java.util.Optional<ParticipantDetails> participantView(UUID participantId);

    /** Round's time window — for M10 deadline enforcement. */
    java.util.Optional<RoundWindow> roundWindow(UUID roundId);

    record ParticipantDetails(UUID id, UUID competitionId, UUID categoryId, String type,
                              UUID userId, UUID projectId, String status) {}
    record RoundWindow(UUID id, UUID competitionId, java.time.OffsetDateTime startAt,
                       java.time.OffsetDateTime endAt, UUID voteConfigId, UUID rubricVersionId) {}
}
