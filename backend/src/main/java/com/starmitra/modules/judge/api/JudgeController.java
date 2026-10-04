package com.starmitra.modules.judge.api;

import com.starmitra.modules.judge.application.JudgeService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** /api/v1/judges + /api/v1/assignments — M12 (+M20 judge-portal surfaces). */
@RestController
public class JudgeController {

    private final JudgeService judges;

    public JudgeController(JudgeService judges) {
        this.judges = judges;
    }

    @PostMapping("/api/v1/judges")
    public ResponseEntity<JudgeDtos.Judge> createJudge(@Valid @RequestBody JudgeDtos.JudgeCreate body) {
        var j = judges.createJudge(SecurityUtils.currentUserId(), body.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toJudge(j));
    }

    @GetMapping("/api/v1/judges")
    public ResponseEntity<JudgeDtos.JudgePage> listJudges(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        var items = judges.listJudges(offset, limit).stream().map(this::toJudge).toList();
        return ResponseEntity.ok(new JudgeDtos.JudgePage(items, null));
    }

    @PutMapping("/api/v1/judges/{judgeId}/expertise")
    public ResponseEntity<Void> setJudgeExpertise(@PathVariable UUID judgeId,
                                                  @Valid @RequestBody List<JudgeDtos.Expertise> body) {
        judges.setExpertise(SecurityUtils.currentUserId(), judgeId, body.stream()
                .map(e -> new JudgeService.ExpertiseCmd(e.skillId(), e.domainLabel(), e.verified()))
                .toList());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/judges/{judgeId}/assignments")
    public ResponseEntity<JudgeDtos.Assignment> createAssignment(
            @PathVariable UUID judgeId, @Valid @RequestBody JudgeDtos.AssignmentCreate body) {
        var a = judges.assign(SecurityUtils.currentUserId(), judgeId,
                new JudgeService.AssignmentCmd(body.competitionId(), body.categoryId(), body.roundId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toAssignment(a));
    }

    @PostMapping("/api/v1/assignments/{assignmentId}/revoke")
    public ResponseEntity<Void> revokeAssignment(@PathVariable UUID assignmentId) {
        judges.revokeAssignment(SecurityUtils.currentUserId(), assignmentId);
        return ResponseEntity.ok().build();
    }

    private JudgeDtos.Judge toJudge(JudgeService.JudgeView j) {
        return new JudgeDtos.Judge(j.id(), j.userId(), j.status());
    }

    private JudgeDtos.Assignment toAssignment(JudgeService.AssignmentView a) {
        return new JudgeDtos.Assignment(a.id(), a.judgeId(), a.competitionId(),
                a.categoryId(), a.roundId(), a.status());
    }
}
