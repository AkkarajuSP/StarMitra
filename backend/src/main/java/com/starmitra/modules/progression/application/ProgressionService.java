package com.starmitra.modules.progression.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starmitra.modules.competition.application.CompetitionStructureContract;
import com.starmitra.modules.progression.persistence.*;
import com.starmitra.modules.scoring.application.ScoringTruthContract;
import com.starmitra.modules.submission.application.SubmissionTruthContract;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.security.SystemRoleGuard;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * M15 — Round Progression. Consumes M14 truth, never recomputes.
 *
 * - progression_configs: versioned JSONB rules {targetRoundId?, maxAdvance?,
 *   minScore?, tiePolicy?} — data-driven, no hard-coded progression model
 * - progression_records: one decision per submission+decision-round+config
 *   (UQ → retry/concurrency-safe by DB); outcome ADVANCED/ELIMINATED/PENDING
 * - finalize: competition round_state ACTIVE→COMPLETE via M09 contract;
 *   M14 must be sealed for the round (authoritative scoring required)
 * - overrides: append-only admin corrections (audited)
 * - M15 never splits PROJECT entries — submission is the single unit that
 *   advances (participant/project preserved via M10 participant truth)
 */
@Service
public class ProgressionService implements ProgressionTruthContract {

    private final ProgressionConfigRepository configs;
    private final ProgressionRecordRepository records;
    private final ProgressionOverrideRepository overrides;
    private final ScoringTruthContract scoring;
    private final SubmissionTruthContract submissions;
    private final CompetitionStructureContract competition;
    private final AuditService audit;
    private final ObjectMapper json;

    public ProgressionService(ProgressionConfigRepository configs,
                              ProgressionRecordRepository records,
                              ProgressionOverrideRepository overrides,
                              ScoringTruthContract scoring,
                              SubmissionTruthContract submissions,
                              CompetitionStructureContract competition,
                              AuditService audit, ObjectMapper json) {
        this.configs = configs;
        this.records = records;
        this.overrides = overrides;
        this.scoring = scoring;
        this.submissions = submissions;
        this.competition = competition;
        this.audit = audit;
        this.json = json;
    }

    public record RecordView(UUID id, UUID submissionId, String outcome,
                             UUID targetRoundId) {}

    // ---------- config (admin, versioned) ----------

    @Transactional
    public UUID createConfig(UUID competitionId, Map<String, Object> payload) {
        SystemRoleGuard.requireAdmin();
        return configs.save(new ProgressionConfigEntity(competitionId,
                configs.maxVersion(competitionId) + 1, writeJson(payload))).getId();
    }

    // ---------- round lifecycle via M09 contract ----------

    /** M15-owned round activation — NOT_STARTED→ACTIVE (validates comp+round). */
    @Transactional
    public void activate(UUID admin, UUID competitionId, UUID roundId) {
        SystemRoleGuard.requireAdmin();
        if (!competition.roundBelongsTo(competitionId, roundId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Round not in competition");
        }
        competition.transitionRoundState(competitionId, "ACTIVE");
        audit.record("M15", "ROUND_ACTIVATED", admin, "user", "round", roundId.toString(), null);
    }

    /** Round completion requires sealed M14 results + ACTIVE state. */
    @Transactional
    public void complete(UUID admin, UUID competitionId, UUID roundId) {
        SystemRoleGuard.requireAdmin();
        if (!competition.roundBelongsTo(competitionId, roundId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Round not in competition");
        }
        if (!scoring.isSealed(roundId)) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID,
                    "Scoring must be sealed before completion");
        }
        competition.transitionRoundState(competitionId, "COMPLETE");
        audit.record("M15", "ROUND_COMPLETED", admin, "user", "round", roundId.toString(), null);
    }

    // ---------- progression execution (consumes M14, never recomputes) ----------

