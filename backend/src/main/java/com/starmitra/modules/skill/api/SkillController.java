package com.starmitra.modules.skill.api;

import com.starmitra.modules.skill.application.SkillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** /api/v1/skills — taxonomy: read (any authenticated), mutate (ADMIN). */
@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final SkillService skills;

    public SkillController(SkillService skills) {
        this.skills = skills;
    }

    @GetMapping
    public ResponseEntity<SkillDtos.SkillPage> listSkills(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        var r = skills.listActive(cursor, limit);
        return ResponseEntity.ok(new SkillDtos.SkillPage(
                r.items().stream().map(this::toDto).toList(),
                new SkillDtos.PageMeta(r.page().nextCursor(), r.page().hasMore(), r.page().total())));
    }

    @PostMapping
    public ResponseEntity<SkillDtos.Skill> createSkill(@Valid @RequestBody SkillDtos.SkillCreate body) {
        var s = skills.create(body.name(), body.description(), body.parentSkillId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(s));
    }

    @PutMapping("/{skillId}")
    public ResponseEntity<SkillDtos.Skill> updateSkill(@PathVariable UUID skillId,
                                                     @Valid @RequestBody SkillDtos.SkillCreate body) {
        return ResponseEntity.ok(toDto(skills.update(skillId, body.name(), body.description(), body.parentSkillId())));
    }

    @PostMapping("/{skillId}/deactivate")
    public ResponseEntity<Void> deactivateSkill(@PathVariable UUID skillId) {
        skills.deactivate(skillId);
        return ResponseEntity.noContent().build();
    }

    private SkillDtos.Skill toDto(SkillService.SkillView v) {
        return new SkillDtos.Skill(v.id(), v.name(), v.status(), v.parentSkillId());
    }
}
