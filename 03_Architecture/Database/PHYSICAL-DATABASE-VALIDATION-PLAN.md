# Physical Database Validation Plan

Verifies `PHYSICAL-DATABASE-SCHEMA.md` fully and faithfully realizes the canonical model + PO gate — before any DDL is written.

## 1. Coverage Checks

| Check | Method | Pass criterion |
|-------|--------|----------------|
| Every canonical entity mapped | inventory cross-walk (§35 reconciliation) | 100% entities → table, `DEFERRED` marked not-created |
| Single-owner per authoritative entity | ownership column review | no entity appears in two module sections |
| Every relationship mapped | relationship-matrix vs schema | all canonical relationships represented as FK/REF/POLY/DERIVED |
| All accepted constraints represented | constraint register cross-walk | every register entry has a named CK/UQ/FK in schema |

## 2. Deferred / Unsupported-Entity Checks

- `moderation_appeals` — **must not exist** (DB-07)
- `assignment_scope` — **must not exist** (DB-08)
- `password_credentials` — **must not exist** unless PO approves
- No `parent_comment_id`/`edited_at` on `comments` (DB-05)
- No entity beyond canonical inventory (no invented tables)

## 3. Constraint Realization Spot-Checks

- `UQ(user_system_roles(user_id,role_id))` ✓
- `UQ(user_talent_skills(user_id,skill_id))` ✓
- `follows` composite PK + `CK(follower<>followee)` ✓
- `likes` `UQ(user_id,target_type,target_id)` + `CK target IN(MEDIA,PORTFOLIO)` ✓
- `portfolios` `UQ(user_id)` ✓
- `competition_participants` XOR `CK` on participant_type ✓
- `judge_assignments` `UQ(judge,comp,cat,round)` inline scope ✓
- `judge_evaluations` `UQ(judge,submission,round)` + `rubric_version_id` FK ✓
- `scoring_configurations` `CK(wA+wJ=100)` + versioned UQ ✓
- `final_scores`/`progression_records` version-bound UQ ✓
- `votes` `UQ(voter,submission,round)` + `UQ(client_msg_id)` ✓
- `audit_log` insert-only (no updated_at; grant-restricted) ✓

## 4. FK Ordering / Cycle Check

Migration order (Flyway §2) produces no circular FK dependencies — verified: every physical FK points to a table in the same or earlier module slice; cross-module references are REF (no FK) so they cannot create ordering cycles. **No deferred-constraint hacks needed.**

## 5. Index Traceability

Every index in `DATABASE-INDEX-STRATEGY.md` traces to a documented query pattern; no speculative indexes added in physical schema. FTS `search_vector` GIN + `pg_trgm` flags carried into DDL phase.

## 6. Type Compatibility

- UUID PKs — compatible with logical UUIDv7 strategy
- `TIMESTAMPTZ` — matches timestamp convention
- `NUMERIC` scores/weights — matches reproducibility requirements (no float)
- `JSONB` only on config/rule payloads — relational data stays relational
- `VARCHAR+CHECK` enum strategy — consistent with §25 decision

## 7. Sign-off Criteria

- Reconciliation table: **zero RED**
- Zero deferred entities physically present
- Every constraint-register item realized
- Reviewer sign-off on security-sensitive tables (tokens, OTP, votes, evaluations, scores, progression, audit)
