package com.starmitra.modules.progression.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Append-only admin correction — separate from M14 score_overrides. */
@Entity
@Table(name = "progression_overrides")
public class ProgressionOverrideEntity {

    @Id
    private UUID id;

    @Column(name = "progression_record_id", nullable = false)
    private UUID progressionRecordId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "before_outcome", nullable = false, length = 20)
    private String beforeOutcome;

    @Column(name = "after_outcome", nullable = false, length = 20)
    private String afterOutcome;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected ProgressionOverrideEntity() {}

    public ProgressionOverrideEntity(UUID recordId, UUID actorId, String before,
                                     String after, String reason) {
        this.id = UUID.randomUUID();
        this.progressionRecordId = recordId;
        this.actorId = actorId;
        this.beforeOutcome = before;
        this.afterOutcome = after;
        this.reason = reason;
    }
}
