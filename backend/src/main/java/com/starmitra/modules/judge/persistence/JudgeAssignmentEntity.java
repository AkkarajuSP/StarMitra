package com.starmitra.modules.judge.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Judge scope grant — the judge authorization boundary (DB-08 inline scope). */
@Entity
@Table(name = "judge_assignments")
public class JudgeAssignmentEntity {

    @Id
    private UUID id;

    @Column(name = "judge_id", nullable = false)
    private UUID judgeId;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "category_id")
    private UUID categoryId;      // nullable = all categories in competition

    @Column(name = "round_id")
    private UUID roundId;         // nullable = all rounds

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt = OffsetDateTime.now();

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected JudgeAssignmentEntity() {}

    public JudgeAssignmentEntity(UUID judgeId, UUID competitionId, UUID categoryId, UUID roundId) {
        this.id = UUID.randomUUID();
        this.judgeId = judgeId;
        this.competitionId = competitionId;
        this.categoryId = categoryId;
        this.roundId = roundId;
    }

    public UUID getId() { return id; }
    public UUID getJudgeId() { return judgeId; }
    public UUID getCompetitionId() { return competitionId; }
    public UUID getCategoryId() { return categoryId; }
    public UUID getRoundId() { return roundId; }
    public boolean isActive() { return "ACTIVE".equals(status) && revokedAt == null; }
    public void revoke() { this.status = "REVOKED"; this.revokedAt = OffsetDateTime.now(); }
}
