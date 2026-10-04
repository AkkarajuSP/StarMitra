package com.starmitra.modules.room.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "project_members")
@IdClass(ProjectMemberEntity.Pk.class)
public class ProjectMemberEntity {

    public enum Status { ACTIVE, LEFT }

    public static class Pk implements Serializable {
        private UUID roomId;
        private UUID userId;
        public Pk() {}
        public Pk(UUID roomId, UUID userId) { this.roomId = roomId; this.userId = userId; }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && roomId.equals(p.roomId) && userId.equals(p.userId);
        }
        @Override public int hashCode() { return Objects.hash(roomId, userId); }
    }

    @Id
    @Column(name = "room_id")
    private UUID roomId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt = OffsetDateTime.now();

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    protected ProjectMemberEntity() {}

    public ProjectMemberEntity(UUID roomId, UUID userId) {
        this.roomId = roomId;
        this.userId = userId;
    }

    public void leave() {
        this.status = Status.LEFT;
        this.leftAt = OffsetDateTime.now();
    }

    public void rejoin() {
        this.status = Status.ACTIVE;
        this.leftAt = null;
        this.joinedAt = OffsetDateTime.now();
    }

    public UUID getRoomId() { return roomId; }
    public UUID getUserId() { return userId; }
    public Status getStatus() { return status; }
    public OffsetDateTime getJoinedAt() { return joinedAt; }
}
