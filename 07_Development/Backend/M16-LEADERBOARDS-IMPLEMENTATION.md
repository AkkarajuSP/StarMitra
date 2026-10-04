# M16 — Leaderboards — Implementation

**Slice:** seventeenth business implementation. **Status: complete.** M16 is a pure read/presentation domain over M14 ranking truth + M15 outcomes — never recalculates anything.

## Ownership (3 V1.14 tables, mapped verbatim)

- `leaderboard_publications` — **authoritative visibility** (HIDDEN/PUBLISHED/ARCHIVED; admin-controlled; publication-gated reads)
- `leaderboard_projections` — **derived/rebuildable read model** (display_payload + rank; refreshed on PUBLISH; never truth)
- `leaderboard_snapshots` — sealed `result_version_refs` capture for reproducibility

## Contract surface (3 ops, unchanged)

`GET /leaderboards` (publication-gated, cursor) · `POST /leaderboards/publications` {competitionId, categoryId?, roundId?, action=PUBLISH|HIDE|ARCHIVE} · `POST /leaderboards/{publicationId}/snapshot`.

## Model

- **Publication-gated**: leaderboard reads require a PUBLISHED publication for the exact context — hidden/unpublished → NOT_FOUND (no existence leak)
- **Projection**: on PUBLISH, rebuild from `ScoringTruthContract.latestResults` (rank, finalScore, qualified, tied — M14 verbatim) + `ProgressionTruthContract.outcomesOf` (ADVANCED/ELIMINATED/PENDING display)
- **Never recalculates**: rank/score/qualification come verbatim from M14's sealed snapshot; display string, never ordering input
- **Cursor**: opaque `rank:<n>` keyset (rank monotonic within a publication)
- **Comp-wide scope**: null categoryId normalizes to zero-UUID key (NOT NULL cols); comp-level publish surfaces all categories

## Cross-module

| Dir | Contract |
|---|---|
| M16 → M14 | `latestResults` (new — rank+score+qualified+tied+snapshotVersion) |
| M16 → M15 | `outcomesOf` |
| M16 → M09 | `roundBelongsTo` (publish validation) |

## Testing

`LeaderboardFlowIT` 5 real-PG17: hidden→publish→read→hide gating + snapshot, M14 rank verbatim (never recalculated), admin-only/cross-comp denial, comp-wide scope, rank-keyset cursor pagination. Suite: **177**.

## Known follow-ups

- Per-round-only leaderboards currently supported; comp-level "overall" aggregation across rounds is a product decision (M14 owns any cross-round rollup).
- `leaderboard_snapshots.result_version_refs` captures `{capturedBy}` + binding to publication — richer M14-version refs can be added when M16 needs multi-round archival.
- No realtime (request-based refresh per MVP; ADR-009 reserved).
