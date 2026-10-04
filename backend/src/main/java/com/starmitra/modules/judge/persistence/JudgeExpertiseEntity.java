package com.starmitra.modules.judge.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Qualification data — never authorization (FRS). */
@Entity
@Table(name = "judge_expertise")
public class JudgeExpertiseEntity {

    @Id
    private UUID id;

    @Column(name = "judge_id", nullable = false)
    private UUID judgeId;

    @Column(name = "skill_id")
    private UUID skillId;                          // REF → talent_skills (no FK)

    @Column(name = "domain_label", length = 120)
    private String domainLabel;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected JudgeExpertiseEntity() {}

    public JudgeExpertiseEntity(UUID judgeId, UUID skillId, String domainLabel, boolean verified) {
        this.id = UUID.randomUUID();
        this.judgeId = judgeId;
        this.skillId = skillId;
        this.domainLabel = domainLabel;
        this.verified = verified;
    }
}
