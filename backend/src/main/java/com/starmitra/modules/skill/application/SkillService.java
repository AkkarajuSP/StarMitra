package com.starmitra.modules.skill.application;

import com.starmitra.modules.skill.persistence.*;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.pagination.Cursor;
import com.starmitra.platform.security.SystemRoleGuard;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * M03 — talent taxonomy + user-skill associations.
 *
 * CRITICAL INVARIANT: TalentSkill is profile data — NEVER authorization.
 * Nothing here grants roles, scope, or access; ADMIN guard on taxonomy
 * mutation is the only authz in this module.
 *
 * Idempotency: DB uniqueness is the first line (uq_talent_skills_name,
 * pk(user_id, skill_id)); addMySkill is PUT-upsert — naturally idempotent.
 */
@Service
public class SkillService {

    private final TalentSkillRepository skills;
    private final SkillProficiencyRepository proficiencies;
    private final UserTalentSkillRepository userSkills;
    private final AuditService audit;

    public SkillService(TalentSkillRepository skills, SkillProficiencyRepository proficiencies,
                        UserTalentSkillRepository userSkills, AuditService audit) {
        this.skills = skills;
        this.proficiencies = proficiencies;
        this.userSkills = userSkills;
        this.audit = audit;
    }

    public record SkillView(UUID id, String name, String status, UUID parentSkillId) {}
    public record Page(String nextCursor, boolean hasMore, Integer total) {}
    public record SkillPageResult(List<SkillView> items, Page page) {}
    public record UserSkillView(UUID skillId, String name, String proficiency) {}

    // ---------- taxonomy ----------

    @Transactional(readOnly = true)
    public SkillPageResult listActive(String cursor, Integer limit) {
        int size = Cursor.limit(limit);
        UUID after = null;
        if (cursor != null) {
            String[] parts = Cursor.decode(cursor);
            if (parts.length != 2) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid cursor");
            }
            after = UUID.fromString(parts[1]);
        }
        List<TalentSkillEntity> rows = skills.findActiveAfter(after, PageRequest.of(0, size + 1));
        boolean hasMore = rows.size() > size;
        var items = rows.stream().limit(size)
                .map(s -> new SkillView(s.getId(), s.getName(), s.getStatus().name(), s.getParentSkillId()))
                .toList();
        String next = hasMore && !items.isEmpty()
                ? Cursor.encode("id", items.get(items.size() - 1).id().toString())
                : null;
        return new SkillPageResult(items, new Page(next, hasMore, null));
    }

    /** Admin taxonomy create — name UQ is the duplicate guard. */
    @Transactional
    public SkillView create(String name, String description, UUID parentSkillId) {
        SystemRoleGuard.requireAdmin();
        if (parentSkillId != null) requireActiveSkill(parentSkillId);
        var s = new TalentSkillEntity(name, description, parentSkillId);
        try {
            s = skills.saveAndFlush(s);
        } catch (DataIntegrityViolationException dup) {
            throw new ApiException(ErrorCode.CONFLICT, "Skill name already exists");
        }
        audit.record("M03", "SKILL_CREATED", null, "admin", "talent_skill", s.getId().toString(), null);
        return new SkillView(s.getId(), s.getName(), s.getStatus().name(), s.getParentSkillId());
    }

    @Transactional
    public SkillView update(UUID skillId, String name, String description, UUID parentSkillId) {
        SystemRoleGuard.requireAdmin();
        var s = skills.findById(skillId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Skill not found"));
        s.update(name, description, parentSkillId);
        s = skills.saveAndFlush(s);
        return new SkillView(s.getId(), s.getName(), s.getStatus().name(), s.getParentSkillId());
    }

    /** Lifecycle: deactivate, never delete-in-place — historical references must survive. */
    @Transactional
    public void deactivate(UUID skillId) {
        SystemRoleGuard.requireAdmin();
        var s = skills.findById(skillId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Skill not found"));
        if (s.isActive()) {
            s.deactivate();
            skills.save(s);
            audit.record("M03", "SKILL_DEACTIVATED", null, "admin", "talent_skill", skillId.toString(), null);
        }
    }

    // ---------- user associations ----------

    @Transactional(readOnly = true)
    public List<UserSkillView> mySkills(UUID userId) {
        return userSkills.findSkillViewsByUserId(userId).stream()
                .map(r -> new UserSkillView((UUID) r[0], (String) r[1], (String) r[2]))
                .toList();
    }

    /** Idempotent PUT-upsert: create association or update proficiency. */
    @Transactional
    public UserSkillView addMySkill(UUID userId, UUID skillId, String proficiencyCode) {
        var skill = requireActiveSkill(skillId);
        UUID profId = proficiencyIdOrNull(proficiencyCode);
        var key = new UserTalentSkillEntity.Pk(userId, skillId);
        var existing = userSkills.findById(key);
        if (existing.isPresent()) {
            existing.get().setProficiencyId(profId);
            userSkills.save(existing.get());
        } else {
            try {
                userSkills.saveAndFlush(new UserTalentSkillEntity(userId, skillId, profId));
                audit.record("M03", "USER_SKILL_ADDED", userId, "user", "talent_skill", skillId.toString(), null);
            } catch (DataIntegrityViolationException dup) {
                // concurrent first insert — converged on same PK
            }
        }
        return new UserSkillView(skill.getId(), skill.getName(), proficiencyCode);
    }

    /** Idempotent DELETE — removing a non-associated skill is a no-op 204. */
    @Transactional
    public void removeMySkill(UUID userId, UUID skillId) {
        var key = new UserTalentSkillEntity.Pk(userId, skillId);
        if (userSkills.existsById(key)) {
            userSkills.deleteById(key);
            audit.record("M03", "USER_SKILL_REMOVED", userId, "user", "talent_skill", skillId.toString(), null);
        }
    }

    private TalentSkillEntity requireActiveSkill(UUID skillId) {
        return skills.findById(skillId)
                .filter(TalentSkillEntity::isActive)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Skill not found or inactive"));
    }

    private UUID proficiencyIdOrNull(String code) {
        if (code == null) return null;
        return proficiencies.findByCode(code)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown proficiency"))
                .getId();
    }
}
