package com.starmitra.modules.profile.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class ProfileDtos {

    private ProfileDtos() {}

    public record SkillView(UUID skillId, String name, String proficiency) {}

    public record Profile(UUID userId, String displayName, String bio, String location,
                          UUID avatarMediaId, String visibilityState, List<SkillView> skills) {}

    public record ProfilePublic(UUID userId, String displayName, String bio,
                                UUID avatarMediaId, List<SkillView> skills) {}

    public record ProfileUpdate(
            @Size(max = 120) String displayName,
            @Size(max = 4000) String bio,
            @Size(max = 120) String location,
            UUID avatarMediaId,
            @Pattern(regexp = "PUBLIC|FOLLOWERS|COLLABORATION_ONLY|PRIVATE") String visibilityState) {}
}
