package com.starmitra.modules.competition.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "competition_category_skills")
@IdClass(CompetitionCategorySkillEntity.Pk.class)
public class CompetitionCategorySkillEntity {

    public static class Pk implements Serializable {
        private UUID categoryId;
        private UUID skillId;
        public Pk() {}
        public Pk(UUID c, UUID s) { this.categoryId = c; this.skillId = s; }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && categoryId.equals(p.categoryId) && skillId.equals(p.skillId);
        }
        @Override public int hashCode() { return Objects.hash(categoryId, skillId); }
    }

    @Id
    @Column(name = "category_id")
    private UUID categoryId;

    @Id
    @Column(name = "skill_id")
    private UUID skillId;                          // REF → talent_skills (no FK)

    protected CompetitionCategorySkillEntity() {}

    public CompetitionCategorySkillEntity(UUID categoryId, UUID skillId) {
        this.categoryId = categoryId;
        this.skillId = skillId;
    }

    public UUID getSkillId() { return skillId; }
}
