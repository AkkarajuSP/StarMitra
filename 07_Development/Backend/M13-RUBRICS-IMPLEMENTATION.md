# M13 — Judge Rubrics — Implementation

**Slice:** fourteenth business implementation. **Status: complete.** M13 owns rubric + judge-evaluation truth — never scoring/ranking aggregation (M14).

## Ownership

`evaluation_templates`, `evaluation_template_versions` (DRAFT/PUBLISHED immutable), `evaluation_criteria`, `judge_evaluations` (DB-09 UQ), `judge_evaluation_criterion_scores` — full V1.11 surface.

## Contract surface (7 ops, unchanged)

createTemplate · listTemplates · updateTemplate · publishRubricVersion · submitEvaluation · getEvaluation · reopenEvaluation.

## Models

- **Template**: name + ACTIVE status; `created_by` admin.
- **Version**: `version_no` UQ(template); DRAFT→PUBLISHED (+`published_at`); payload JSONB = `{applicability:{competitionId,categoryId,roundId}, scoreScale:{min,max}}` — **1–10 default, configurable, never hard-coded**.
- **Criterion**: name/desc/`weight`/`max_score`/`sort_order` — admin-defined, no FRS sample names baked in.
- **Evaluation**: judge+assignment+submission+**exact rubric_version** + comp/cat/round + per-criterion score/comment; `uq_jev_judge_sub_round` → replay idempotent.

## Invariants

- **Publish = weights Σ = EXACTLY 100.00** — app-enforced (ck_ec_weight bounds 0–100 per row); empty/≠100 rejected.
- **PUBLISHED immutable** — service refuses criteria edits; `updateTemplate` on published head creates **new DRAFT v(n+1)**; re-publish → STATE_TRANSITION_INVALID.
- **Round binding**: `round.rubric_version_id` (M09) must match when set; payload applicability scope enforced.
- **Judge scope**: M12 `requireScopedAssignment` resolves the covering assignment (stored on the eval as `assignment_id`) or CROSS_SCOPE_DENIED.
- **Score bounds**: per criterion `max_score` (or scale max); every criterion must be scored.
- **Reopen**: admin-authorized → REOPENED → resubmit replaces criterion scores (audited).

## Cross-module

| Dir | Contract |
|---|---|
| M13 → M12 | `JudgeScopeService` + new `requireScopedAssignment` |
| M13 → M09 | `roundWindow` (+rubricVersionId — extended) |
| M13 → M10 | `SubmissionTruthContract.submissionView` (FINALIZED gate) |
| M14 → M13 | `RubricContract` — `publishedVersion`, `resolveForRound` (criteria+weights+scale) |

## Testing

`RubricFlowIT` 6 real-PG17: publish @Σ100, bad-weight reject, publish→immutable+new-draft-v2+re-publish reject, scoped eval lifecycle (DB-09 replay, unassigned denial), validation (over-max, missing criterion, non-finalized), M14 `resolveForRound` seam. Suite: **162**.

## Known follow-ups

- `updateTemplate` re-supplies scope/scale on new version (payload rebuilt, not inherited — documented).
- RETIRED lifecycle + per-round rubric swap policy (progression-time concern, M15/M14).
- If-Match on updateTemplate accepted but version-concurrency is implicit (draft head edits).
