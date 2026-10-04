package com.starmitra.modules.rubric.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class RubricDtos {

    private RubricDtos() {}

    public record Criterion(@NotBlank @Size(max = 160) String name, String description,
                            @NotNull BigDecimal weight, BigDecimal maxScore) {}

    /** TemplateCreate per contract: name + criteria; scope/scale carried via fields below. */
    public record TemplateCreate(@NotBlank @Size(max = 160) String name,
                                 @Valid List<Criterion> criteria,
                                 Integer scaleMin, Integer scaleMax,
                                 UUID competitionId, UUID categoryId, UUID roundId) {}

    public record Rubric(UUID id, String name, String status) {}
    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}
    public record RubricPage(List<Rubric> items, PageMeta page) {}

    public record RubricVersion(UUID id, UUID templateId, Integer versionNo, String status,
                                List<Object> criteria) {}

    public record ScoreItem(@NotNull UUID criterionId, @NotNull BigDecimal score, String comment) {}
    public record EvaluationSubmit(@NotNull UUID submissionId, @NotNull UUID rubricVersionId,
                                   @NotNull @Size(min = 1) @Valid List<ScoreItem> scores) {}

    public record Evaluation(UUID id, UUID judgeId, UUID submissionId, UUID rubricVersionId,
                             String status, String submittedAt) {}
}
