package com.starmitra.modules.skill.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "skill_proficiencies")
public class SkillProficiencyEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 40, unique = true)
    private String code;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(nullable = false)
    private int ordinal;

    protected SkillProficiencyEntity() {}

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
}
