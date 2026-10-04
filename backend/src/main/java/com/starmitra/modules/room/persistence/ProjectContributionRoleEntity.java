package com.starmitra.modules.room.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Contextual project role ("Lead Actor") — NEVER a TalentSkill or permission. */
@Entity
@Table(name = "project_contribution_roles")
public class ProjectContributionRoleEntity {

    @Id
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "member_user_id", nullable = false)
    private UUID memberUserId;

    @Column(name = "role_name", nullable = false, length = 80)
    private String roleName;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt = OffsetDateTime.now();

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected ProjectContributionRoleEntity() {}

    public ProjectContributionRoleEntity(UUID roomId, UUID memberUserId, String roleName) {
        this.id = UUID.randomUUID();
        this.roomId = roomId;
        this.memberUserId = memberUserId;
        this.roleName = roleName;
    }

    public boolean isActive() { return revokedAt == null; }

    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public UUID getMemberUserId() { return memberUserId; }
    public String getRoleName() { return roleName; }
}
