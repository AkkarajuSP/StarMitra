package com.starmitra.modules.scoring.api;

import com.starmitra.modules.scoring.application.ScoringService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /api/v1 — M14 Scoring & Ranking. */
@RestController
public class ScoringController {

    private final ScoringService scoring;

    public ScoringController(ScoringService scoring) {
        this.scoring = scoring;
    }

    public record ScoringConfigCreate(@NotNull UUID competitionId,
                                      @NotNull BigDecimal weightAudience,
                                      @NotNull BigDecimal weightJudge) {}
    public record ConfigCreate(@NotNull UUID competitionId, @NotNull Map<String, Object> payload) {}
    public record ScoringTrigger(@NotNull UUID competitionId, @NotNull UUID roundId,
                                 UUID configVersionId) {}
    public record OverrideCreate(@NotNull UUID targetId, @NotNull BigDecimal afterValue,
                                 @NotBlank String reason) {}
    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}
    public record ResultPage(List<Map<String, Object>> items, PageMeta page) {}

    @PostMapping("/api/v1/scoring-configs")
    public ResponseEntity<Map<String, String>> createScoringConfig(
            @Valid @RequestBody ScoringConfigCreate body) {
        var id = scoring.createScoringConfig(SecurityUtils.currentUserId(), body.competitionId(),
                body.weightAudience(), body.weightJudge());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id.toString()));
    }

    @PostMapping("/api/v1/tie-break-configs")
    public ResponseEntity<Map<String, String>> createTieBreakConfig(
            @Valid @RequestBody ConfigCreate body) {
        var id = scoring.createTieBreakConfig(body.competitionId(), body.payload());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id.toString()));
    }

    @PostMapping("/api/v1/qualification-configs")
    public ResponseEntity<Map<String, String>> createQualificationConfig(
            @Valid @RequestBody ConfigCreate body) {
        var id = scoring.createQualificationConfig(body.competitionId(), body.payload());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id.toString()));
    }

    @PostMapping("/api/v1/scoring/calculate")
    public ResponseEntity<Map<String, Object>> calculateScores(
            @Valid @RequestBody ScoringTrigger body) {
        int n = scoring.calculate(SecurityUtils.currentUserId(), body.competitionId(),
                body.roundId(), body.configVersionId());
        return ResponseEntity.accepted().body(Map.of("calculated", n));
    }

    @PostMapping("/api/v1/scoring/finalize")
    public ResponseEntity<Map<String, Object>> finalizeScores(
            @Valid @RequestBody ScoringTrigger body) {
        int n = scoring.finalize(SecurityUtils.currentUserId(), body.competitionId(), body.roundId());
        return ResponseEntity.ok(Map.of("sealed", n));
    }

    @GetMapping("/api/v1/scoring/results")
    public ResponseEntity<ResultPage> getResults(@RequestParam UUID competitionId,
                                                 @RequestParam(required = false) UUID roundId) {
        var items = scoring.results(competitionId, roundId).stream()
                .<Map<String, Object>>map(r -> Map.of(
                        "submissionId", r.submissionId().toString(),
                        "finalScore", r.finalScore() == null ? "" : r.finalScore(),
                        "rank", r.rank() == null ? 0 : r.rank(),
                        "qualified", r.qualified() != null && r.qualified(),
                        "overridden", r.overridden()))
                .toList();
        return ResponseEntity.ok(new ResultPage(items, null));
    }

    @PostMapping("/api/v1/scoring/overrides")
    public ResponseEntity<Void> createScoreOverride(@Valid @RequestBody OverrideCreate body) {
        scoring.override(SecurityUtils.currentUserId(), body.targetId(), body.afterValue(),
                body.reason());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
