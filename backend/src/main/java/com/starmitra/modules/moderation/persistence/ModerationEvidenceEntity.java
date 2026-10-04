package com.starmitra.modules.moderation.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Typed evidence refs (REPORT, MEDIA, SCREENSHOT_REF, ...) — never duplicates content. */
@Entity
@Table(name = "moderation_evidence_references")
public class ModerationEvidenceEntity {

    @Id
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "evidence_type", nullable = false, length = 40)
    private String evidenceType;

    @Column(name = "ref_type", nullable = false, length = 40)
    private String refType;

    @Column(name = "ref_id", nullable = false)
    private UUID refId;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt = OffsetDateTime.now();

    protected ModerationEvidenceEntity() {}

    public ModerationEvidenceEntity(UUID caseId, String evidenceType, String refType, UUID refId) {
        this.id = UUID.randomUUID();
        this.caseId = caseId;
        this.evidenceType = evidenceType;
        this.refType = refType;
        this.refId = refId;
    }
}
