package com.starmitra.modules.skill.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "talent_skills")
public class TalentSkillEntity {

    public enum Status { ACTIVE, INACTIVE }

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "parent_skill_id")
    private UUID parentSkillId;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected TalentSkillEntity() {}

    public TalentSkillEntity(String name, String description, UUID parentSkillId) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.parentSkillId = parentSkillId;
    }

    public void update(String name, String description, UUID parentSkillId) {
        if (name != null) this.name = name;
        this.description = description;
        this.parentSkillId = parentSkillId;
        this.updatedAt = OffsetDateTime.now();
    }

    public void deactivate() {
        this.status = Status.INACTIVE;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public UUID getParentSkillId() { return parentSkillId; }
    public boolean isActive() { return status == Status.ACTIVE; }
}
