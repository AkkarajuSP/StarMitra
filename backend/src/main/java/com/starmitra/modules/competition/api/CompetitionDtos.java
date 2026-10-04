package com.starmitra.modules.competition.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CompetitionDtos {

    private CompetitionDtos() {}

    public record Competition(UUID id, String title, String configStatus,
                              String participationStatus, String roundState, int version) {}

    public record CompetitionCreate(@NotBlank @Size(max = 200) String title,
                                    @Size(max = 4000) String description) {}

    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}
    public record CompetitionPage(List<Competition> items, PageMeta page) {}
    public record ParticipantPage(List<Participant> items, PageMeta page) {}

    public record Category(UUID id, String name) {}
    public record CategoryCreate(@NotBlank @Size(max = 120) String name,
                                 @Size(max = 4000) String description,
                                 List<UUID> skillIds) {}

    public record Round(UUID id, int sequence, String name, String startAt, String endAt,
                        UUID voteConfigId, UUID rubricVersionId) {}
    public record RoundCreate(@NotNull Integer sequence, @Size(max = 120) String name,
                              String startAt, String endAt, UUID voteConfigId,
                              UUID rubricVersionId, UUID progressionConfigId) {}

    public record EligibilityRuleCreate(@NotBlank @Size(max = 40) String ruleType,
                                        Map<String, Object> ruleParams) {}

    public record SubmissionConfig(@NotNull Map<String, Object> config) {}

    public record Participant(UUID id, String participantType, UUID userId,
                              UUID projectId, String status) {}
    public record ParticipantRegister(@NotNull @Pattern(regexp = "USER|PROJECT") String participantType,
                                      UUID categoryId, UUID projectId) {}
}
