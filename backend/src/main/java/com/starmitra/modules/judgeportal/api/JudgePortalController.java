package com.starmitra.modules.judgeportal.api;

import com.starmitra.modules.judge.application.JudgeScopeService;
import com.starmitra.platform.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * M20 Judge Portal — orchestration surface only. Depends on M12 application
 * contracts (JudgeScopeService), never on M12 persistence. Scope is resolved
 * server-side; no cross-assignment access possible.
 */
@RestController
@RequestMapping("/api/v1/judges/me")
public class JudgePortalController {

    private final JudgeScopeService judgeScope;

    public JudgePortalController(JudgeScopeService judgeScope) {
        this.judgeScope = judgeScope;
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
}
