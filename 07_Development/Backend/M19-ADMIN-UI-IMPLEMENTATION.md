# M19 — Admin Portal Web UI — Implementation

**Slice:** M19 frontend half. **Status: complete.** React + TS + Vite admin app over owning-module APIs — zero domain duplication.

## Structure

```
frontend/src/
  api/client.ts         typed fetch + ApiProblem (RFC 9457), Bearer/ETag/Idempotency-Key
  api/auth.ts           M01 OTP login (request/verify), isAdmin gate
  admin/
    brand.css           StarMitra palette (navy/gold/purple/lavender/coral)
    layout/             AdminShell (sidebar+header+crumbs), LoginPage (2-step OTP)
    shared/ui.tsx       useApi + State (loading/error/empty) + Badge + Confirm
    dashboard/          GET /admin/dashboard — derived stat cards
    audit/              GET /admin/audit — module/actorId filters
    moderation/         M18 cases + decision dialog (reason required)
    leaderboards/       M16 publish/hide/archive (confirm)
    skills/             M03 list/create/deactivate (confirm)
    competitions/       M09 list (config/participation/round badges)
    judges/             M12 roster
    rubrics/            M13 templates (immutable-published note)
    scoring/            M14 calculate/finalize (confirm)
    progression/        M15 calculate/finalize (confirm)
    notifications/      M17 announcement surface (contract-gated)
    users/              M01 roles display (no mutation surface exposed)
```

## Design rules honored

- **Backend authoritative** — `isAdmin` gates navigation only; every op hits admin-guarded APIs
- **Sensitive actions** — Confirm dialog + warning + reason (decisions); finalize/publish flagged
- **Derived-not-truth** — dashboard carries `derived` marker + caption
- **Owning-module APIs only** — scoring/progression POST `.../calculate` + `.../finalize` (M14/M15 bodies `{competitionId, roundId}`); leaderboard publish → M16; moderation decisions → M18
- **No new infra** — same Vite/React stack; `/api` proxy; no state library needed

## Verification

`npm test` → 3 vitest (admin gating, ApiProblem mapping) · `tsc -b` clean · `vite build` clean · backend suite unchanged (190 PASS).

## Known follow-ups

- Deeper per-module admin forms (judge assignment UI, rubric criteria editor, competition create wizard) — backend APIs exist; UI is incremental.
- ETag/If-Match wired in client (`ifMatch` option); page-level use on the next UI pass.
- Session refresh (M01 `/auth/refresh`) — token held in memory; refresh-on-401 not yet wired.
