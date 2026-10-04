package com.starmitra.modules.submission.api;

import com.starmitra.modules.submission.application.SubmissionService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /api/v1 — M10 Submissions. */
@RestController
public class SubmissionController {

    private final SubmissionService submissions;

    public SubmissionController(SubmissionService submissions) {
        this.submissions = submissions;
    }

    @PostMapping("/api/v1/competitions/{competitionId}/submissions")
    public ResponseEntity<SubmissionDtos.Submission> createSubmission(
            @PathVariable UUID competitionId,
            @Valid @RequestBody SubmissionDtos.SubmissionCreate body) {
        var s = submissions.create(SecurityUtils.currentUserId(), body.participantId(), body.roundId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(s));
    }

    @GetMapping("/api/v1/submissions/{submissionId}")
    public ResponseEntity<SubmissionDtos.Submission> getSubmission(@PathVariable UUID submissionId) {
        return ResponseEntity.ok(toDto(submissions.get(SecurityUtils.currentUserId(), submissionId)));
    }

    @PostMapping("/api/v1/submissions/{submissionId}/media")
    public ResponseEntity<Void> attachSubmissionMedia(@PathVariable UUID submissionId,
                                                      @Valid @RequestBody SubmissionDtos.AssetLink body) {
        submissions.attachMedia(SecurityUtils.currentUserId(), submissionId,
                body.mediaId(), body.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/api/v1/submissions/{submissionId}/contributors")
    public ResponseEntity<Void> declareContributors(
            @PathVariable UUID submissionId,
            @Valid @RequestBody SubmissionDtos.ContributorDeclare body) {
        submissions.declareContributors(SecurityUtils.currentUserId(), submissionId,
                body.contributors().stream()
                        .map(c -> new SubmissionService.ContributorCmd(c.memberRef(), c.roleRef()))
                        .toList());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/submissions/{submissionId}/submit")
    public ResponseEntity<SubmissionDtos.Submission> submitSubmission(@PathVariable UUID submissionId) {
        return ResponseEntity.ok(toDto(submissions.submit(SecurityUtils.currentUserId(), submissionId)));
    }

    @PostMapping("/api/v1/submissions/{submissionId}/finalize")
    public ResponseEntity<SubmissionDtos.Submission> finalizeSubmission(@PathVariable UUID submissionId) {
        return ResponseEntity.ok(toDto(submissions.finalize(SecurityUtils.currentUserId(), submissionId)));
    }

    @PostMapping("/api/v1/submissions/{submissionId}/withdraw")
    public ResponseEntity<SubmissionDtos.Submission> withdrawSubmission(@PathVariable UUID submissionId) {
        return ResponseEntity.ok(toDto(submissions.withdraw(SecurityUtils.currentUserId(), submissionId)));
    }

    @GetMapping("/api/v1/submissions/{submissionId}/history")
    public ResponseEntity<List<Map<String, Object>>> submissionHistory(@PathVariable UUID submissionId) {
        return ResponseEntity.ok(submissions.history(SecurityUtils.currentUserId(), submissionId).stream()
                .map(h -> Map.<String, Object>of(
                        "from", h.fromState() == null ? "" : h.fromState(),
                        "to", h.toState(), "at", h.at()))
                .toList());
    }

    @GetMapping("/api/v1/competitions/{competitionId}/submissions/list")
    public ResponseEntity<SubmissionDtos.SubmissionPage> listSubmissions(
            @PathVariable UUID competitionId,
            @RequestParam(required = false) UUID roundId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        var p = submissions.list(SecurityUtils.currentUserId(), competitionId, roundId, categoryId,
                cursor, limit);
        return ResponseEntity.ok(new SubmissionDtos.SubmissionPage(
                p.items().stream().map(this::toDto).toList(),
                new SubmissionDtos.PageMeta(p.page().nextCursor(), p.page().hasMore(), p.page().total())));
    }

    private SubmissionDtos.Submission toDto(SubmissionService.SubmissionView s) {
        return new SubmissionDtos.Submission(s.id(), s.participantId(), s.competitionId(),
                s.roundId(), s.state(), s.submittedAt(), s.finalizedAt());
    }
}
