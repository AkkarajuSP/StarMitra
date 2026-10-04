# M18 — Moderation — Implementation

**Slice:** nineteenth business implementation. **Status: complete.** M18 decides + audits; owning modules enforce via their own contracts — M18 never mutates M02/M04/M06/M07/M10/M21 resource state.

## Ownership (7 V1.16 tables, mapped verbatim)

`moderation_reports` · `moderation_cases` (opened_at/closed_at) · `moderation_decisions` (append-only) · `moderation_actions` (enforcement rows for owning module) · `moderation_evidence_references` (typed refs — never duplicates content) · `moderation_restrictions` (governance ≠ M21 user_block) · `moderation_policy_references`.

## Contract surface (5 ops, unchanged)

createReport · listModerationCases · getModerationCase · createModerationDecision · createRestriction.

## Model

- **Reports**: user-scoped; controlled `TargetType` (9) + 8-reason taxonomy — forged types/reasons rejected; one active report per reporter+target (pre-check — no dup spam); different reporters allowed
- **Cases**: one per report; report linked as REPORT evidence (originals never deleted); OPEN→UNDER_REVIEW→RESOLVED/DISMISSED — closed never reopens
- **Decisions**: append-only — never overwritten; action rows record what the OWNING module must enforce
- **Restrictions**: admin-enforced governance state (POSTING/COMMENTING/...) — separate from user-to-user blocks

## Cross-module

| Dir | Contract |
|---|---|
| Consumers → M18 | `ModerationContract` — `activeRestrictions`, `isRestricted` (M04 delivery, M05 discovery, M21 social enforce via their own state) |
| M18 → M17 | NotificationContract seam (per-policy wiring follow-up) |

## Testing

`ModerationFlowIT` 5 real-PG17: report lifecycle + case/evidence + dup-report conflict, forged taxonomy denial, queue→decide→RESOLVED + closed-no-regress, admin-only surface, governance restrictions. Suite: **187**.

## Known follow-ups

- Case→resource enforcement wiring (M04 `moderation_state`, M05 exclusion, M21 comment hiding) — contracts ready; per-module application in their slices.
- No optimistic-lock column on cases (schema lacks `version`) — closed-transition is app-enforced.
- Appeals — DB-07 deferred (no `moderation_appeals` table by design).
- Retention — open product decision (no silent deletion).
