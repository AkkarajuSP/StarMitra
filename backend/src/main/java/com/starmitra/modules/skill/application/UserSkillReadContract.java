package com.starmitra.modules.skill.application;

import java.util.List;
import java.util.UUID;

/**
 * M03-owned read contract — the ONLY way other modules consume a user's
 * talent skills. Never grants authorization; skills are not permissions.
 */
public interface UserSkillReadContract {

    List<SkillView> skillsOf(UUID userId);

    record SkillView(UUID skillId, String name, String proficiency) {}
}
