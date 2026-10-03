package com.starmitra.modules.skill.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Real M03 implementation of the M02-consumed read contract — replaces the
 * temporary EmptyUserSkillReadContract. Returns ACTIVE skills only.
 */
@Service
public class UserSkillReadService implements UserSkillReadContract {

    private final SkillService skills;

    public UserSkillReadService(SkillService skills) {
        this.skills = skills;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillView> skillsOf(UUID userId) {
        return skills.mySkills(userId).stream()
                .map(s -> new SkillView(s.skillId(), s.name(), s.proficiency()))
                .toList();
    }
}
