package com.starmitra.modules.judge.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class JudgeDtos {

    private JudgeDtos() {}

    public record Judge(UUID id, UUID userId, String status) {}
    public record JudgeCreate(@NotNull UUID userId) {}
    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}
    public record JudgePage(List<Judge> items, PageMeta page) {}

    public record Expertise(UUID skillId, @Size(max = 120) String domainLabel, Boolean verified) {}

    public record Assignment(UUID id, UUID judgeId, UUID competitionId, UUID categoryId,
                             UUID roundId, String status) {}
    public record AssignmentCreate(@NotNull UUID competitionId, UUID categoryId, UUID roundId) {}

    public record Submission(UUID id, UUID participantId, UUID competitionId, UUID roundId,
                             String state) {}
    public record SubmissionPage(List<Submission> items, PageMeta page) {}
}
