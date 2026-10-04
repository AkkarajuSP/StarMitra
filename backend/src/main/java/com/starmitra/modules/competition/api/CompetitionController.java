package com.starmitra.modules.competition.api;

import com.starmitra.modules.competition.application.CompetitionService;
import com.starmitra.platform.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/** /api/v1/competitions — M09. */
@RestController
@RequestMapping("/api/v1/competitions")
public class CompetitionController {

    private final CompetitionService competitions;
    private final ObjectMapper json;

    public CompetitionController(CompetitionService competitions, ObjectMapper json) {
        this.competitions = competitions;
        this.json = json;
    }

    @GetMapping
    public ResponseEntity<CompetitionDtos.CompetitionPage> listCompetitions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        var p = competitions.list(SecurityUtils.currentUserId(), status, cursor, limit);
        return ResponseEntity.ok(new CompetitionDtos.CompetitionPage(
                p.items().stream().map(this::toDto).toList(),
                new CompetitionDtos.PageMeta(p.page().nextCursor(), p.page().hasMore(), p.page().total())));
    }

    @PostMapping
    public ResponseEntity<CompetitionDtos.Competition> createCompetition(
            @Valid @RequestBody CompetitionDtos.CompetitionCreate body) {
        var c = competitions.create(SecurityUtils.currentUserId(),
                new CompetitionService.CompetitionCommand(body.title(), body.description()));
        return ResponseEntity.status(HttpStatus.CREATED).eTag(String.valueOf(c.version())).body(toDto(c));
    }

    @GetMapping("/{competitionId}")
    public ResponseEntity<CompetitionDtos.Competition> getCompetition(@PathVariable UUID competitionId) {
        var c = competitions.get(SecurityUtils.currentUserId(), competitionId);
        return ResponseEntity.ok().eTag(String.valueOf(c.version())).body(toDto(c));
    }

    @PutMapping("/{competitionId}")
    public ResponseEntity<CompetitionDtos.Competition> updateCompetition(
            @PathVariable UUID competitionId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody CompetitionDtos.CompetitionCreate body) {
        var c = competitions.update(SecurityUtils.currentUserId(), competitionId,
                new CompetitionService.CompetitionCommand(body.title(), body.description()), ifMatch);
        return ResponseEntity.ok().eTag(String.valueOf(c.version())).body(toDto(c));
    }

    @PostMapping("/{competitionId}/categories")
    public ResponseEntity<CompetitionDtos.Category> createCategory(
            @PathVariable UUID competitionId, @Valid @RequestBody CompetitionDtos.CategoryCreate body) {
        var c = competitions.createCategory(SecurityUtils.currentUserId(), competitionId,
                new CompetitionService.CategoryCommand(body.name(), body.description(), body.skillIds()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new CompetitionDtos.Category(c.id(), c.name()));
    }

    @PostMapping("/{competitionId}/rounds")
    public ResponseEntity<CompetitionDtos.Round> createRound(
            @PathVariable UUID competitionId, @Valid @RequestBody CompetitionDtos.RoundCreate body) {
        var r = competitions.createRound(SecurityUtils.currentUserId(), competitionId,
                new CompetitionService.RoundCommand(body.sequence(), body.name(),
                        parseTime(body.startAt()), parseTime(body.endAt()),
                        body.voteConfigId(), body.rubricVersionId(), body.progressionConfigId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new CompetitionDtos.Round(
                r.id(), r.sequence(), r.name(), r.startAt(), r.endAt(),
                r.voteConfigId(), r.rubricVersionId()));
    }

    @PostMapping("/{competitionId}/eligibility-rules")
    public ResponseEntity<Void> addEligibilityRule(@PathVariable UUID competitionId,
            @Valid @RequestBody CompetitionDtos.EligibilityRuleCreate body) {
        competitions.addEligibilityRule(SecurityUtils.currentUserId(), competitionId,
                body.ruleType(), body.ruleParams() == null ? null : writeJson(body.ruleParams()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{competitionId}/submission-config")
    public ResponseEntity<Void> putSubmissionConfig(
            @PathVariable UUID competitionId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody CompetitionDtos.SubmissionConfig body) {
        competitions.putSubmissionConfig(SecurityUtils.currentUserId(), competitionId,
                writeJson(body.config()), ifMatch);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{competitionId}/participants")
    public ResponseEntity<CompetitionDtos.ParticipantPage> listParticipants(
            @PathVariable UUID competitionId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        var p = competitions.listParticipants(SecurityUtils.currentUserId(), competitionId, cursor, limit);
        return ResponseEntity.ok(new CompetitionDtos.ParticipantPage(
                p.items().stream().map(this::toParticipantDto).toList(),
                new CompetitionDtos.PageMeta(p.page().nextCursor(), p.page().hasMore(), p.page().total())));
    }

    @PostMapping("/{competitionId}/participants/register")
    public ResponseEntity<CompetitionDtos.Participant> registerParticipant(
            @PathVariable UUID competitionId, @Valid @RequestBody CompetitionDtos.ParticipantRegister body) {
        var p = competitions.register(SecurityUtils.currentUserId(), competitionId,
                body.participantType(), body.categoryId(), body.projectId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toParticipantDto(p));
    }

    private CompetitionDtos.Competition toDto(CompetitionService.CompetitionView c) {
        return new CompetitionDtos.Competition(c.id(), c.title(), c.configStatus(),
                c.participationStatus(), c.roundState(), c.version());
    }

    private CompetitionDtos.Participant toParticipantDto(CompetitionService.ParticipantView p) {
        return new CompetitionDtos.Participant(p.id(), p.participantType(), p.userId(),
                p.projectId(), p.status());
    }

    private OffsetDateTime parseTime(String s) { return s == null ? null : OffsetDateTime.parse(s); }

    private String writeJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid JSON payload");
        }
    }
}
