package com.starmitra.modules.social.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "likes")
public class LikeEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;                   // MEDIA | PORTFOLIO (ck_likes_target)

    @Column(name = "target_id", nullable = false)
    private UUID targetId;                       // polymorphic — no FK by design (V1.17)

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected LikeEntity() {}

    public LikeEntity(UUID userId, String targetType, UUID targetId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
}
