# Flyway Migration Execution Report

**Environment:** PostgreSQL 17.6 (local dev instance) · Fresh DB `starmitra_mig_test` · Executed via `psql -v ON_ERROR_STOP=1` in Flyway version order (Flyway orders `V1.10 > V1.2` numerically; execution also succeeded when files were applied in arbitrary sequence — no cross-slice FK dependencies exist)

## 1. Migration count
**21 migrations** — V1.0…V1.20 (module-ordered slices per `FLYWAY-MIGRATION-STRATEGY.md`, one migration per module/coherent table set + 1 seed)

## 2. Migration order
`V1.0 m01 → V1.1 m03 → V1.2 m04 → V1.3 m02 → V1.4 m06 → V1.5 m07 → V1.6 m08 → V1.7 m09 → V1.8 m10 → V1.9 m11 → V1.10 m12 → V1.11 m13 → V1.12 m14 → V1.13 m15 → V1.14 m16 → V1.15 m17 → V1.16 m18 → V1.17 m21 → V1.18 kernel → V1.19 m05 → V1.20 seed`

## 3. Fresh database migration result
**PASS** — all 21 files executed with `ON_ERROR_STOP=1`, zero errors, zero rollbacks. Re-runnable seed verified (`ON CONFLICT DO NOTHING`).

## 4. Final table count
**90** (`pg_tables` = 90) — ⚠️ the prompt/reconciliation figure of 87 was a documentation arithmetic error; the physical schema's own §5 enumeration lists 90, and 90 is the verified count. See `FLYWAY-SCHEMA-RECONCILIATION.md` AMBER-4.

## 5. Constraint count
PKs **90** · FKs **76** · UNIQUE **36** · CHECKs **25** — including all acceptance constraints (participant XOR, snapshot columns, portfolio UQ, eval UQ triple, judge-assignment scoped UQ, weights=100 CK, follow not-self CK, like/comment target CKs, dedup UQs)

## 6. Index count
**210** total `pg_indexes` entries (includes constraint-backed indexes) — ~40 explicitly created beyond PK/UQ backing; all trace to the index strategy

## 7. Seed/reference data result
`system_roles` = 4 (USER, JUDGE, ADMIN, SUPER_ADMIN — MODERATOR left unseeded, taxonomy open) · `skill_proficiencies` = 4 (baseline-proposed scale) — idempotent

## 8. Deferred objects confirmed absent
`password_credentials` · `moderation_appeals` · `assignment_scope` — `deferred_present = 0` ✓

## 9. Validation result
**PASS** — clean migrate from empty DB; no duplicate objects; no missing dependencies; constraints+indexes compile; seeds idempotent; all accepted tables present; deferred absent. `pg_trgm` extension created (V1.3). FTS `search_vector` generated column verified.

## 10. Warnings
- **Table-count documentation error** — corrected to 90 (AMBER-4)
- **`UQ(id, version_no)` was vacuous** on versioned config tables — replaced with meaningful series-scoped uniqueness (AMBER-1/2)
- **NULL-handling in UQs** — `NULLS NOT DISTINCT` required to actually enforce XOR/scoped uniqueness (AMBER-3)
- **`CREATE INDEX CONCURRENTLY`** not needed — all tables empty at creation; flagged for future backfill migrations only
- **`audit_log` insert-only enforced by grants** — deployment-time privilege restriction, not DDL

## 11. RED/AMBER findings
**RED: 0** · **AMBER: 5** — all documented implementation refinements in `FLYWAY-SCHEMA-RECONCILIATION.md`; none change business semantics.

## Status
**Migrations verified green on fresh PostgreSQL.** Ready for Flyway runtime integration when the backend module is scaffolded.
