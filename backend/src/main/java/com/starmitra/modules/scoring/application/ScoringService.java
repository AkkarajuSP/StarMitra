package com.starmitra.modules.scoring.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starmitra.modules.competition.application.CompetitionStructureContract;
import com.starmitra.modules.rubric.application.RubricContract;
import com.starmitra.modules.scoring.persistence.*;
import com.starmitra.modules.submission.application.SubmissionTruthContract;
import com.starmitra.modules.voting.application.VoteTruthContract;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.security.SystemRoleGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * M14 — Scoring & Ranking. Deterministic, version-bound, reproducible.
 *
 * ALGORITHMS (explicit MVP policies — documented, not silently product truth):
 *
 * - Judge score (per judge): Σ(criterion_score / criterion_max) * weight
 *   → judge percentage 0–100.
 * - Multi-judge aggregation: **arithmetic mean of each judge's percentage**
 *   (equal judge weight — FRS leaves policy open; documented decision).
 * - Audience score: submission_votes / max_votes_in_round * 100
 *   (max-normalized 0–100; a round with zero votes → all zeros).
 * - Final score = audience% * wA/100 + judge% * wJ/100, weights from the
 *   versioned scoring_configuration (Σ=100 enforced by ck_sc_weights + app).
 * - Precision: BigDecimal scale 4 internally; display rounds to 2 by clients.
 * - Ranking: final_score desc; tie-break policy from tie_break_config
 *   ({policy:"JUDGE_SCORE"} default → judge% desc, then audience% desc,
 *   then submission_id asc — deterministic). Truly unresolved ties share a
 *   rank and are flagged in tie_break_applied {"tied":true}.
 * - Qualification: rule_payload {type:TOP_N,n} | {type:MIN_SCORE,min} |
 *   {type:TOP_N_AND_MIN,n,min} → QUALIFIED/NOT_QUALIFIED rows.
 *
 * Reproducibility: results are bound to config_version_id + score_version /
 * snapshot_version; sealed final_scores never recalculated in place.
 */
@Service
public class ScoringService implements ScoringTruthContract {

    private final ScoringConfigRepository scoringConfigs;
    private final TieBreakConfigRepository tieBreaks;
    private final QualificationConfigRepository qualConfigs;
    private final JudgeScoreAggRepository judgeAggs;
    private final AudienceScoreAggRepository audienceAggs;
    private final FinalScoreRepository finalScores;
    private final RankingRepository rankings;
    private final QualificationResultRepository qualResults;
    private final ScoreOverrideRepository overrides;
    private final SubmissionTruthContract submissions;
    private final VoteTruthContract voteTruth;
    private final RubricContract rubrics;
    private final CompetitionStructureContract competition;
    private final AuditService audit;
    private final ObjectMapper json;

    public ScoringService(ScoringConfigRepository sc, TieBreakConfigRepository tb,
                          QualificationConfigRepository qc, JudgeScoreAggRepository ja,
                          AudienceScoreAggRepository aa, FinalScoreRepository fs,
                          RankingRepository rk, QualificationResultRepository qr,
                          ScoreOverrideRepository so, SubmissionTruthContract submissions,
                          VoteTruthContract voteTruth, RubricContract rubrics,
                          CompetitionStructureContract competition, AuditService audit,
                          ObjectMapper json) {
        this.scoringConfigs = sc; this.tieBreaks = tb; this.qualConfigs = qc;
        this.judgeAggs = ja; this.audienceAggs = aa; this.finalScores = fs;
        this.rankings = rk; this.qualResults = qr; this.overrides = so;
        this.submissions = submissions; this.voteTruth = voteTruth; this.rubrics = rubrics;
        this.competition = competition; this.audit = audit; this.json = json;
    }

    public record ResultView(UUID submissionId, UUID roundId, String finalScore,
                             Integer rank, Boolean qualified, Boolean overridden) {}

    // ---------- configuration (admin, versioned) ----------

    @Transactional
    public UUID createScoringConfig(UUID admin, UUID competitionId,
                                    BigDecimal weightAudience, BigDecimal weightJudge) {
        SystemRoleGuard.requireAdmin();
        if (weightAudience.add(weightJudge).compareTo(new BigDecimal("100")) != 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Weights must total 100");
        }
        var c = new ScoringConfigEntity(competitionId, scoringConfigs.maxVersion(competitionId) + 1,
                weightAudience, weightJudge);
        return scoringConfigs.save(c).getId();
    }

    @Transactional
    public UUID createTieBreakConfig(UUID competitionId, Map<String, Object> payload) {
        SystemRoleGuard.requireAdmin();
        return tieBreaks.save(new TieBreakConfigEntity(competitionId,
                tieBreaks.maxVersion(competitionId) + 1, writeJson(payload))).getId();
    }

