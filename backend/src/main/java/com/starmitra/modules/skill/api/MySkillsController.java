package com.starmitra.modules.skill.api;

import com.starmitra.modules.skill.application.SkillService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** /api/v1/users/me/skills — own associations; owner always from JWT sub. */
@RestController
@RequestMapping("/api/v1/users/me/skills")
public class MySkillsController {

    private final SkillService skills;

    public MySkillsController(SkillService skills) {
        this.skills = skills;
    }

    @GetMapping
    public ResponseEntity<List<SkillDtos.UserSkill>> getMySkills() {
        return ResponseEntity.ok(skills.mySkills(SecurityUtils.currentUserId()).stream()
                .map(this::toDto).toList());
    }

    @PutMapping("/{skillId}")
    public ResponseEntity<SkillDtos.UserSkill> addMySkill(
            @PathVariable UUID skillId,
            @Valid @RequestBody(required = false) SkillDtos.UserSkillUpdate body) {
        String proficiency = body == null ? null : body.proficiency();
        return ResponseEntity.ok(toDto(skills.addMySkill(SecurityUtils.currentUserId(), skillId, proficiency)));
    }

    @DeleteMapping("/{skillId}")
    public ResponseEntity<Void> removeMySkill(@PathVariable UUID skillId) {
        skills.removeMySkill(SecurityUtils.currentUserId(), skillId);
        return ResponseEntity.noContent().build();
    }

    private SkillDtos.UserSkill toDto(SkillService.UserSkillView v) {
        return new SkillDtos.UserSkill(v.skillId(), v.name(), v.proficiency());
    }
}
