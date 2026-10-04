package com.starmitra.modules.submission.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SubmissionDtos {

    private SubmissionDtos() {}

    public record Submission(UUID id, UUID participantId, UUID competitionId, UUID roundId,
                             String state, String submittedAt, String finalizedAt) {}

    public record SubmissionCreate(@NotNull UUID participantId, @NotNull UUID roundId) {}

    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}
    public record SubmissionPage(List<Submission> items, PageMeta page) {}

    public record ContributorRef(@NotNull UUID memberRef, UUID roleRef) {}
    public record ContributorDeclare(@NotNull @Valid List<ContributorRef> contributors) {}

    public record AssetLink(@NotNull UUID mediaId, String role, Integer sortOrder) {}
}
