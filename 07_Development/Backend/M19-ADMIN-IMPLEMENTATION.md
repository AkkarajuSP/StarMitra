# M19 — Admin Portal — Implementation

**Slice:** twentieth business implementation. **Status: complete.** M19 is pure orchestration/read — **zero transactional tables, zero domain ownership**. Every capability delegates to the owning module's contract.

## Contract surface (2 ops, unchanged)

`GET /admin/dashboard` (derived; never business truth) · `GET /admin/audit` (module/actorId/correlationId/cursor/limit).

## Model

- **Dashboard**: composed from owning-module contracts — `openModerationCases` via M18 `listCases`, `recentAuditEntries` via kernel `AuditQueryService`, `derived:true` marker
- **Audit search**: new kernel read seam `AuditQueryService.search(module, actorId, limit)` over append-only `audit_log` — preserves insert-only model (no separate audit system, no second table)
- **Authorization**: `SystemRoleGuard.requireAdmin()` on every op — backend authoritative, UI hiding never a control

## Design rule (per FRS/contract)

M19 exists for **dashboard aggregation + audit visibility** only. All admin workflows (role mgmt, taxonomy, competition, judges, rubrics, scoring, progression, leaderboards, moderation, announcements) call their **owning module's APIs directly** — M19 never duplicates them. This is why the contract surface is 2 ops.

## Cross-module

| Dir | Contract |
|---|---|
| M19 → M18 | `listCases` (open-case count) |
| M19 → kernel | `AuditQueryService` (new read seam over audit_log) |

## Testing

`AdminFlowIT` 3 real-PG17: dashboard derives from M18 truth + derived marker, audit search module+actor filtering, admin-only (unauthenticated + judge denied). Suite: **190**.

## Known follow-ups

- React/Vite admin UI is the frontend half — backend orchestration seam is what this slice delivers (frontend is a separate slice).
- `correlationId`/`cursor` contract params accepted; current implementation filters module+actorId (cursor keyset wired when dashboard volume justifies).
- SUPER_ADMIN-only actions stay inside owning modules (no M19 bypass).
