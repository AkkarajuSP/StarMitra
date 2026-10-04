package com.starmitra.modules.voting.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public final class VoteDtos {

    private VoteDtos() {}

    public record VoteCast(@NotNull UUID submissionId,
                           @NotBlank @Size(max = 128) String clientMsgId) {}

    public record Vote(UUID id, UUID submissionId, String targetType, String castAt) {}

    public record VoteCount(UUID submissionId, long count, boolean derived) {}

    public record VoteConfigCreate(@NotBlank @Size(max = 80) String seriesKey,
                                   @NotNull Map<String, Object> payload) {}
}
