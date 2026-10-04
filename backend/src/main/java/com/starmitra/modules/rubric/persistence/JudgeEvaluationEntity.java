package com.starmitra.modules.rubric.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** DB-09: one evaluation per judge+submission+round, bound to exact rubric version. */
@Entity
@Table(name = "judge_evaluations")
public class JudgeEvaluationEntity {

    public enum Status { SUBMITTED, REOPENED }

    @Id
    private UUID id;

    @Column(name = "judge_id", nullable = false)
    private UUID judgeId;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "rubric_version_id", nullable = false)
    private UUID rubricVersionId;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "round_id", nullable = false)
    private UUID roundId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.SUBMITTED;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected JudgeEvaluationEntity() {}

    public JudgeEvaluationEntity(UUID judgeId, UUID assignmentId, UUID submissionId,
                                 UUID rubricVersionId, UUID competitionId, UUID categoryId,
                                 UUID roundId) {
        this.id = UUID.randomUUID();
        this.judgeId = judgeId;
        this.assignmentId = assignmentId;
        this.submissionId = submissionId;
        this.rubricVersionId = rubricVersionId;
        this.competitionId = competitionId;
        this.categoryId = categoryId;
        this.roundId = roundId;
        this.submittedAt = OffsetDateTime.now();
    }

    public void reopen() {
        this.status = Status.REOPENED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void resubmit() {
        this.status = Status.SUBMITTED;
        this.submittedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getJudgeId() { return judgeId; }
    public UUID getAssignmentId() { return assignmentId; }
    public UUID getSubmissionId() { return submissionId; }
    public UUID getRubricVersionId() { return rubricVersionId; }
    public UUID getRoundId() { return roundId; }
    public Status getStatus() { return status; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
}
