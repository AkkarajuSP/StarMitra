package com.starmitra.modules.room.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_invitations")
public class ProjectInvitationEntity {

    public enum Status { PENDING, ACCEPTED, DECLINED, WITHDRAWN, EXPIRED }

    @Id
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "inviter_id", nullable = false)
    private UUID inviterId;

    @Column(name = "invitee_id", nullable = false)
    private UUID inviteeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected ProjectInvitationEntity() {}

    public ProjectInvitationEntity(UUID roomId, UUID inviterId, UUID inviteeId, OffsetDateTime expiresAt) {
        this.id = UUID.randomUUID();
        this.roomId = roomId;
        this.inviterId = inviterId;
        this.inviteeId = inviteeId;
        this.expiresAt = expiresAt;
    }

    public boolean isPending() { return status == Status.PENDING; }
    public void accept() { this.status = Status.ACCEPTED; }
    public void decline() { this.status = Status.DECLINED; }

    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public UUID getInviterId() { return inviterId; }
    public UUID getInviteeId() { return inviteeId; }
    public Status getStatus() { return status; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}
