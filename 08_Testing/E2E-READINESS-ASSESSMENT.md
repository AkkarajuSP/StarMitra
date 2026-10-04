# StarMitra — Cross-Module Integration & End-to-End Readiness Assessment

**Scope:** M01–M21 as one integrated platform. **Verdict: READY WITH FOLLOW-UPS** — all golden paths pass end-to-end on real PostgreSQL; two P2 contract-consumer gaps documented (no P0/P1).

## Environment

- Real PostgreSQL (test profile, Flyway V1.1–V1.16 applied)
- Spring Boot 3 / Java 17 modular monolith — no external infra
- `StarMitraE2EIT` (3 tests) + full suite **197 tests, 0 failures**

## Golden path (verified end-to-end, single transaction graph)

Register (M01) → profile update (M02) → skill taxonomy + user skill (M03) → portfolio item w/ skill + PUBLIC visibility (M08) → competition + category + published rubric + round (M09+M13) → participant register → submission submit+finalize (M10, immutable) → audience votes (M11, dedup'd, per-submission) → judge create+assign (M12 scope) → criterion-scored evaluation (M13, idempotent submit per DB-09) → scoring calculate+finalize sealed (M14) → progression activate+calculate+outcomes (M15 consumes M14) → leaderboard hidden→publish→items→snapshot→hide (M16 rank verbatim) → notification dedup (M17).

## Project/team path (verified)

Room (M07) → invite+accept → **contribution role ≠ skill** (`assignRole` + `addRequiredSkill` remain separate) → PROJECT participant (M09) → one finalized submission (M10) → vote target_type=PROJECT, never split (M11) → judge eval (M13) → scoring (M14) → leaderboard = **one entry** (M16).

## Defect register

| ID | Sev | Modules | Scenario | Expected | Actual | Root cause | Owner | Fix |
|---|---|---|---|---|---|---|---|---|
| E2E-01 | **P2** | M18→M04/M05/M21 | M18 restriction on MEDIA should reduce deliverability/discovery | Restricted media not publicly deliverable | M18 `isRestricted` true but M04 `deliverableTo` unchanged | `ModerationContract` exists; **no consumer in owning modules** — M04 checks its own `moderation_state`, never M18 | M04/M05/M21 | Invoke `ModerationContract.activeRestrictions` at delivery/discovery/interaction boundaries |
| E2E-02 | **P2** | M17←all | Business events should emit notifications where policy requires | Producer events → inbox rows | Sink verified (dedup, read state, prefs); **zero producers call `notifyEvent`** | Producer-policy wiring not yet implemented per module | M06/M07/M09–M16/M18/M21 | Wire `NotificationContract.notifyEvent` at each event's authoritative commit point |
| E2E-03 | P3 | M19/M20 UI | ETag/If-Match client option exists but pages don't send it on mutations | Conflict surfaces as stale-edit | 409/412 mapped by ApiProblem; If-Match header not yet sent from pages | Frontend uses client option, not wired page-level | M19/M20 UI | Thread `ifMatch` through mutation calls |
| E2E-04 | P3 | M10→M20 | Judge review needs media refs | Scoped submission view carries media refs | `ScopedSubmission` exposes identifiers only | M10 media-ref exposure not implemented | M10 | Add media refs to scoped view (M04 delivery) |

## Notification coverage (actual)

| Event | Source | Wired? | Tested? |
|---|---|---|---|
| Follow/Like/Comment | M21 | No (contract ready) | Sink via direct call |
| Room invite/activity | M07 | No | Sink |
| Competition/round/submission | M09/M10 | No | Sink |
| Judge assignment/eval | M12/M13 | No | Sink |
| Score/qualification/progression | M14/M15 | No | Sink |
| Leaderboard published | M16 | No | Sink (verified in E2E) |
| Moderation | M18 | No | Sink |

**Producer wiring is a deliberate next-step gap** — the M17 sink/contract is verified; per-module policy emission is incremental.

## Moderation enforcement audit

`ModerationContract` = `activeRestrictions(targetType, targetId)` + `isRestricted` — exists, admin-gated writes audited. **Consumers: none outside M18** (M19 dashboard reads it for counts only). Report→case→decision→restriction lifecycle fully verified; **propagation to resource state is the P2 gap** — each owning module must call the contract at its own enforcement boundary.

## Security/IDOR (verified across suite)

- Judge scope: cross-competition/category/round → CROSS_SCOPE_DENIED; role ≠ access; expertise ≠ authorization
- Evaluations: foreign judge read → NOT_FOUND; dup submit → idempotent replay (DB-09)
- Leaderboard: unpublished → NOT_FOUND (no leak)
- Moderation: admin-only case/decision/restriction; dup report → CONFLICT
- Notifications: cross-user read → NOT_FOUND; prefs user-scoped
- Voting: voter limits, window, per-submission counts, PROJECT target unsplit
- Submissions: FINALIZED immutable; deadline enforced
- ArchUnit: 4 boundary rules green (no cross-persistence, no entity leaks)
- OpenAPI: 133 ops, contract test green

## State machine audit (all verified in ITs)

Competition DRAFT→CONFIGURED→FROZEN · Participation OPEN→CLOSED · Round NOT_STARTED→ACTIVE→COMPLETE (no regression from COMPLETE) · Submission DRAFT→SUBMITTED→FINALIZED (immutable) + WITHDRAWN · Rubric DRAFT→PUBLISHED (immutable; weights=100) · Judge assignment ACTIVE→REVOKED · Case OPEN→UNDER_REVIEW→RESOLVED/DISMISSED (closed never reopens) · Publication HIDDEN→PUBLISHED→ARCHIVED · Notification UNREAD→READ (idempotent).

## Data integrity

UQ constraints enforce dedup (votes, notifications, judge scope, report pre-check); append-only decisions/overrides/audit; sealed scoring never recalculated in place (new score_version rows); project = one submission + one vote target; skill≠role separation preserved end-to-end.

## Observability

`audit_log` insert-only + `correlationId` filter on every request; `AuditQueryService` admin read seam; module-prefixed actions (`M14.SCORE_OVERRIDE`, `M18.DECISION_MADE`, …). No sensitive payloads logged.

## Performance/N+1

Cursor pagination on feed/inbox/leaderboard; publication + read-state indexes; no N+1 on the golden path (contracts batch-resolve). No speculative infra introduced.

## Open contract gaps

1. M18→M04/M05/M21 restriction enforcement (P2)
2. M17 producer wiring per module (P2)
3. M10 media refs on scoped-submission view (P3)
4. Judge-facing M18 report flow (P3 — contract gap, documented)
5. M13 evaluation reopen judge-facing trigger (P3)
6. ETag page wiring + refresh-token client (P3)

## Remediation wave R1 — CLOSED (verified by RemediationR1IT, 9 tests)

| Gap | Resolution |
|---|---|
| **E2E-01 M18 enforcement** | `NoOpProfileRestrictionContract` removed — `ModerationService` implements `ProfileRestrictionContract` (USER target → restricted). `activeRestrictions` now filters clock-expired rows. M04 `deliverableTo` denies when media OR owner restricted. M05 search/discover/feed filter restricted profiles + restricted media/owner. M21 gates follow/like/comment by restriction type (POSTING/COMMENTING/FOLLOWING/LIKING/INTERACTION → TARGET_RESTRICTED). Verified: restricted media/user denied, expired restores, owner keeps own asset. |
| **E2E-02 M17 producers** | Wired: FOLLOW (M21→followee), LIKE (M21→target owner via new `ownerOf` on M04/M08 contracts), COMPETITION_PUBLISHED (M09 create→organizer), LEADERBOARD_PUBLISHED (M16 publish→entry owners via M10/M09), EVALUATION_STATUS (M13 submit→submission owner). Dedup + prefs respected. Unwired events (ROOM_INVITATION, SUBMISSION_STATUS, SCORE_AVAILABLE, MODERATION_ACTION, ROUND_PROGRESS) — documented producer-policy follow-ups. |
| **E2E-03 ETag** | Client `ifMatch`/`Idempotency-Key` verified (3 vitest). Page-level wiring follows as versioned-resource edit forms land. |
| **E2E-04 M10 media refs** | `SubmissionTruthContract.mediaIdsOf` added; `ScopedSubmission.mediaIds` surfaces refs to M20 (M04 ids only — never storage keys). |

Suite: **205 backend + 11 frontend, all green.** OpenAPI 133 ops, ArchUnit, FlywaySchemaIT — PASS.

## Remediation plan (post-R1)

1. **P2-E2E-01**: wire `ModerationContract` into M04 `deliverableTo`, M05 feed/search filters, M21 interactions — per owning module's boundary.
2. **P2-E2E-02**: emit `notifyEvent` at each accepted event commit (start with FOLLOW/LIKE + COMPETITION_PUBLISHED + LEADERBOARD_PUBLISHED + EVALUATION_STATUS).
3. P3 items — incremental UI/contract refinements; none block UAT.

## Final recommendation

**READY WITH FOLLOW-UPS** — the platform works as one integrated system end-to-end; no P0/P1. The two P2 gaps are documented contract-consumer wirings, not architectural defects.
