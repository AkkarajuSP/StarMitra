package com.starmitra.modules.room.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "required_skills")
@IdClass(RequiredSkillEntity.Pk.class)
public class RequiredSkillEntity {

    public static class Pk implements Serializable {
        private UUID roomId;
        private UUID skillId;
        public Pk() {}
        public Pk(UUID roomId, UUID skillId) { this.roomId = roomId; this.skillId = skillId; }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && roomId.equals(p.roomId) && skillId.equals(p.skillId);
        }
        @Override public int hashCode() { return Objects.hash(roomId, skillId); }
    }

    @Id
    @Column(name = "room_id")
    private UUID roomId;

    @Id
    @Column(name = "skill_id")
    private UUID skillId;                          // REF → talent_skills (no FK)

    protected RequiredSkillEntity() {}

    public RequiredSkillEntity(UUID roomId, UUID skillId) {
        this.roomId = roomId;
        this.skillId = skillId;
    }

    public UUID getRoomId() { return roomId; }
    public UUID getSkillId() { return skillId; }
}
