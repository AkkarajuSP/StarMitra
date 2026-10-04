package com.starmitra.modules.scoring.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** M14 qualification truth — consumed by M15 (never transitions rounds here). */
@Entity
@Table(name = "qualification_results")
public class QualificationResultEntity {

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "round_id", nullable = false)
    private UUID roundId;

    @Column(name = "config_version_id", nullable = false)
    private UUID configVersionId;

    @Column(nullable = false)
    private boolean qualified;

    @Column(name = "decided_at", nullable = false)
    private OffsetDateTime decidedAt = OffsetDateTime.now();

    protected QualificationResultEntity() {}

    public QualificationResultEntity(UUID submissionId, UUID roundId,
                                     UUID configVersionId, boolean qualified) {
        this.id = UUID.randomUUID();
        this.submissionId = submissionId;
        this.roundId = roundId;
        this.configVersionId = configVersionId;
        this.qualified = qualified;
    }

    public UUID getSubmissionId() { return submissionId; }
    public boolean isQualified() { return qualified; }
}
