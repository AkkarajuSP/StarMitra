# Physical Database Design — Readiness Assessment

**Logical model:** accepted | **Canonical design:** `75bf23a` + PO decision gate (this commit) | **Gate status:** DB-01…DB-09 **resolved** — **no schema-hard-blockers remain**

## 1. Decisions Resolved (PO gate)

| ID | Outcome | Schema consequence |
|----|---------|--------------------|
| DB-01 | `targetType ∈ {MEDIA, PORTFOLIO}` | extensible enum — new targets additive |
| DB-02 | Live ref + **snapshot at finalize** | `snapshotMemberDisplay`, `snapshotRoleName`, `capturedAt` on `SubmissionContributor` |
| DB-03 | Single `CompetitionParticipant` typed table | `participantType` + `userId`/`projectId` nullable + XOR `CK` |
| DB-04 | One portfolio per user | `UQ(Portfolio.userId)` |
| DB-05 | Comment create+delete only | no `editedAt`/`parentCommentId` MVP |
| DB-06 | Follow user→user only | `Follow(followerId,followeeId)`; extensible shape retained |
| DB-07 | `ModerationAppeal` deferred | no table; M18 extension point |
| DB-08 | Inline assignment scope | `competitionId`+nullable `categoryId`/`roundId`; no scope table |
| DB-09 | One eval per judge+submission+round | `UQ(judgeId,submissionId,roundId)`; corrections = audited reopen/amend |

## 2. Remaining Open Decisions — **config-value level only (DB-10…DB-20)**

None block the physical schema — they populate config rows or set policy values on already-defined columns: status vocabularies, retention durations, scoring formulas/weights, tie-breaks, vote limits/reversal, deadline precedence, taxonomies (skills/judge-expertise/contribution-roles/competition-types), `PasswordCredential` (deferred), multi-tenancy (future-only).

## 3. Physical Schema Prerequisites

1. ✅ Canonical logical model accepted — ~90 entities, ownership unambiguous
2. ✅ PO gate cleared — no AMBER schema items remain
3. Physical-phase items (deferred appropriately): UUIDv7 physical type choice, Flyway migration ordering, table/column naming conventions, index DDL, `tsvector`/`GIN`/`pg_trgm` physical setup, object-storage key conventions — **none are open business decisions; all are implementation mechanics for the next phase**

## 4. Implementation Constraints (carry-over — ADR-bound)

- PostgreSQL authoritative; no distributed cache; no partitioning at MVP
- Cross-module boundaries = application-layer read-contracts, not cross-module FKs
- Append-only `AuditLog` (Platform Kernel); immutable versioned configs; finalize-immutability on submission evidence
- Signed-URL media access; no BLOBs; media binaries in object storage
- UUIDv7-style PKs server-generated; `clientMessageId`/idempotency = dedup keys

## 5. Unresolved Items

**None blocking.** Remaining: DB-10…DB-20 config/policy decisions + M21 minor open semantics (block-interaction rules, notification trigger scope, counter freshness SLA) — all safe to resolve during or after physical design.

## 6. Recommendation

**Proceed to Physical Database / Schema Design** — Flyway strategy → DDL generation → schema validation. The logical model is complete, ownership-clean, and decision-gated; nothing further blocks schema work.
