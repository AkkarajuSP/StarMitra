# Database Open Decisions — PO / Technical Register

**Status:** DB-01…DB-09 **RESOLVED by Product Owner** (this gate). DB-10…DB-20 remain open — they shape config values, not schema structure.

## Resolved — Schema-Shaping Decisions (DB-01…DB-09)

| ID | Decision | Final status | Schema consequence | Future extension? |
|----|----------|--------------|--------------------|-------------------|
| DB-01 | `Like`/`Comment` target types | **ACCEPTED FOR MVP** — `MEDIA`, `PORTFOLIO` only | `targetType` enum = {MEDIA, PORTFOLIO}; extensible shape preserved — new targets = new enum values | ✅ additive |
| DB-02 | `SubmissionContributor` snapshot | **ACCEPTED FOR MVP** — live ref **+ snapshot** | snapshot columns: `snapshotMemberDisplay`, `snapshotRoleName`, `capturedAt` — populated at finalization | ✅ evidence-grade |
| DB-03 | `CompetitionParticipant` shape | **CONFIRMED ARCHITECTURE** — single typed table | `participantType` + `userId` nullable + `projectId` nullable + `CK` exactly-one | ✅ |
| DB-04 | `Portfolio` cardinality | **ACCEPTED FOR MVP** — one per user | `UQ(Portfolio.userId)`; model extensible | ✅ drop constraint later |
| DB-05 | Comment lifecycle | **ACCEPTED FOR MVP** — create + delete only | no `editedAt`, no `parentCommentId` in MVP | ✅ additive columns later |
| DB-06 | Follow target scope | **LOCKED FOR MVP** — user→user only | `Follow(followerId, followeeId)`; extensible `targetRef` shape retained for future | ✅ documented extension |
| DB-07 | `ModerationAppeal` | **DEFERRED/FUTURE** — not an MVP physical table | no table; M18 extension point documented only | ✅ add later, no schema risk |
| DB-08 | `AssignmentScope` entity | **CONFIRMED ARCHITECTURE** — inline fields | `JudgeAssignment(competitionId, categoryId NULL, roundId NULL)`; no separate table | ✅ entity added only if submission-level scope approved |
| DB-09 | Multiple evaluations | **ACCEPTED FOR MVP** — one per judge+submission+round | `UQ(judgeId, submissionId, roundId)`; corrections via authorized reopen/amend flow | ✅ constraint-drop + version columns if ever needed |

### Rationale (summary)

- **DB-02** is the only evidence-integrity decision: finalized competition evidence must not depend on mutable M07 membership/role state — snapshot columns are required for reproducibility.
- **DB-03/DB-08** are confirmations of the canonical design's existing shape — no redesign.
- **DB-01/DB-04/DB-05/DB-06/DB-09** are MVP-scoping locks — all chosen in the extensible direction (additive-safe).
- **DB-07** deferred — appeal workflow undefined in FRS; extension point retained, no speculative table.

## Remaining Open Decisions (DB-10…DB-20 — config/value level, do NOT block schema)

| ID | Decision | Modules | Status |
|----|----------|---------|--------|
| DB-10 | Lifecycle status vocabularies (proposed values across M04–M18) | all | OPEN — columns exist; final enums = PO |
| DB-11 | Retention durations (messages/media/audit/evidence) | M04/06/17/18/kernel | OPEN — policy |
| DB-12 | Scoring/aggregation formulas + weights | M14 | OPEN — config rows |
| DB-13 | Tie-break criteria/order | M14 | OPEN — config rows |
| DB-14 | Vote reversal policy + limits | M11 | OPEN — config |
| DB-15 | Deadline precedence/timezone/grace | M10 | OPEN — evaluation logic |
| DB-16 | `JudgeExpertise` taxonomy + verification | M12 | OPEN — configurable |
| DB-17 | Contribution-role taxonomy + assignment workflow | M07 | OPEN — configurable |
| DB-18 | Competition/project-type taxonomies | M09/M07 | OPEN — configurable |
| DB-19 | `PasswordCredential` | M01 | OPEN/deferred — table only if approved |
| DB-20 | Multi-tenancy | all | OPEN — future decision only |

## Resolution Path — executed

1. ✅ **Blockers-first batch done** — DB-01…DB-09 resolved by PO gate
2. **Next:** physical schema — Flyway strategy → DDL → validation *(not started)*
3. DB-10…DB-20 resolve anytime — config values, not schema structure
