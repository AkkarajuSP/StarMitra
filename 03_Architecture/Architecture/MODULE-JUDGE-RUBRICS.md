# Module Design — 13: Judge Rubrics

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 13)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–12 without modification. Scoring/ranking aggregation is M14's domain — contract-referenced only.

## 1. Purpose

Own the **configurable, versioned evaluation rubric** — what criteria judges score against — plus the `JudgeEvaluation` record structure that references an exact published version. Not judge assignment (M12), not scoring aggregation (M14).

## 2. Core Model — five distinct concepts

`EvaluationTemplate` (rubric container) ≠ `EvaluationTemplateVersion` (immutable published snapshot) ≠ `EvaluationCriterion` (scored dimension) ≠ `JudgeEvaluation` (judge's completed evaluation — **owned here as the rubric's output record, structured by criterion scores**) ≠ `Score` (M14's aggregate). Configuration ≠ evaluation ≠ scoring.

## 3. FRS Scope `[FRS §20]` — traced

Configurable judge forms · evaluation template + category applicability · criteria add/remove/reorder · **weights total 100%** · configurable scoring scale (**1–10 is a *recommended example*, never hard-coded**) · comments · **immutable published versions** · independent judging until close.

## 4. Rubric Lifecycle

`Draft → Validated → Published → Active → Retired` *(proposed states — open)*. Editable in draft only; publish freezes; changes post-publish = new version; retire/deactivate allowed.

## 5. Versioning — critical

**Published `EvaluationTemplateVersion` is immutable** — criteria, weights, scale, applicability all frozen. Any change → new version. Every `JudgeEvaluation` stores `rubricVersionId` — historical evaluations trace to the exact version used. Never overwrite published definitions.

## 6. Criteria

`EvaluationCriterion`: name, description, weight, display-order, scoring-config ref, active flag. **Add/remove/reorder permitted in draft only.** No fixed criterion list — FRS examples are illustrative.

## 7. Weight Validation — 100% total (FRS-required)

Draft may hold incomplete weights; **publish requires total = 100%**; precision strategy defined *(decimal precision = open)*; **no silent normalization** — configured weights preserved exactly in the published version.

## 8. Scoring Scale — configurable

`{ min, max, step }` configurable per rubric — **1–10 is a suggested example, not a rule**; decimal vs integer = open. Validation enforces the configured scale at evaluation input.

## 9. Comments

FRS includes judge comments. **Granularity open:** criterion-level vs overall-evaluation comments — both supported in design; which is required = open product decision.

## 10. Applicability

`EvaluationTemplateVersion` references applicable context: `competitionId` + optional `categoryId` + optional `roundId` — refs to M09, never duplicated. Explicit-vs-inherited assignment = **open**; the most-specific applicable version wins *(precedence = open if overlapping)*.

## 11. Rubric Assignment to Judging — vs M12

| Module | Answers |
|--------|---------|
| M12 | **WHO** can judge **WHAT** (assignment scope) |
| M13 | **WHAT** criteria apply (rubric version) |

**Never:** JudgeExpertise or TalentSkill auto-selects a rubric. Rubric version is explicitly associated with the judging context; auto-selection = implementation option only.

## 12. JudgeEvaluation Boundary

`JudgeEvaluation`: judgeId, assignmentId*(ref→M12)*, submissionId*(ref→M10)*, **rubricVersionId**, state, timestamps + `JudgeEvaluationCriterionScore` rows {criterionId, score, comment?}. **Validates:** judge has valid M12 assignment covering that submission + uses the applicable published version. M12 remains authz-authoritative; M10 remains entry-authoritative — never duplicated.

## 13. Independent Evaluation — enforced

Judge A cannot see Judge B's evaluation before permitted release; scores never mutate across judges; each attributable/auditable; aggregates never exposed prematurely. **"Close" workflow + release timing = open** (FRS silent on the mechanism).

## 14. Evaluation Immutability

Open evaluation: editable while open *(edit policy = open — audit preserved)*; after submit/finalize: frozen. Never silently overwrite official evaluation evidence; finalization semantics = open.

## 15. Score-Calculation Boundary — strict

M13 = criterion-level inputs + weights *within a single evaluation*. M14 = judge aggregation + audience-vs-judge weighting + final score + ranking + ties. **A criterion weight ≠ audience/judge weighting ≠ ranking weight** — never conflated.

## 16. Data Model

| Entity | Purpose | Notes |
|--------|---------|-------|
| `EvaluationTemplate` | rubric container | name, context refs, status |
| `EvaluationTemplateVersion` | **immutable published snapshot** | version no, criteria snapshot, scale, weights, applicability |
| `EvaluationCriterion` | dimension | within version — name, desc, weight, order |
| `JudgeEvaluation` | completed evaluation | judgeId, assignmentId, submissionId, **rubricVersionId**, state |
| `JudgeEvaluationCriterionScore` | per-criterion input | evaluationId+criterionId, score, comment |

## 17. API Surface (conceptual — ADR-007)

`POST /api/v1/evaluation-templates` · `PUT /{id}` (draft) · `PUT /{id}/criteria` (add/remove/reorder) · `PUT /{id}/weights` · `PUT /{id}/scale` · `POST /{id}/validate` · `POST /{id}/publish` · `GET /api/v1/judges/me/rubrics/{contextId}` (applicable version) · `POST /api/v1/evaluations` (submit — uses published version) · `PUT /{id}` (edit if open) · `POST /{id}/finalize` · `GET /{id}` (authz-scoped). Errors: `RUBRIC_WEIGHT_INVALID`, `RUBRIC_NOT_PUBLISHED`, `RUBRIC_VERSION_IMMUTABLE`, `EVALUATION_OUT_OF_SCOPE`, `EVALUATION_DUPLICATE`, `EVALUATION_FINALIZED` + RFC 9457.

## 18. Authorization

- **Rubric admin:** explicit system-role-gated create/edit/publish
- **Evaluation:** valid M12 assignment covering the submission + applicable published rubric
- **Visibility:** independent judging — cross-judge eval access denied pre-release
- **Never:** TalentSkill/JudgeExpertise-alone/ProjectContributionRole as authorization

## 19. Security

Rubric tampering, published-version modification (immutability enforced + audited), eval IDOR, cross-judge leakage, cross-assignment access, unauthorized publish/score-mod, replay/duplicate eval (idempotent), audit bypass.

## 20. Concurrency / Consistency

Concurrent draft edits → optimistic lock; publish race → single version sequence; reorder/weight races → tx; duplicate eval submit → unique constraint + idempotency; finalize race → state guard; publish-while-evaluating → eval binds version at create-time. PG-only.

## 21. Downstream Contracts

| Module | Consumes |
|--------|----------|
| **M14 Scoring** | JudgeEvaluation + rubricVersion + criterion scores + weights + context + evaluation state → owns aggregation/audience-judge weighting/final score |
| **M15 Progression** | evaluation/result state for qualification decisions |
| **M16 Leaderboards** | result references |

## 22. Moderation / Audit

Audited: rubric create/draft-changes/publish/versioning; eval create/change/finalize; unauthorized access. Moderation policy separate. `AuditLog` ≠ telemetry.

## 23. Notifications

Signals: `RubricPublished`, `EvaluationOpened`, `EvaluationDeadline`, `JudgingClosed` — delivery owned by Notifications.

## 24. Open Decisions

Rubric status taxonomy, criterion precision, scale defaults, integer-vs-decimal, comment granularity, applicability precedence, auto-selection, eval edit policy, eval finalization, visibility release, close semantics, retirement, version numbering, admin approval, rubric reuse scope.

## 25. Acceptance Validation

Configurable ✓ · draft add/remove/reorder ✓ · 100% publish-gate ✓ · **published versions immutable** ✓ · evals bound to exact version ✓ · scale configurable (1–10 not hard-coded) ✓ · M12 assignment ✓ · M10 submission ✓ · M11 voting ✓ · M14 scoring ✓ · independent judging ✓ · assignment-scoped access ✓ · skills/roles never authorize ✓ · PG authoritative ✓ · no infra ✓ · no impl ✓

## 26. Traceability

- **FRS:** §20 rubrics · §19 judge scope · §21 evaluations · §30 audit · BR-2
- **ADRs:** ADR-001 boundary · ADR-003 PG · ADR-007 API
- **Modules:** M12 (assignment authz) · M10 (submission) · M09 (context) · M14/15/16 (consumers) · Moderation · Notifications
