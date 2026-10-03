package com.starmitra.modules.skill.application;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Contract implementation until M03 lands its feature slice — returns an
 * empty skill set. Real reads will come from user_talent_skills under M03.
 */
@Component
public class EmptyUserSkillReadContract implements UserSkillReadContract {

    @Override
    public List<SkillView> skillsOf(UUID userId) {
        return List.of();
    }
}
