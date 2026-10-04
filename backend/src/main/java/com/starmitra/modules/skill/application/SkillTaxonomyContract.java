package com.starmitra.modules.skill.application;

import java.util.UUID;

/** M03-owned read contract — other modules validate a skill ref without touching taxonomy. */
public interface SkillTaxonomyContract {
    boolean isActiveSkill(UUID skillId);
}
