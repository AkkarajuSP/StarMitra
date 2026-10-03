# Module Design — 14: Scoring & Ranking

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 14)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–13 without modification. Round progression (M15) and leaderboard presentation (M16) are downstream contracts, not designed here.

## 1. Purpose

Own the **authoritative scoring/ranking outcome**: aggregate judge evaluations + audience votes through configurable weighting into final scores, deterministic ranking, tie-handling, and qualification *inputs/results* — all reproducible, versioned, and auditable `[FRS §22–§24]`. The "what the result is" module — not "what happens next" (M15) or "how it's displayed" (M16).

## 2. Three Weight Concepts — never conflated

| Concept | Owner | Meaning |
|---------|-------|---------|
| **A. Rubric criterion weights** | M13 | weight *within* a judge's evaluation |
| **B. Audience-vs-judge weighting** | **M14** | component weights *in the final score* (configurable, totals 100%) |
| **C. Ranking/qualification rules** | **M14** (rules) + **M15** (execution) | how scores rank/qualify vs. what happens next |

**No fixed formula invented** — 40/60, 50/50, any percentage, any ranking algorithm = configurable, never hard-coded.

## 3. Module Boundary

| Owns | Does NOT own |
|------|--------------|
| `ScoringConfiguration`(+version), `JudgeScoreAggregation`, `AudienceScoreAggregation`, `FinalScore`, `Ranking`, `TieBreakConfiguration`, `QualificationResult`/`QualificationInput`, `ScoreOverride` | Competition/Category/Round (M09), Submission (M10), Vote records (M11), Judge/assignment (M12), Rubric/Evaluation (M13), progression state machine (M15), leaderboard presentation (M16), authn/authz (M01) |

## 4. Source vs Derived — strict boundary

| Source (authoritative, upstream) | Derived (M14, rebuildable) |
|----------------------------------|----------------------------|
| finalized Submission (M10) | judge aggregate |
| valid Vote records (M11) | audience aggregate |
| JudgeEvaluation + criterion scores (M13) | weighted FinalScore, Ranking, QualificationResult |

**Derived data never replaces source records** — always reproducible from source + versioned config.

## 5. Judge Score Aggregation

M13 supplies: rubric version, criteria, criterion weights, criterion scores, evaluation state. M14 consumes valid finalized evaluations.

Rules: eligible evaluations = finalized + valid + assignment-valid; incomplete/invalidated/duplicate evaluations excluded *(policy per eval state)*; revoked assignments handled per config *(open)*; different rubric versions across judges *(open — if permitted, aggregation must reconcile or be disallowed)*; precision/rounding = **open**; aggregation runs at configured finalize point. **Judge independence preserved** — aggregation reads stored evaluations; never exposes one judge's eval to another pre-release.

## 6. Audience Score Aggregation

M11 supplies authoritative valid Vote records. **Vote → audience score formula = OPEN product decision** — raw count vs normalized vs percentage vs configurable model; normalization across unequal volumes = open; timing/finalization boundary = open. **Never modify Vote records; cached/derived counts never authoritative.**

## 7. Audience + Judge Weighting

`FinalScore = f(audience_component × weightA, judge_component × weightJ)` where `weightA + weightJ = 100%` exactly — both configurable. Extensible to future components without over-engineering MVP. Distinct from criterion weights (M13) and aggregation internals.

## 8. FinalScore — authoritative boundary

- **When calculated:** at configured finalize/scoring point (triggered by close/finalization job per ADR-012)
- **Provisional vs final:** provisional while inputs open; **final** when inputs + config finalized
- **Inputs:** finalized submission + valid votes + valid evals + rubric version(s) + scoring-config version + tie-break version + qualification version
- **Persisted vs derived:** FinalScore persisted as authoritative result *(derived but sealed — reproducible)*
- **Recalculation:** recompute on upstream invalidation; produces new result record preserving prior + audit
- **Reproducibility:** recomputable from inputs + versioned config — never silently different
- **Manual modification:** only via authorized override (§11) — never direct edit

## 9. Ranking

Derived ordering over finalized scores: deterministic, reproducible, scoped to competition/category/round per FRS. Equal scores → tie-break path (§9 below). **Ranking snapshot vs recalculation:** both supported — snapshot for published results; recompute produces new snapshot + audit. Never modifies source submissions/votes/evals.

## 10. Tie-Break Rules — configurable, deterministic

`TieBreakConfiguration`: ordered criteria list — **versioned, deterministic, auditable**. Example criteria (audience votes, specific judge criterion, recency) are **illustrative only — none hard-coded as defaults**. Order + criteria = open product decision. Tie-break *execution* is part of ranking; outcome recorded.

## 11. Qualification Boundary — M14 vs M15

| Module | Answers |
|--------|---------|
| **M14** | "Given finalized scores + qualification config, entry **qualifies / does not qualify**" — produces `QualificationResult`/`QualificationInput` |
| **M15** | "Advance/eliminate/retain the entry per progression rules" — owns the state transition |

Qualification config supports **threshold-based + count-based** + FRS-supported modes; unsupported modes not invented. `QualificationResult` = scoring-side output; M15 consumes it — no progression state machine here.

## 12. Authorized Overrides — exceptional + auditable

`ScoreOverride`: who *(admin authorization matrix = open — not invented)*, what overridden (final score/rank/qualification — **not** source Vote/JudgeEvaluation), **mandatory reason**, before/after values, actor, timestamp, affected competition/category/round/submission, audit ref, recalculation consequence. **Overrides never overwrite source records** — they adjust the derived result with full trail.

## 13. Versioning / Reproducibility

