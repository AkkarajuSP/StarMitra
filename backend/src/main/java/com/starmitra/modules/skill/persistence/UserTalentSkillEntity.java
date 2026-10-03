package com.starmitra.modules.skill.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_talent_skills")
@IdClass(UserTalentSkillEntity.Pk.class)
public class UserTalentSkillEntity {

    public static class Pk implements Serializable {
        private UUID userId;
        private UUID skillId;
        public Pk() {}
        public Pk(UUID userId, UUID skillId) { this.userId = userId; this.skillId = skillId; }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && userId.equals(p.userId) && skillId.equals(p.skillId);
        }
        @Override public int hashCode() { return Objects.hash(userId, skillId); }
    }

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "skill_id")
    private UUID skillId;

    @Column(name = "proficiency_id")
    private UUID proficiencyId;

    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt = OffsetDateTime.now();

    protected UserTalentSkillEntity() {}

    public UserTalentSkillEntity(UUID userId, UUID skillId, UUID proficiencyId) {
        this.userId = userId;
        this.skillId = skillId;
        this.proficiencyId = proficiencyId;
    }

    public void setProficiencyId(UUID proficiencyId) { this.proficiencyId = proficiencyId; }

    public UUID getUserId() { return userId; }
    public UUID getSkillId() { return skillId; }
    public UUID getProficiencyId() { return proficiencyId; }
}
