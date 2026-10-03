# Database Open Decisions — PO / Technical Register

Decisions that must be resolved (or consciously deferred) before physical schema design. **None are silently resolved in the canonical model.** Severity for schema: **blocks-column** = schema can't finalize without it · **blocks-constraint** = table shape fine, constraint/config deferred · **deferrable** = safe to proceed and add later.

## Schema-Shaping Decisions (resolve before physical schema)

| ID | Decision | Modules | Options | Schema impact | PO needed? |
|----|----------|---------|---------|---------------|-----------|
| DB-01 | `Like`/`Comment` target types | M21 | user-content only / media+portfolio / broader | enum values on `targetType` | **Yes** |
| DB-02 | `SubmissionContributor` snapshot fields | M10 | live-ref only vs live+snapshot | presence of snapshot columns | **Yes** — evidence integrity |
| DB-03 | `CompetitionParticipant` shape | M09/M10 | single-row typed-ref (chosen) vs separate user/team tables | table structure | Confirm typed-ref |
| DB-04 | `Portfolio` cardinality | M08 | 1-per-user (proposal) vs many | unique constraint on userId | **Yes** |
| DB-05 | `Comment` edit/delete/reply | M21 | create-only vs editable vs threaded | editedAt/parentId columns | **Yes** |
| DB-06 | `Follow` target scope | M21 | user-only vs broader | targetType present-or-not | **Yes** |
| DB-07 | `ModerationAppeal` | M18 | included vs deferred | table exists-or-not | **Yes** — deferrable if excluded |
| DB-08 | `AssignmentScope` entity | M12 | separate entity vs inline fields | table-or-columns | Confirm inline |
| DB-09 | Multiple `JudgeEvaluation` per judge/submission | M13 | one-only vs multiple attempts | unique constraint shape | **Yes** |

## Value/Config Decisions (schema-shape safe — columns exist, values configurable)

| ID | Decision | Modules | Notes |
|----|----------|---------|-------|
| DB-10 | All lifecycle status enums (proposed values across M04–M18) | all | states exist as columns; final vocabularies open |
| DB-11 | Retention durations (messages, media, audit, moderation evidence) | M04/06/17/18/kernel | retention fields/flags exist; durations = policy |
| DB-12 | Scoring/aggregation formulas + weights | M14 | config rows; formula choice = PO |
| DB-13 | Tie-break criteria/order | M14 | config rows |
| DB-14 | Vote reversal policy + limits | M11 | config + constraint toggles |
| DB-15 | Deadline precedence + timezone + grace | M10 | evaluation logic; timestamps stored |
| DB-16 | `JudgeExpertise` taxonomy + verification | M12 | configurable refs |
| DB-17 | Contribution-role taxonomy + assignment workflow | M07 | configurable |
| DB-18 | Competition/project-type taxonomies | M09/M07 | configurable |
| DB-19 | PasswordCredential | M01 | deferred — table exists only if approved |
| DB-20 | Multi-tenancy | all | single-tenant; future decision only |

## Definitely Green (no decision needed)

All identity, media-lifecycle, messaging, submission-structure, voting-record, judge-assignment, rubric-versioning, scoring-config, progression, leaderboard, notification, moderation-case, follow entities — sufficiently defined to model now.

## Resolution Path

1. **Blockers-first batch** — resolve DB-01…DB-09 in one PO session (all are single-line answers)
2. **Then physical schema** — Flyway strategy → DDL → validation
3. DB-10…DB-20 can resolve anytime — they shape config values, not schema structure