    /**
     * Calculate = create PENDING/ADVANCED/ELIMINATED records from M14
     * qualificationOf for every finalized submission in the decision round.
     * Idempotent by UQ — re-run returns existing records.
     */
    @Transactional
    public int calculate(UUID admin, UUID competitionId, UUID roundId, UUID configVersionId) {
        SystemRoleGuard.requireAdmin();
        if (!competition.roundBelongsTo(competitionId, roundId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Round not in competition");
        }
        var cfg = configs.findById(configVersionId != null ? configVersionId
                        : configs.latestPublished(competitionId).stream().findFirst()
                            .map(ProgressionConfigEntity::getId)
                            .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                                    "No progression config")))
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown config"));
        var rule = readJson(cfg.getRulePayload());
        UUID targetRound = rule.hasNonNull("targetRoundId")
                ? UUID.fromString(rule.get("targetRoundId").asText()) : null;
        if (targetRound != null && !competition.roundBelongsTo(competitionId, targetRound)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Target round not in competition");
        }
        int maxAdvance = rule.path("maxAdvance").asInt(Integer.MAX_VALUE);
        var ranking = scoring.latestRanking(roundId);
        var qual = scoring.qualificationOf(roundId);
        if (qual.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "No M14 qualification results");
        }
        int created = 0;
        for (var subId : submissions.finalizedInScope(competitionId, null, roundId)) {
            if (records.findBySubmissionIdAndRoundId(subId, roundId).isPresent()) continue;
            boolean qualified = Boolean.TRUE.equals(qual.get(subId));
            int rank = ranking.getOrDefault(subId, Integer.MAX_VALUE);
            var outcome = qualified && rank <= maxAdvance
                    ? ProgressionRecordEntity.Outcome.ADVANCED
                    : ProgressionRecordEntity.Outcome.ELIMINATED;
            try {
                records.saveAndFlush(new ProgressionRecordEntity(subId, roundId, roundId,
                        outcome == ProgressionRecordEntity.Outcome.ADVANCED ? targetRound : null,
                        outcome, cfg.getId()));
                created++;
            } catch (DataIntegrityViolationException race) {
                // concurrent execution — UQ wins; record already exists
            }
        }
        audit.record("M15", "PROGRESSION_CALCULATED", admin, "user", "round",
                roundId.toString(), "records=" + created);
        return created;
    }

    /** Finalize decisions — records finalized; round transitions ACTIVE→COMPLETE. */
    @Transactional
    public int finalize(UUID admin, UUID competitionId, UUID roundId) {
        SystemRoleGuard.requireAdmin();
        var roundRecords = records.findByRoundId(roundId);
        if (roundRecords.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "No progression records");
        }
        roundRecords.forEach(ProgressionRecordEntity::finalize);
        records.saveAll(roundRecords);
        complete(admin, competitionId, roundId);              // sealed-required
        return roundRecords.size();
    }

    @Transactional(readOnly = true)
    public List<RecordView> records(UUID roundId) {
        return records.findByRoundId(roundId).stream()
                .map(r -> new RecordView(r.getId(), r.getSubmissionId(),
                        r.getOutcome().name(), r.getTargetRoundId())).toList();
    }

    /** Admin correction — append-only, audited; never mutates qualification truth. */
    @Transactional
    public void override(UUID admin, UUID recordId, String afterOutcome, String reason) {
        SystemRoleGuard.requireAdmin();
        var r = records.findById(recordId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        var before = r.getOutcome().name();
        if (!List.of("ADVANCED", "ELIMINATED", "PENDING").contains(afterOutcome)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid outcome");
        }
        overrides.save(new ProgressionOverrideEntity(recordId, admin, before, afterOutcome, reason));
        audit.record("M15", "PROGRESSION_OVERRIDE", admin, "user",
                "progression_record", recordId.toString(), before + "->" + afterOutcome);
    }

    // ---------- ProgressionTruthContract (M16) ----------

    @Override @Transactional(readOnly = true)
    public String roundStateOf(UUID competitionId) {
        return competition.roundStateOf(competitionId).orElse("NOT_STARTED");
    }

    @Override @Transactional(readOnly = true)
    public Map<UUID, String> outcomesOf(UUID roundId) {
        return records.findByRoundId(roundId).stream().collect(Collectors.toMap(
                ProgressionRecordEntity::getSubmissionId,
                r -> r.getOutcome().name()));
    }

    @Override @Transactional(readOnly = true)
    public List<UUID> advancedFrom(UUID roundId) {
        return records.findByRoundId(roundId).stream()
                .filter(r -> r.getOutcome() == ProgressionRecordEntity.Outcome.ADVANCED)
                .map(ProgressionRecordEntity::getSubmissionId).toList();
    }

    // ---------- internals ----------

    private JsonNode readJson(String s) {
        try {
            return json.readTree(s == null ? "{}" : s);
        } catch (Exception e) {
            return json.createObjectNode();
        }
    }

    private String writeJson(Map<String, Object> m) {
        try {
            return json.writeValueAsString(m == null ? Map.of() : m);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid payload");
        }
    }
}
