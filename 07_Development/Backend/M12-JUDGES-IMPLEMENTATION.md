# M12 — Judge Management — Implementation

**Slice:** thirteenth business implementation. **Status: complete.** M12 owns judge + assignment truth — never rubrics/scoring/progression.

## Ownership

`judges`, `judge_expertise`, `judge_assignments` (DB-08 inline scope) — full V1.10 surface.

## Contract surface (9 ops, unchanged)

createJudge · listJudges · setJudgeExpertise · createAssignment · revokeAssignment · /judges/me · /me/assignments · /me/submissions · /me/rubrics/{contextId} (M13 seam → 404).

## Models

- **Judge**: one per user (UQ), ACTIVE; creation grants `JUDGE` system role via new M01 `SystemRoleContract` — **role ≠ evaluation access**.
- **Expertise**: skill_id (M03-validated) + domain_label + verified — **qualification data, NEVER authorization** (proven in-test).
- **Assignment**: `judge + competition + category? + round?` — `NULL` = wildcard within comp; `uq_ja_scope` NULLS NOT DISTINCT dedup; ACTIVE→REVOKED with `revoked_at`.

## The authz boundary

`JudgeScopeService` is the **only** arbiter — `coversScope`/`requireScope` resolve `judgeId + comp + (cat wildcard) + (round wildcard)`. Cross-comp/round/category access denied in-tests.

## Cross-module

| Dir | Contract |
|---|---|
| M12 → M01 | `SystemRoleContract.grantSystemRole` (new) |
| M12 → M09 | `categoryBelongsTo`/`roundBelongsTo` — scope validation |
| M12 → M10 | `finalizedInScope` + `submissionView` (judge portal, scoped) |
| M12 → M03 | `isActiveSkill` (expertise refs only) |
| M13/M14 → M12 | `JudgeScopeService` — `requireJudge`, `coversScope`, `requireScope` |

## Security

Admin-only judge/assignment ops (`ROLE_ADMIN`); JUDGE role + active assignment required for portal; `requireJudge` → JUDGE_FORBIDDEN; scope miss → CROSS_SCOPE_DENIED. Expertise/skill NEVER grants access.

## Testing

`JudgeFlowIT` 6 real-PG17: registration+idempotent, scope boundary (cross-comp/round/category denial + revoke), M09 scope validation, scoped-submissions isolation between judges, role-required, expertise≠authz. Suite: **156**.

## Known follow-ups

- Assignment COMPLETED lifecycle (PENDING state exists; transitions are product-level).
- Judge deactivation endpoint (status column ready).
- Conflict-of-interest hooks (FRS defers; not invented).
