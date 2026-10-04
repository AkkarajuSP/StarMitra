package com.starmitra.modules.connect.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class ConversationEntity {

    public enum Type { ONE_TO_ONE, GROUP, PROJECT }

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(name = "project_id")
    private UUID projectId;                       // REF → creative_rooms (no FK)

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected ConversationEntity() {}

    public ConversationEntity(Type type, UUID projectId, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.projectId = projectId;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public Type getType() { return type; }
    public UUID getProjectId() { return projectId; }
    public String getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
