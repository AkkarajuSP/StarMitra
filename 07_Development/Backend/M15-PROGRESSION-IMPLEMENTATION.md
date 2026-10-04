# M15 — Round Progression — Implementation

**Slice:** sixteenth business implementation. **Status: complete.** M15 owns round lifecycle execution + qualification-driven advancement — never recomputes M14 results, never presents leaderboards (M16).

## Ownership (3 V1.13 tables, mapped verbatim)

`progression_configurations` (versioned JSONB rules, freeze-capable) · `progression_records` (UQ submission+round+config_version; outcomes ADVANCED/ELIMINATED/PENDING) · `progression_overrides` (append-only, separate from M14 score_overrides).

## Contract surface (5 ops, unchanged)

createProgressionConfig · calculateProgression (202) · finalizeProgression · getProgressionRecords · createProgressionOverride.

## Round lifecycle (via M09 contract — new `transitionRoundState`)

M09 owns `round_state`; M15 executes transitions via contract only:
- `activate` — comp+round validated, NOT_STARTED→ACTIVE
- `complete` — requires ACTIVE + **M14 sealed** for the round
- COMPLETE → anything else → STATE_TRANSITION_INVALID (no silent regress)
- `finalizeProgression` = record finalization + round completion in one audited step

## Progression execution

- `calculate` reads M14 `qualificationOf` + `latestRanking` — **never recomputes** — writes one record per finalized submission: ADVANCED (qualified + rank ≤ maxAdvance) / ELIMINATED, bound to latest published config version
- Config JSONB: `{targetRoundId?, maxAdvance?, minScore?, tiePolicy?}` — data-driven
- UQ `uq_pr_entry` → re-run idempotent, concurrent execution race-safe (pre-check + race catch)
- PROJECT entries advance as one unit — submission is the single progression key; never split by contributors
- Category preserved: advancement references target round within the SAME competition; cross-comp config rejected

## Overrides

`POST /progression/overrides` — admin-only, append-only (actor, before/after outcome, reason), audited; M14 qualification truth untouched.

## Cross-module

| Dir | Contract |
|---|---|
| M15 → M09 | `roundBelongsTo`, `roundStateOf`, `transitionRoundState` (new) |
| M15 → M10 | `finalizedInScope` |
| M15 → M14 | `ScoringTruthContract` — `qualificationOf`, `latestRanking`, `isSealed` |
| M16 → M15 | `ProgressionTruthContract` — `roundStateOf`, `outcomesOf`, `advancedFrom` |

## Testing

`ProgressionFlowIT` 5 real-PG17: lifecycle transitions + COMPLETE no-regress, M14-driven ADVANCED/ELIMINATED + idempotent recalc, finalize→COMPLETE+advancedFrom, sealed-required completion denial, admin-only/cross-comp denial. Suite: **172**.

## Known follow-ups

- Round activation is competition-level (M09 round_state is per-competition, not per-round) — sequential multi-round scheduling policy is a product decision.
- Auto-activation of next round: NOT implemented (explicit config/auditable policy required by FRS intent).
- `progression_configurations.frozen_at` reserved for mid-competition freeze policy.
