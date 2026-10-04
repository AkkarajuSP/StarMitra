# M20 — Judge Portal — Implementation

**Slice:** judge-facing experience. **Status: complete.** M20 is orchestration only — zero domain ownership, zero transactional tables; every surface consumes M12/M13/M17 contracts.

## Contract surface (4 ops, unchanged)

`GET /judges/me` (M12 judge view) · `GET /judges/me/assignments` (M12 scope) · `GET /judges/me/submissions` (scope-resolved FINALIZED via M10 truth) · `GET /judges/me/rubrics/{roundId}` (**wired this slice**: M09 `roundWindow` → M12 `requireScope` → M13 `resolveForRound` — was a stub until M13 landed).

## Frontend (`frontend/src/judge/`)

- `layout/JudgeShell` — sidebar (Dashboard/Assignments/Submissions/Notifications), JUDGE badge, no admin nav
- `dashboard/` — counts from `myAssignments` + `mySubmissions` (authoritative, no invented aggregates)
- `assignments/` — scope labeled Competition-wide / Category-scoped / Round-scoped; read-only
- `submissions/` — scoped finalized list → Evaluate link
- `evaluation/` — published rubric rendered read-only, per-criterion score inputs (range-validated vs `maxScore`), comments, review→submit via `POST /api/v1/evaluations` (M13), submitted/locked state, 409→"already submitted"
- `notifications/` — M17 inbox
- `api.ts` — typed client; auth via shared M01 OTP login, `isJudge` nav gate (backend authoritative)

## Security/independence

- Scope resolved server-side by M12 `requireScope`/`requireScopedAssignment` — forged round/submission → CROSS_SCOPE_DENIED
- Judge independence: `getEvaluation` IDOR-safe (foreign judge → NOT_FOUND); duplicate submit = idempotent replay (DB-09), not a conflict

## Testing

`JudgePortalFlowIT` 3 real-PG17: rubric scope-checked resolution + cross-scope denial, in-scope evaluation + idempotent replay + judge-independence read denial, no-rubric 404. Frontend: 5 vitest (judge gating, score-range). Suite: **193**.

## Known follow-ups

- Submission media review uses M04 delivery URLs when M10 exposes media refs on the scoped-submission view (currently identifier-only — no evidence payload exposed).
- Judge-facing M18 reporting flow — contract gap; documented, not shortcut.
- Reopen workflow (M13 `evaluations/{id}/reopen`, admin-initiated) — no judge UI trigger.