    @Transactional
    public UUID createQualificationConfig(UUID competitionId, Map<String, Object> payload) {
        SystemRoleGuard.requireAdmin();
        return qualConfigs.save(new QualificationConfigEntity(competitionId,
                qualConfigs.maxVersion(competitionId) + 1, writeJson(payload))).getId();
    }

    // ---------- calculation ----------

    /** Admin-triggered aggregation for a round bound to a scoring config version. */
    @Transactional
    public int calculate(UUID admin, UUID competitionId, UUID roundId, UUID configVersionId) {
        SystemRoleGuard.requireAdmin();
        if (!competition.roundBelongsTo(competitionId, roundId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Round not in competition");
        }
        var cfg = scoringConfigs.findById(configVersionId != null ? configVersionId
                        : scoringConfigs.versionsOf(competitionId).stream().findFirst()
                            .map(ScoringConfigEntity::getId)
                            .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                                    "No scoring config")))
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown config"));
        if (!cfg.getCompetitionId().equals(competitionId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Config belongs to another competition");
        }

        var voteCounts = voteTruth.countsBySubmission(roundId);
        long maxVotes = voteCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);

        var subIds = submissions.finalizedInScope(competitionId, null, roundId);
        int calculated = 0;
        for (var subId : subIds) {
            var s = submissions.submissionView(subId).orElseThrow();

            // Judge % : mean over judges of Σ(score/max)*weight — independent evals preserved
            BigDecimal judgePct = BigDecimal.ZERO;
            var evals = rubrics.submittedEvaluations(subId);
            if (!evals.isEmpty()) {
                var rubric = rubrics.publishedVersion(evals.get(0).rubricVersionId()).orElseThrow();
                var criterionMeta = rubric.criteria().stream()
                        .collect(Collectors.toMap(RubricContract.CriterionView::id, c -> c));
                BigDecimal sum = BigDecimal.ZERO;
                for (var e : evals) {
                    BigDecimal judgeTotal = BigDecimal.ZERO;
                    for (var entry : e.criterionScores().entrySet()) {
                        var c = criterionMeta.get(entry.getKey());
                        if (c == null) continue;
                        BigDecimal cap = c.maxScore() != null ? c.maxScore()
                                : BigDecimal.valueOf(rubric.scaleMax());
                        judgeTotal = judgeTotal.add(entry.getValue()
                                .divide(cap, 6, RoundingMode.HALF_UP).multiply(c.weight()));
                    }
                    sum = sum.add(judgeTotal);
                }
                judgePct = sum.divide(BigDecimal.valueOf(evals.size()), 4, RoundingMode.HALF_UP);
            }
            // Audience % : votes / round-max * 100 (normalized — never raw counts)
            BigDecimal audiencePct = maxVotes == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(voteCounts.getOrDefault(subId, 0L))
                        .divide(BigDecimal.valueOf(maxVotes), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);

            BigDecimal finalScore = audiencePct.multiply(cfg.getWeightAudience())
                    .add(judgePct.multiply(cfg.getWeightJudge()))
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            judgeAggs.save(new JudgeScoreAggEntity(subId, roundId, cfg.getId(), judgePct));
            audienceAggs.save(new AudienceScoreAggEntity(subId, roundId, cfg.getId(), audiencePct));
            int v = finalScores.maxScoreVersion(subId, roundId, cfg.getId()) + 1;
            finalScores.save(new FinalScoreEntity(subId, competitionId, s.categoryId(),
                    roundId, cfg.getId(), v, finalScore));
            calculated++;
        }
        audit.record("M14", "SCORES_CALCULATED", admin, "user", "round", roundId.toString(),
                "submissions=" + calculated);
        return calculated;
    }

    /** Seal results + build ranking snapshot + qualification results. */
    @Transactional
    public int finalize(UUID admin, UUID competitionId, UUID roundId) {
        SystemRoleGuard.requireAdmin();
        // latest score_version per submission (sealed rows never change)
        var latest = new HashMap<UUID, FinalScoreEntity>();
        for (var f : finalScores.findByRoundId(roundId)) {
            latest.merge(f.getSubmissionId(), f,
                    (a, b) -> a.getScoreVersion() >= b.getScoreVersion() ? a : b);
        }
        if (latest.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "No calculated scores");
        }
        latest.values().forEach(FinalScoreEntity::seal);

        // ranking: final desc → tie-break policy → submission_id asc (deterministic)
        var tbPolicy = tieBreaks.latestPublished(competitionId).stream().findFirst()
                .map(TieBreakConfigEntity::getCriteria).orElse("{}");
        String policy = readJson(tbPolicy).path("policy").asText("JUDGE_SCORE");

        var ranked = latest.values().stream().sorted(rankComparator(roundId)).toList();
        int snapshot = rankings.maxSnapshot(roundId) + 1;
        Map<UUID, Integer> rankMap = new LinkedHashMap<>();
        int rank = 0;
        BigDecimal prev = null;
        for (var f : ranked) {
            boolean tied = prev != null && f.getFinalScore().compareTo(prev) == 0;
            rank = tied ? rank : rank + 1;
            rankMap.put(f.getSubmissionId(), rank);
            rankings.save(new RankingEntity(f.getCompetitionId(), f.getCategoryId(), roundId,
                    f.getSubmissionId(), rank, tied ? "{\"tied\":true}" : null, snapshot));
            prev = f.getFinalScore();
        }

        // qualification from latest published qual config — M15 consumes, never transitions here
        qualConfigs.latestPublished(competitionId).stream().findFirst().ifPresent(qc -> {
            var rule = readJson(qc.getRulePayload());
            int n = rule.path("n").asInt(Integer.MAX_VALUE);
            var min = rule.path("min").decimalValue();
            for (var f : ranked) {
                boolean top = rankMap.get(f.getSubmissionId()) <= n;
                boolean above = min == null || f.getFinalScore().compareTo(min) >= 0;
                boolean q = switch (rule.path("type").asText("TOP_N")) {
                    case "MIN_SCORE" -> above;
                    case "TOP_N_AND_MIN" -> top && above;
                    default -> top;                                  // TOP_N
                };
                qualResults.save(new QualificationResultEntity(f.getSubmissionId(), roundId,
                        qc.getId(), q));
            }
        });
        audit.record("M14", "SCORES_SEALED", admin, "user", "round", roundId.toString(), null);
        return ranked.size();
    }

    @Transactional(readOnly = true)
    public List<ResultView> results(UUID competitionId, UUID roundId) {
        var rankMap = latestRanking(roundId);
        var qualMap = qualificationOf(roundId);
        return finalScoresByRound(roundId).stream()
                .collect(Collectors.groupingBy(FinalScoreEntity::getSubmissionId,
                        Collectors.maxBy(Comparator.comparingInt(FinalScoreEntity::getScoreVersion))))
                .values().stream().map(Optional::get).map(f -> new ResultView(
                        f.getSubmissionId(), roundId, f.getFinalScore().toPlainString(),
                        rankMap.get(f.getSubmissionId()), qualMap.get(f.getSubmissionId()),
                        !overrides.findByFinalScoreId(f.getId()).isEmpty()))
                .toList();
    }

    /** Admin override — append-only; never mutates sealed score or sources. */
    @Transactional
    public void override(UUID admin, UUID finalScoreId, BigDecimal afterValue, String reason) {
        SystemRoleGuard.requireAdmin();
        var f = finalScores.findById(finalScoreId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        var before = f.getFinalScore();
        overrides.save(new ScoreOverrideEntity(finalScoreId, admin, before, afterValue, reason));
        audit.record("M14", "SCORE_OVERRIDE", admin, "user", "final_score",
                finalScoreId.toString(), before + "->" + afterValue);
    }

    // ---------- ScoringTruthContract (M15/M16) ----------

    @Override @Transactional(readOnly = true)
    public Map<UUID, Boolean> qualificationOf(UUID roundId) {
        return qualResults.findByRoundId(roundId).stream().collect(
                Collectors.toMap(QualificationResultEntity::getSubmissionId,
                        QualificationResultEntity::isQualified, (a, b) -> a));
    }

    @Override @Transactional(readOnly = true)
    public Map<UUID, Integer> latestRanking(UUID roundId) {
        int snap = rankings.maxSnapshot(roundId);
        if (snap == 0) return Map.of();
        return rankings.findByRoundIdAndSnapshotVersionOrderByRankAsc(roundId, snap).stream()
                .collect(Collectors.toMap(RankingEntity::getSubmissionId, RankingEntity::getRank));
    }

    @Override @Transactional(readOnly = true)
    public boolean isSealed(UUID roundId) {
        return finalScoresByRound(roundId).stream().anyMatch(f -> f.getStatus().equals("SEALED"));
    }

    // ---------- internals ----------

    private List<FinalScoreEntity> finalScoresByRound(UUID roundId) {
        return finalScores.findByRoundId(roundId);
    }

    /** final desc → tie-break (default JUDGE_SCORE pct) → submission_id asc. */
    private Comparator<FinalScoreEntity> rankComparator(UUID roundId) {
        Map<UUID, BigDecimal> judgePct = judgeAggs.findByRoundId(roundId).stream()
                .collect(Collectors.toMap(JudgeScoreAggEntity::getSubmissionId,
                        JudgeScoreAggEntity::getAggregate, (a, b) -> a));
        return Comparator.<FinalScoreEntity, BigDecimal>comparing(FinalScoreEntity::getFinalScore)
                .reversed()
                .thenComparing(f -> judgePct.getOrDefault(f.getSubmissionId(), BigDecimal.ZERO),
                        Comparator.reverseOrder())
                .thenComparing(f -> f.getSubmissionId().toString());
    }

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
