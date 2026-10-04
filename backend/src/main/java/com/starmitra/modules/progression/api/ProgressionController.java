package com.starmitra.modules.progression.api;

import com.starmitra.modules.progression.application.ProgressionService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /api/v1/progression* — M15 Round Progression. */
@RestController
public class ProgressionController {

    private final ProgressionService progression;

    public ProgressionController(ProgressionService progression) {
        this.progression = progression;
    }

    public record ConfigCreate(@NotNull UUID competitionId, @NotNull Map<String, Object> payload) {}
    public record ScoringTrigger(@NotNull UUID competitionId, @NotNull UUID roundId,
                                 UUID configVersionId) {}
    public record OverrideCreate(@NotNull UUID targetId, @NotBlank String afterValue,
                                 @NotBlank String reason) {}
    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}
    public record ProgressionPage(List<ProgressionService.RecordView> items, PageMeta page) {}

    @PostMapping("/api/v1/progression-configs")
    public ResponseEntity<Map<String, String>> createProgressionConfig(
            @Valid @RequestBody ConfigCreate body) {
        var id = progression.createConfig(body.competitionId(), body.payload());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id.toString()));
    }

    @PostMapping("/api/v1/progression/calculate")
    public ResponseEntity<Map<String, Object>> calculateProgression(
            @Valid @RequestBody ScoringTrigger body) {
        int n = progression.calculate(SecurityUtils.currentUserId(), body.competitionId(),
                body.roundId(), body.configVersionId());
        return ResponseEntity.accepted().body(Map.of("records", n));
    }

    @PostMapping("/api/v1/progression/finalize")
    public ResponseEntity<Map<String, Object>> finalizeProgression(
            @Valid @RequestBody ScoringTrigger body) {
        int n = progression.finalize(SecurityUtils.currentUserId(), body.competitionId(),
                body.roundId());
        return ResponseEntity.ok(Map.of("finalized", n));
    }

    @GetMapping("/api/v1/progression/records")
    public ResponseEntity<ProgressionPage> getProgressionRecords(@RequestParam UUID roundId) {
        return ResponseEntity.ok(new ProgressionPage(progression.records(roundId), null));
    }

    @PostMapping("/api/v1/progression/overrides")
    public ResponseEntity<Void> createProgressionOverride(@Valid @RequestBody OverrideCreate body) {
        progression.override(SecurityUtils.currentUserId(), body.targetId(), body.afterValue(),
                body.reason());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
