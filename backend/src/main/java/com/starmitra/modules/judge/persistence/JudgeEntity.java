package com.starmitra.modules.judge.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "judges")
public class JudgeEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected JudgeEntity() {}

    public JudgeEntity(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getStatus() { return status; }
}
