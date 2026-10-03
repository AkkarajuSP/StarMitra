# Module Design — 16: Leaderboards

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 16)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–15 without modification. **M16 is a read/presentation module** — it presents authoritative upstream results, never computes them.

## 1. Purpose

Own **leaderboard presentation**: rebuildable read models/projections of competition/round/category results, publication/visibility control, pagination/filtering, and historical snapshots — always referencing upstream authority, never replacing it.

## 2. Authoritative Source Model — every field traced

| Field | Authoritative owner |
|-------|---------------------|
| Entry identity | M10 Submission |
| Audience vote result | M11 |
| Judge evaluation inputs | M13 |
| FinalScore | M14 |
| Rank | M14 |
| Qualification | M14 |
| Progression | M15 |
| Competition/Category/Round | M09 |

**M16 never calculates a score or rank** — projections are rebuildable copies.

## 3. Module Boundary

| Owns | Does NOT own |
|------|--------------|
| `LeaderboardProjection` (derived read model), `LeaderboardPublication` (visibility state), `LeaderboardSnapshot` (historical ref) | Submission (M10), Vote (M11), Judge/assignment (M12), Rubric/Evaluation (M13), Score/Rank/Qualification (M14), Progression (M15), Competition (M09), authz (M01) |

## 4. Leaderboard Read Model — conceptual fields

`{competitionId, categoryId, roundId, entryId, displayRef, finalScore*(proj.)*, rank*(proj.)*, qualificationStatus, progressionStatus, publicationStatus, updatedAt}` — each field a **projection** of its upstream owner; none authoritative; snapshot-required fields flagged.

## 5. Leaderboard Scopes

Competition / category / round — **FRS-supported only**. Global-lifetime, talent-global, city, social, popularity leaderboards = **future/open, not invented**. Ranking scope (M14) ≠ display scope (M16).

## 6. Publication / Release — separate from finalization

`Hidden → Published → Archived` *(conceptual states — open)*. **Finalization ≠ publication:** a finalized M14/M15 result isn't automatically visible — publication is its own decision driven by competition config + product policy.

## 7. Provisional vs Final

Provisional display = **optional/open**; if supported, clearly labelled, never represented as final, defined refresh semantics. Final results always marked as such.

## 8. Snapshot vs Live Projection

- **Live projection:** current derived view, refreshable
- **Historical snapshot:** sealed reference to `{scoringConfigVersion, resultVersion, progressionVersion, publicationContext}` — reproducible even after authorized upstream correction. Snapshot is a reference, not a second source of truth.

## 9. Refresh / Rebuild

Projections generated → refreshed on upstream signal → invalidated on correction → rebuilt deterministically → reconciled. **Stale projection never alters results** — source stays authoritative; projection rebuildable always.

## 10. Consistency / Freshness

Transactional finalization (upstream) → async projection refresh → published freshness. Freshness SLA = **open**; stale leaderboard never affects competition outcome.

## 11. Pagination / Filtering (ADR-007)

Deterministic ordering (M14 rank); cursor/keyset; bounded pages; scope-limited filters. **Filtering changes the displayed set — never recalculates rank.** No arbitrary client sorting.

## 12. Ties / Rank Display

M14 owns tie-break + ranking convention. M16 displays the resulting rank/tie state **verbatim** — never recalculates, never picks a numbering convention (1,2,2,4 vs 1,2,2,3 = M14's output, not M16's choice).

## 13. Team / Project Display

One team entry = one leaderboard entry; rank applies to the entry; votes never split; contributors displayed as contextual metadata only. `ProjectContributionRole`/`TalentSkill` never authorize access.

## 14. Multi-Talent Model — preserved

Multi-skill users appear as their competition entries; leaderboard identity = entry; skills are display metadata only, never scope or authorization.

## 15. Visibility / Privacy

Respects competition visibility + submission visibility + participant profile visibility + moderation status — **visibility ≠ authorization; M16 never bypasses upstream authz**. No leakage of private participant/project data, judge identities pre-release, internal audit, or moderation internals.

## 16. Judge Confidentiality — enforced

No individual judge evaluations/scores/comments/assignments/confidential metadata exposed before configured release — **public leaderboard shows only product-allowed aggregate/final result**. M12/M13 remain authoritative for judge access + eval confidentiality.

## 17. Result Explanation

If "how was this calculated" exists: M16 consumes M14's explanation contract — **never independently recomputes** score composition. Presents only product-allowed components (final, judge/audience components if released, weights if published).

## 18. API Boundary (conceptual — ADR-007)

`GET /api/v1/leaderboards/{competitionId}` · `/{competitionId}/categories/{categoryId}` · `/{competitionId}/rounds/{roundId}` · `GET /entries/{id}/rank` · `GET /{competitionId}/results` · `GET /{contextId}/explanation` (release-gated). `/api/v1/leaderboards`; DTOs; Problem Details; cursor; authz-gated.

## 19. Authorization

Public / authenticated / participant / judge / admin access per product policy *(role matrix open)*. SystemRole authorizes; `TalentSkill`/`ProjectContributionRole`/`JudgeExpertise` never do; `JudgeAssignment` governs eval-context access, not leaderboard-admin access.

## 20. Observability

Projection refresh success/failure, lag, rebuilds, reconciliation failures, stale-data detection, query latency, publication failures — telemetry ≠ audit; analytics (M17+) never result-authoritative.

## 21. Data Integrity

Projection never source-of-truth; displayed rank references M14; progression references M15; published can't silently rewrite; one entry = one leaderboard row; rebuilds deterministic; hidden/restricted never leak.

## 22. Failure / Recovery

Refresh failure → stale flagged + retry; upstream correction → invalidate + rebuild; duplicate projection → idempotent; partial rebuild → resumable; restart → state preserved. **Read-model failure never touches authoritative outcomes.**

## 23. Cache / Performance (ADR-010)

HTTP caching for published/immutable pages; CDN where appropriate; in-process for stable refs — **cache never authoritative** for score/rank/qualification/progression/votes/evals. No Redis.

## 24. Versioning / Historical Results

Published leaderboard retains refs to `{competition/category/round, entry, resultVersion, rank, qualification, progression, publicationContext}` — explainable without making M16 authoritative; no full upstream duplication.

## 25. Open Product Owner Decisions

Scopes, provisional-vs-final display, publication states/timing, public/private visibility, freshness SLA, snapshot policy, retention, tie-display convention (consumed from M14), explanation visibility, participant-identity display, judge-aggregate visibility, withdrawal display, page sizes, filters, cache policy.

## 26. FRS Traceability

`[FRS §14–17]` competition/rounds/submissions/progression · `[§18]` voting · `[§22–24]` scoring/ranking/qualification · `[§25]` leaderboards · `[§30]` audit · team/multi-talent rules — FRS terminology preserved.

## 27. ADR Validation

ADR-003 PG (projection persistence) ✓ · ADR-007 API ✓ · ADR-010 cache-never-authority ✓ · ADR-012 jobs (projection rebuild via managed jobs) ✓ · ADR-013 analytics-never-produces-results ✓ · historical explainability ✓

## 28. Acceptance Validation

M16 = presentation-only ✓ · every field traced to upstream owner ✓ · projections rebuildable ✓ · finalization ≠ publication ✓ · ties displayed not recalculated ✓ · team = one entry ✓ · judge confidentiality enforced ✓ · visibility ≠ authz ✓ · no new score/rank computed ✓ · skills/roles never authorize ✓ · PG-backed projections ✓ · no infra ✓ · no impl ✓
