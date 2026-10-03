# Flyway Migration Strategy

**Status:** Strategy only — no migration SQL yet | **Tool:** Flyway (Spring Boot integration) | **DB:** PostgreSQL 15+

## 1. Conventions

| Aspect | Rule |
|--------|------|
| **Location** | `db/migration` (classpath) — single source per environment |
| **Naming** | `V{major}.{seq}__{module}_{description}.sql` — e.g., `V1.1__m01_identity_core.sql`, `V1.7__m09_competitions.sql` |
| **Ordering** | module-dependency order: M01 → M03 → M04 → M02 → M06 → M07 → M08 → M09 → M10 → M11 → M12 → M13 → M14 → M15 → M16 → M17 → M18 → M21 → M05/KERNEL projections (dependencies first; cross-module REF columns never create FK ordering problems) |
| **Transactional** | default Flyway transaction-per-migration; no `BEGIN`/`COMMIT` inside files |
| **Repeatable** | `R__` prefix only for genuinely repeatable artifacts (views/functions if introduced); none currently |
| **Checksums** | never edit applied migrations — fix-forward with a new `V` migration; `validateOnMigrate=true` |
| **Immutability rule** | published config tables enforce immutability via `CHECK`/`status` guards + app-layer validation — documented in migrations |

## 2. Baseline Strategy

- New environments: `flyway migrate` from `V1.0` baseline — no pre-prod schema dump
- If a pre-existing dev schema ever exists: `baselineOnMigrate=true`, `baselineVersion=0` once, then forward-only
- No `flyway clean` outside local disposable environments

## 3. Seed / Reference Data

- **Reference taxonomies** (`system_roles`, `skill_proficiencies`, `platform_config` keys, moderation-policy codes): dedicated `V1.x__seed_*` migrations, idempotent (`INSERT ... ON CONFLICT DO NOTHING`) — never app-boot seeding
- **No business seed data** (no fake users/competitions) in migrations — test fixtures live outside Flyway
- Repeatable seeds only if a taxonomy is genuinely managed-in-place; otherwise new `V` versions

## 4. Rollback Philosophy

- **Fix-forward only** — no `undo` migrations; destructive changes only via explicit new `V` migration after review
- Rollback = restore from managed-Postgres backup/PITR — never schema rewind

## 5. Validation & Promotion

- `flyway validate` + `info` in CI before merge; `migrate` executes against ephemeral Postgres testcontainer/service in CI
- Promotion: same migration set applies dev → staging → prod unchanged (checksum-enforced)
- Manual review gate on any migration touching `audit_log`, `refresh_tokens`, `votes`, `judge_evaluations`, `final_scores`, `progression_records` — evidence/security-sensitive tables

## 6. Migration Slicing

- One migration = one module's coherent table set + its constraints/indexes (≈ 20 migrations for V1)
- Cross-cutting constraints (e.g., CHECKs) ship with their owning table's migration
- Indexes on high-volume tables (`messages`, `votes`, `notifications`, `audit_log`) created in the same migration — `CREATE INDEX CONCURRENTLY` deferred note: Flyway runs in transaction; concurrent indexes require a non-transactional migration file (`#flyway:shouldExecute` or split) — flag during DDL phase

## 7. Out of Scope (until next phase)

- Actual `V*.sql` files
- `CREATE INDEX CONCURRENTLY` handling for large backfills
- Migration for partitioning (deferred per ADR-003)
