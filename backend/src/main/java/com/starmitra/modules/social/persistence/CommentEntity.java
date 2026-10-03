package com.starmitra.modules.social.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class CommentEntity {

    public enum Status { ACTIVE, REMOVED }

    @Id
    private UUID id;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected CommentEntity() {}

    public CommentEntity(UUID authorId, String targetType, UUID targetId, String body) {
        this.id = UUID.randomUUID();
        this.authorId = authorId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.body = body;
    }

    public void softDelete() { this.status = Status.REMOVED; }

    public UUID getId() { return id; }
    public UUID getAuthorId() { return authorId; }
    public String getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public String getBody() { return body; }
    public Status getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