`ScoringConfiguration` (+ `TieBreakConfiguration`, `QualificationConfiguration`) are **versioned**. A result is reproducible from: competition + category/round + finalized submission + valid votes + valid evals + rubric version(s) + scoring-config version + tie-break version + qualification version + overrides. **Published config is immutable** — changes produce new versions; historical results stay explainable.

## 14. Round / Competition Boundaries

M14 consumes competition/category/round config from M09 (refs); produces outcomes for M15/M16. No competition lifecycle or round state duplicated.

## 15. Multi-Talent / Team Rules — preserved

`TalentSkill`/`ProjectContributionRole` never authorize scoring access; multi-skill users score as entries; **team/project = ONE entry** — votes attach to it, scoring/ranking applies to it, **no contributor-level scoring** unless FRS requires *(not currently)*.

## 16. Data Integrity

PostgreSQL authoritative. Invariants: scoring-config weights = exactly 100%; one authoritative config per scope+version; deterministic ranking; ordered tie-break; consistent qualification; override audit complete. **No** Redis/Kafka/RabbitMQ/ES/K8s/separate-scoring-service.

## 17. Failure / Recalculation

Incomplete eval → excluded + flagged; invalid vote → excluded from count; eval/vote invalidated → dependent result flagged + recalculated (new versioned result); late changes per policy *(open)*; concurrent finalization → state guard; duplicate scoring request → idempotent; job retry → safe re-run (idempotent recompute). **In-memory never authoritative.**

## 18. Downstream Contract → M15

`{competitionId, categoryId, roundId, finalizedScores, ranking, qualificationResults, tieOutcomes, scoringConfigVersion, auditRefs}` → M15 advances/eliminates/retains entries + owns progression state machine.

## 19. Downstream Contract → M16

`{competitionId, categoryId, roundId, rankedEntries, finalScores, ranks, qualificationStatus, releasedStatus}` → M16 owns leaderboard read-model/presentation (not scoring).

## 20. API Boundary (conceptual — ADR-007)

`POST /api/v1/scoring-config` (configure) · `POST /{id}/finalize` · `POST /api/v1/scoring/calculate` (trigger) · `GET /api/v1/scoring/{contextId}/result` · `GET /api/v1/scoring/{contextId}/ranking` · `POST /api/v1/scoring/{resultId}/override` (authorized) · `GET /{resultId}/explanation` (audit/explain). DTOs, Problem Details, correlationId, idempotency, backend authz. `/api/v1/scoring` namespace.

## 21. Audit / Explainability

Result explanation contract (conceptual — audit data, not a second source of truth):

```text
Submission → JudgeEvaluations → CriterionScores×Weights → JudgeAggregate
         → AudienceVoteAggregate → ×Weights → FinalScore
         → TieBreak → Rank → Qualification → Overrides(if any)
```

Audited: config create/publish/change, scoring trigger, recalculation, override, result finalize, unauthorized access. `AuditLog` separate from telemetry.

## 22. Observability

Scoring-job metrics (runtime, items processed, failures, recalculation count), correlation IDs across chain, result-version tracking, override alerts. Technical telemetry ≠ audit ≠ analytics.

## 23. Open Product Owner Decisions

Judge aggregation method (mean/median/etc.), audience score formula + normalization, precision/rounding, config lifecycle, finalize timing, ranking semantics, tie-break criteria/order, threshold semantics, count semantics, override authorization matrix, result release timing, invalidation/recalculation policy.

## 24. ADR Validation

- **ADR-003 PG:** authoritative store for config + results + audit ✓
- **ADR-007 API:** REST + OpenAPI + DTOs + Problem Details + idempotency ✓
- **ADR-010 Cache:** cache never result-authority; rebuildable derived data ✓
- **ADR-012 Cloud/Jobs:** scoring runs via app-managed durable-PG jobs ✓
- **ADR-013 Analytics:** analytics never generates competition outcomes ✓

## 25. Data Model (conceptual)

| Entity | Authoritative? | Purpose |
|--------|---------------|---------|
| `ScoringConfiguration` | ✅ (versioned) | weighting + context + lifecycle |
| `TieBreakConfiguration` | ✅ (versioned) | ordered criteria |
| `QualificationConfiguration` | ✅ (versioned) | threshold/count rules |
| `JudgeScoreAggregation` | derived | per-submission judge aggregate |
| `AudienceScoreAggregation` | derived | per-submission audience aggregate |
| `FinalScore` | ✅ (sealed result) | reproducible weighted outcome |
| `Ranking` | derived snapshot | deterministic order |
| `QualificationResult` | derived | qualifies/not + inputs |
| `ScoreOverride` | ✅ (audit) | before/after + reason + actor |

## 26. Acceptance Validation

Aggregation configurable ✓ · 3 weight-concepts separated ✓ · weights total 100% ✓ · FinalScore reproducible ✓ · ranking deterministic ✓ · tie-break versioned ✓ · qualification→M15 boundary ✓ · overrides auditable + never overwrite source ✓ · team=one-entry + votes-not-split ✓ · skills/roles never authorize ✓ · PG authoritative ✓ · no infra ✓ · no impl ✓

## 27. Traceability

- **FRS:** §22 scoring · §23 ranking/ties · §24 qualification · §18 team-vote rule · §30 audit · BR-2
- **ADRs:** ADR-001 boundary · ADR-003 PG · ADR-007 API · ADR-010 cache · ADR-012 jobs · ADR-013 analytics
- **Modules:** M09 config · M10 submission · M11 votes · M12 scope · M13 evals → M15/M16 consumers · Moderation · Notifications
