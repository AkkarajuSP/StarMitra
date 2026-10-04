package com.starmitra.modules.judgeportal.api;

import com.starmitra.modules.judge.application.JudgeService;
import com.starmitra.modules.judge.application.JudgeScopeService;
import com.starmitra.platform.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * M20 Judge Portal — orchestration surface only. Depends on M12 application
 * contracts (JudgeScopeService / JudgeService), never on M12 persistence.
 * Scope is resolved server-side; no cross-assignment access possible.
 */
@RestController
@RequestMapping("/api/v1/judges/me")
public class JudgePortalController {

    private final JudgeScopeService judgeScope;
    private final JudgeService judges;

    public JudgePortalController(JudgeScopeService judgeScope, JudgeService judges) {
        this.judgeScope = judgeScope;
        this.judges = judges;
    }

    @GetMapping
    public ResponseEntity<JudgeScopeService.JudgeView> getMyJudgeProfile() {
        return ResponseEntity.ok(judgeScope.requireJudge(SecurityUtils.currentUserId()));
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<JudgeScopeService.AssignmentView>> myAssignments() {
        return ResponseEntity.ok(
                judgeScope.activeAssignments(judgeScope.requireJudge(SecurityUtils.currentUserId()).judgeId()));
    }

    /** Scope-resolved FINALIZED submissions — server-side, never frontend-filtered. */
    @GetMapping("/submissions")
    public ResponseEntity<List<JudgeService.ScopedSubmission>> myScopedSubmissions(
            @RequestParam(required = false) UUID competitionId) {
        return ResponseEntity.ok(
                judges.myScopedSubmissions(SecurityUtils.currentUserId(), competitionId));
    }

    /** M13 rubric seam — 404 until rubric versions exist. */
    @GetMapping("/rubrics/{contextId}")
    public ResponseEntity<Void> myRubric(@PathVariable UUID contextId) {
        judges.myRubric(SecurityUtils.currentUserId(), contextId);
        return ResponseEntity.ok().build();
    }
}
