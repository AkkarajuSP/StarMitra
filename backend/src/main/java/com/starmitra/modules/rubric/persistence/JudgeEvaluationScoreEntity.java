package com.starmitra.modules.rubric.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "judge_evaluation_criterion_scores")
@IdClass(JudgeEvaluationScoreEntity.Pk.class)
public class JudgeEvaluationScoreEntity {

    public static class Pk implements Serializable {
        private UUID evaluationId;
        private UUID criterionId;
        public Pk() {}
        public Pk(UUID e, UUID c) { this.evaluationId = e; this.criterionId = c; }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && evaluationId.equals(p.evaluationId) && criterionId.equals(p.criterionId);
        }
        @Override public int hashCode() { return Objects.hash(evaluationId, criterionId); }
    }

    @Id
    @Column(name = "evaluation_id")
    private UUID evaluationId;

    @Id
    @Column(name = "criterion_id")
    private UUID criterionId;

    @Column(nullable = false, precision = 8, scale = 3)
    private BigDecimal score;

    @Column(columnDefinition = "text")
    private String comment;

    protected JudgeEvaluationScoreEntity() {}

    public JudgeEvaluationScoreEntity(UUID evaluationId, UUID criterionId,
                                      BigDecimal score, String comment) {
        this.evaluationId = evaluationId;
        this.criterionId = criterionId;
        this.score = score;
        this.comment = comment;
    }
}
