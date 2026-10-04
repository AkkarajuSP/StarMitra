package com.starmitra.modules.rubric.api;

import com.starmitra.modules.rubric.application.RubricService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** /api/v1/evaluation-templates + /evaluations — M13 Rubrics. */
@RestController
public class RubricController {

    private final RubricService rubrics;

    public RubricController(RubricService rubrics) {
        this.rubrics = rubrics;
    }

    @PostMapping("/api/v1/evaluation-templates")
    public ResponseEntity<RubricDtos.Rubric> createTemplate(
            @Valid @RequestBody RubricDtos.TemplateCreate body) {
        var t = rubrics.createTemplate(SecurityUtils.currentUserId(), body.name(),
                toCmds(body.criteria()),
                body.scaleMin(), body.scaleMax(), body.competitionId(), body.categoryId(),
                body.roundId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RubricDtos.Rubric(t.id(), t.name(), t.status()));
    }

    @GetMapping("/api/v1/evaluation-templates")
    public ResponseEntity<RubricDtos.RubricPage> listTemplates(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        var items = rubrics.listTemplates(offset, limit).stream()
                .map(t -> new RubricDtos.Rubric(t.id(), t.name(), t.status())).toList();
        return ResponseEntity.ok(new RubricDtos.RubricPage(items, null));
    }

    @PutMapping("/api/v1/evaluation-templates/{templateId}")
    public ResponseEntity<RubricDtos.RubricVersion> updateTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody RubricDtos.TemplateCreate body) {
        var v = rubrics.updateTemplate(templateId, toCmds(body.criteria()),
                body.scaleMin(), body.scaleMax(), body.competitionId(), body.categoryId(),
                body.roundId());
        return ResponseEntity.ok(new RubricDtos.RubricVersion(v.id(), v.templateId(),
                v.versionNo(), v.status(), null));
    }

    @PostMapping("/api/v1/evaluation-template-versions/{versionId}/publish")
    public ResponseEntity<RubricDtos.RubricVersion> publishRubricVersion(@PathVariable UUID versionId) {
        var v = rubrics.publish(SecurityUtils.currentUserId(), versionId);
        return ResponseEntity.ok(new RubricDtos.RubricVersion(v.id(), v.templateId(),
                v.versionNo(), v.status(), null));
    }

    @PostMapping("/api/v1/evaluations")
    public ResponseEntity<RubricDtos.Evaluation> submitEvaluation(
            @Valid @RequestBody RubricDtos.EvaluationSubmit body) {
        var e = rubrics.submitEvaluation(SecurityUtils.currentUserId(), body.submissionId(),
                body.rubricVersionId(), body.scores().stream()
                        .map(s -> new RubricService.ScoreCmd(s.criterionId(), s.score(), s.comment()))
                        .toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(toEval(e));
    }

    @GetMapping("/api/v1/evaluations/{evaluationId}")
    public ResponseEntity<RubricDtos.Evaluation> getEvaluation(@PathVariable UUID evaluationId) {
        return ResponseEntity.ok(toEval(rubrics.getEvaluation(
                SecurityUtils.currentUserId(), evaluationId)));
    }

    @PostMapping("/api/v1/evaluations/{evaluationId}/reopen")
    public ResponseEntity<Void> reopenEvaluation(@PathVariable UUID evaluationId) {
        rubrics.reopenEvaluation(SecurityUtils.currentUserId(), evaluationId);
        return ResponseEntity.ok().build();
    }

    private List<RubricService.CriterionCmd> toCmds(List<RubricDtos.Criterion> list) {
        return list == null ? List.of() : list.stream()
                .map(c -> new RubricService.CriterionCmd(c.name(), c.description(),
                        c.weight(), c.maxScore())).toList();
    }

    private RubricDtos.Evaluation toEval(RubricService.EvaluationView e) {
        return new RubricDtos.Evaluation(e.id(), e.judgeId(), e.submissionId(),
                e.rubricVersionId(), e.status(), e.submittedAt());
    }
}
