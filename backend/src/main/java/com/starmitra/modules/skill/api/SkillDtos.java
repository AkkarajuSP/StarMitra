package com.starmitra.modules.skill.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class SkillDtos {

    private SkillDtos() {}

    public record Skill(UUID id, String name, String status, UUID parentSkillId) {}

    public record SkillCreate(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 2000) String description,
            UUID parentSkillId) {}

    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}

    public record SkillPage(List<Skill> items, PageMeta page) {}

    public record UserSkill(UUID skillId, String name, String proficiency) {}

    public record UserSkillUpdate(String proficiency) {}
}
