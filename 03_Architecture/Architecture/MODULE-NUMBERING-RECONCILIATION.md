# Module Numbering Reconciliation — M20 is Judge Portal

**Decision record — documentation only. No code ownership changes.**

## Finding

The implementation surface (M01–M19 backend, M21 Social) appeared to "skip" M20. Inspection of the accepted architecture documentation shows **M20 was intentionally assigned, not omitted**:

- `04_Modules/Judge-Portal/` — accepted module design for **M20 Judge Portal**
- `CROSS-MODULE-CONSISTENCY-REVIEW.md` — M20 referenced throughout as the judge self-service orchestration surface (`/api/v1/judges/me`), alongside M19 Admin Portal as the two **presentation/orchestration-only** layers (no domain ownership)
- `CROSS-MODULE-CONSISTENCY-RESOLUTION-ADDENDUM.md` — CM-07 resolved M20's namespace to `/api/v1/judges/me` (admin surface = `/api/v1/judges`)
- Backend package `com.starmitra.modules.judgeportal` with `JudgePortalController` (M12 contracts, scope-resolved server-side) — **partially pre-implemented** as the judge self-service seam extended by M12/M13 slices

## Decision

1. **M20 = Judge Portal** — real module, intentionally reserved. NOT a gap.
2. **No new domain is created.** M20 remains orchestration-only (delegates to M12/M13), exactly like M19.
3. **M21 Social Engagement** retains its number (created later as a dedicated module — CM-01's social-ownership gap). Not renumbered.
4. Index/documentation references that imply "M19 is the last module before M21" are corrected to acknowledge M20's Judge Portal seat.
5. The `judgeportal` backend package name is preserved (no rename; ArchUnit-safe).

## Canonical numbering

| Range | Modules |
|---|---|
| M01–M18 | Domain truth owners (identity → moderation) |
| **M19** | Admin Portal (admin orchestration/read surface) |
| **M20** | **Judge Portal** (judge self-service orchestration surface) |
| **M21** | Social Engagement |

Presentation/orchestration modules (M19, M20) own no domain tables; all mutations flow through owning-module contracts.
