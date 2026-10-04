package com.starmitra.modules.room.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Authoritative project credit — the only source M08 portfolio may link. */
@Entity
@Table(name = "project_credits")
public class ProjectCreditEntity {

    @Id
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "member_user_id", nullable = false)
    private UUID memberUserId;

    @Column(name = "contribution_role_id", nullable = false)
    private UUID contributionRoleId;

    @Column(name = "credit_label", nullable = false, length = 160)
    private String creditLabel;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected ProjectCreditEntity() {}

    public ProjectCreditEntity(UUID roomId, UUID memberUserId, UUID contributionRoleId,
                               String creditLabel) {
        this.id = UUID.randomUUID();
        this.roomId = roomId;
        this.memberUserId = memberUserId;
        this.contributionRoleId = contributionRoleId;
        this.creditLabel = creditLabel;
    }

    public void verify() {
        this.verified = true;
        this.verifiedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public UUID getMemberUserId() { return memberUserId; }
    public UUID getContributionRoleId() { return contributionRoleId; }
    public String getCreditLabel() { return creditLabel; }
    public boolean isVerified() { return verified; }
}
