# M14 — Scoring & Ranking — Implementation

**Slice:** fifteenth business implementation. **Status: complete.** M14 owns scoring/ranking/qualification truth — never round transitions (M15), never leaderboard views (M16).

## Ownership (9 V1.12 tables, mapped verbatim)

`scoring_configurations` (ck weights=100) · `tie_break_configurations` · `qualification_configurations` · `judge_score_aggregations` · `audience_score_aggregations` · `final_scores` (versioned, sealed) · `rankings` (snapshots) · `qualification_results` · `score_overrides` (append-only).

## Contract surface (8 ops, unchanged)

createScoringConfig · createTieBreakConfig · createQualificationConfig · calculateScores (202) · finalizeScores (seal+rank+qualify) · getResults · createScoreOverride.

## Algorithms (explicit MVP policies — documented, defensible, not hidden)

- **Judge %** — per judge: Σ(score/criterion_max)·weight → 0–100; **multi-judge = arithmetic mean** (equal judge weight).
- **Audience %** — submission_votes / round_max_votes × 100 (max-normalized; PROJECT votes stay one target).
- **Final** — audience%·wA/100 + judge%·wJ/100; BigDecimal scale-4 internally.
- **Ranking** — final desc → tie-break config `policy` (default JUDGE_SCORE pct desc) → audience% → submission_id asc; unresolved ties share rank + `{"tied":true}` marker.
- **Qualification** — rule types TOP_N / MIN_SCORE / TOP_N_AND_MIN; QUALIFIED flag persisted for M15.
- **Reproducibility** — every row bound to config_version + score_version/snapshot_version; sealed rows never recalculated in place; recalc → new version.

## Overrides

`POST /scoring/overrides` — ROLE_ADMIN only; append-only `score_overrides` (actor, before, after, reason, ts); final_scores row untouched; results expose `overridden` flag; `SCORE_OVERRIDE` audit.

## Cross-module

| Dir | Contract |
|---|---|
| M14 → M09 | `roundBelongsTo` |
| M14 → M10 | `finalizedInScope` + `submissionView` |
| M14 → M11 | `VoteTruthContract.countsBySubmission` |
| M14 → M13 | `RubricContract.publishedVersion` + `submittedEvaluations` (new) |
| M15 → M14 | `ScoringTruthContract.qualificationOf` |
| M16 → M14 | `ScoringTruthContract.latestRanking`, `isSealed` |

## Testing

`ScoringFlowIT` 5 real-PG17: weighted scoring + ranking (40/60), weights-100 rejection, TOP_N qualification, append-only audited override preserving sealed row, versioned re-calc never mutates sealed. Suite: **167**.

## Known follow-ups

- `results(roundId=null)` returns empty (contract param optional; filter by comp is M16 concern).
- Tie-break criterion-level policies (policy name resolved; criteria-level tiebreak reserved).
- Score display rounding = client concern (internal scale 4).
