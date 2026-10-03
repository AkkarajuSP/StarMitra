# StarMitra — Physical PostgreSQL Schema Design

**Status:** Physical design — no DDL/migrations yet | **Source:** Canonical model `75bf23a` + PO gate `a799bbb` | **Dialect:** PostgreSQL 15+ · naming: `snake_case` tables/columns

## 1. Global Conventions

| Convention | Rule |
|-----------|------|
| **PK** | `id UUID` (uuid type) — **application-generated UUIDv7**; time-ordered, non-enumerable; `PK <table>_pkey` |
| **Natural/composite keys** | join/dedup tables may use composite PKs `(a,b)` — noted per table |
| **Timestamps** | `TIMESTAMPTZ` everywhere; `created_at NOT NULL DEFAULT now()`; `updated_at` app-managed (trigger optional, not mandated); `deleted_at`/`finalized_at`/`captured_at`/`published_at`/`expires_at` — only where lifecycle requires; all UTC |
| **Statuses** | `VARCHAR(n)` + `CHECK` — never PG `ENUM` for lifecycle statuses (configurability/migration cost); see §25 |
| **Optimistic concurrency** | `version INT NOT NULL DEFAULT 0` — only on entities listed §29 |
| **Text** | `TEXT` for unbounded; `VARCHAR(n)` only for genuinely bounded values |
| **Money/numeric** | `NUMERIC` for scores/weights — never `FLOAT` |
| **JSON** | `JSONB` only for genuinely variable config payloads (configs/rules) — never for relational data |
| **IDs externally** | UUIDv7 = public identifier; `clientMessageId`/idempotency = dedup keys, never PKs |
| **actor/correlation** | `actor_id UUID`, `correlation_id UUID` — on audit/override/state-change records only, not blanket |

## 2. Identifier & Timestamp Decisions

- **UUIDv7, application-generated** — consistent with accepted architecture; DB does not generate PKs (no `gen_random_uuid()` default) — keeps generation consistent across modules; index-friendly (time-ordered → low B-tree fragmentation)
- **FK columns** all `UUID NOT NULL` referencing `id`
- **Timestamps app-written**, `created_at` DB-default `now()` as safety net; `updated_at` app-managed to preserve single-writer semantics

## 3. FK Strategy — classified

| Class | Rule |
|-------|------|
| **Physical FK** | same-module authoritative relationships → real `FOREIGN KEY` (e.g., `message_receipts.message_id → messages`) |
| **Logical cross-module ref** | cross-module ownership references → `UUID` column, **no FK**, documented read-contract (e.g., `submission_contributors.member_ref → project_members`) |
| **Derived ref** | projection tables → `UUID` source-ref, no FK (rebuildable) |
| **Polymorphic ref** | `target_type VARCHAR` + `target_id UUID`, no FK (M21 targets, moderation targets, notification source-refs) |

## 4. Physical Table Inventory — module by module

> Notation: `*` = NOT NULL · `UQ` = unique · `FK` = physical foreign key · `REF` = logical cross-module ref (no FK) · `POLY` = polymorphic typed ref. Standard `created_at/updated_at` omitted unless noted — all tables have both unless marked `append-only`.

### M01 — Authentication & Identity (6)

```sql
users(                 -- account status per FRS §8
  id UUID PK, email VARCHAR(320) * UQ, phone VARCHAR(32) UQ NULL,
  status VARCHAR(20) * CK IN ('ACTIVE','SUSPENDED','BLOCKED','DEACTIVATED'),
  created_at*, updated_at*)
system_roles(          -- configurable taxonomy, not hardcoded
  id UUID PK, name VARCHAR(50) * UQ, description TEXT)
user_system_roles(     -- PK(user_id,role_id) composite
  user_id UUID * FK→users, role_id UUID * FK→system_roles, assigned_at*)
refresh_tokens(        -- opaque, hashed — never plaintext
  id UUID PK, user_id UUID * FK→users, token_hash VARCHAR(128) * UQ,
  family_id UUID *, expires_at*, revoked_at NULL, replaced_by UUID NULL, reuse_detected BOOLEAN * DEFAULT false)
otp_challenges(        -- hashed OTP, never plaintext
  id UUID PK, user_id UUID * FK→users, otp_hash VARCHAR(128) *,
  attempts INT * DEFAULT 0, max_attempts INT *, expires_at*, consumed_at NULL)
authentication_audit_events( -- append-only → kernel AuditLog
  id UUID PK, user_id UUID, event_type VARCHAR(50) *,
  metadata JSONB, created_at*)   -- no updated_at
```

### M02 — User Profile (1)

```sql
user_profiles(
  id UUID PK, user_id UUID * UQ FK→users, display_name VARCHAR(120) *,
  bio TEXT, location VARCHAR(120), avatar_media_id UUID NULL REF→media_assets,
  banner_media_id UUID NULL REF→media_assets, visibility_state VARCHAR(30) *,
  completion_score SMALLINT, search_vector TSVECTOR)  -- GIN for ADR-011 FTS
```

### M03 — Talent Skills (3)

```sql
talent_skills(
  id UUID PK, name VARCHAR(120) * UQ, description TEXT,
  status VARCHAR(20) * CK IN ('ACTIVE','INACTIVE'),
  parent_skill_id UUID NULL FK→talent_skills, display_order INT)
skill_proficiencies(   -- config table, optional use
  id UUID PK, code VARCHAR(40) * UQ, label VARCHAR(80) *, ordinal INT *)
user_talent_skills(    -- composite PK; proficiency optional
  user_id UUID * FK→users, skill_id UUID * FK→talent_skills,
  proficiency_id UUID NULL FK→skill_proficiencies, added_at*, PK(user_id,skill_id))
```

### M04 — Media (2)

```sql
media_assets(          -- 4D state — separate fields, never one status
  id UUID PK, owner_user_id UUID * REF→users, object_key VARCHAR(512) * UQ,
  original_filename VARCHAR(255), media_type VARCHAR(20) *, mime_type VARCHAR(127) *,
  size_bytes BIGINT, checksum VARCHAR(128),
  upload_state VARCHAR(20) * CK IN('INITIATED','UPLOADED','VERIFIED','FAILED'),
  processing_state VARCHAR(20) * CK IN('PENDING','PROCESSING','COMPLETED','FAILED','NOT_REQUIRED'),
  moderation_state VARCHAR(20) * CK IN('PENDING','APPROVED','REJECTED','RESTRICTED'),
  visibility VARCHAR(30) * CK IN('PUBLIC','FOLLOWERS','COLLABORATION_ONLY','PRIVATE'))
media_variants(
  id UUID PK, media_asset_id UUID * FK→media_assets, variant_type VARCHAR(40) *,
  object_key VARCHAR(512) * UQ, width INT, height INT, format VARCHAR(20), size_bytes BIGINT)
```

### M05 — Discovery (2 derived — refreshable read-model tables)

```sql
discovery_projections( -- derived, rebuildable
  id UUID PK, entity_type VARCHAR(40) *, entity_id UUID *, payload JSONB, refreshed_at*)
feed_projections(      -- derived, rebuildable
  id UUID PK, user_id UUID *, item_type VARCHAR(40) *, item_id UUID *,
  rank_score NUMERIC, refreshed_at*)
```

### M06 — Connect (7)

```sql
conversations(
  id UUID PK, type VARCHAR(20) * CK IN('ONE_TO_ONE','GROUP','PROJECT'),
  project_id UUID NULL REF→creative_rooms, status VARCHAR(20) *,
  created_by UUID * FK→users)
conversation_members(  -- composite PK
  conversation_id UUID * FK→conversations, user_id UUID * FK→users,
  role VARCHAR(20), joined_at*, left_at NULL, PK(conversation_id,user_id))
messages(
  id UUID PK, conversation_id UUID * FK→conversations, sender_id UUID * FK→users,
  sequence BIGINT *, body TEXT, sent_at*,
  UQ(conversation_id,sequence))
message_receipts(      -- per-recipient state
  message_id UUID * FK→messages, user_id UUID * FK→users,
  status VARCHAR(10) * CK IN('DELIVERED','READ'), updated_at*,
  PK(message_id,user_id))
message_attachments(
  id UUID PK, message_id UUID * FK→messages, media_id UUID * REF→media_assets, sort_order INT)
user_blocks(           -- M06 user-privacy — NOT moderation
  blocker_id UUID * FK→users, blocked_id UUID * FK→users, created_at*,
  PK(blocker_id,blocked_id), CK(blocker_id <> blocked_id))
message_idempotency(
  client_message_id VARCHAR(128) PK, message_id UUID *, user_id UUID *, created_at*)
```

### M07 — Creative Rooms (9)

```sql
creative_rooms(
  id UUID PK, owner_id UUID * FK→users, name VARCHAR(160) *, description TEXT,
  status VARCHAR(20) *, visibility VARCHAR(20) *, version INT * DEFAULT 0)
project_members(       -- authoritative membership
  room_id UUID * FK→creative_rooms, user_id UUID * FK→users,
  status VARCHAR(20) *, joined_at*, left_at NULL, PK(room_id,user_id))
project_contribution_roles(
  id UUID PK, room_id UUID * FK→creative_rooms, member_user_id UUID *,
  role_name VARCHAR(80) *, assigned_at*, revoked_at NULL,
  UQ(room_id,member_user_id,role_name))   -- member+role unique
required_skills(
  room_id UUID * FK→creative_rooms, skill_id UUID * REF→talent_skills,
  PK(room_id,skill_id))
project_invitations(
  id UUID PK, room_id UUID * FK→creative_rooms, inviter_id UUID * FK→users,
  invitee_id UUID * FK→users, status VARCHAR(20) *, expires_at,
  -- UQ partial: (room_id,invitee_id) WHERE status='PENDING'
project_tasks(
  id UUID PK, room_id UUID * FK→creative_rooms, title VARCHAR(200) *,
  description TEXT, assignee_member_id UUID NULL, status VARCHAR(20) *, due_date DATE)
project_assets(
  id UUID PK, room_id UUID * FK→creative_rooms, media_id UUID * REF→media_assets,
  role VARCHAR(40), UQ(room_id,media_id))
final_outputs(
  id UUID PK, room_id UUID * FK→creative_rooms, media_id UUID * REF→media_assets,
  finalized_at *, output_type VARCHAR(40))
project_credits(       -- verified contribution records
  id UUID PK, room_id UUID * FK→creative_rooms, member_user_id UUID * FK→users,
  contribution_role_id UUID * FK→project_contribution_roles,
  credit_label VARCHAR(160) *, verified BOOLEAN * DEFAULT false, verified_at NULL)
```

### M08 — Portfolio (4)

```sql
portfolios(
  id UUID PK, user_id UUID * UQ FK→users,     -- DB-04: one-per-user
  title VARCHAR(160), status VARCHAR(20) *)
portfolio_items(
  id UUID PK, portfolio_id UUID * FK→portfolios, title VARCHAR(160) *,
  description TEXT, skill_id UUID NULL REF→talent_skills,
  visibility VARCHAR(20) *, sort_order INT, status VARCHAR(20) *)
portfolio_item_media(
  item_id UUID * FK→portfolio_items, media_id UUID * REF→media_assets,
  sort_order INT, PK(item_id,media_id))
portfolio_item_contributions(              -- links verified credits only
  id UUID PK, item_id UUID * FK→portfolio_items,
  project_credit_id UUID * REF→project_credits, UQ(item_id,project_credit_id))
```

### M09 — Competitions (7)

```sql
competitions(
  id UUID PK, title VARCHAR(200) *, description TEXT,
  config_status VARCHAR(20) *, participation_status VARCHAR(20) *,
  round_state VARCHAR(20) *, created_by UUID * REF→users, version INT * DEFAULT 0)
competition_categories(
  id UUID PK, competition_id UUID * FK→competitions, name VARCHAR(120) *,
  description TEXT, UQ(competition_id,name))
competition_category_skills(
  category_id UUID * FK→competition_categories, skill_id UUID * REF→talent_skills,
  PK(category_id,skill_id))
competition_rounds(    -- M09 owns round structure; M15 never duplicates
  id UUID PK, competition_id UUID * FK→competitions, sequence INT *,
  name VARCHAR(120), start_at TIMESTAMPTZ, end_at TIMESTAMPTZ,
  vote_config_id UUID NULL REF→vote_configs, rubric_version_id UUID NULL REF→evaluation_template_versions,
  progression_config_id UUID NULL REF→progression_configurations,
  UQ(competition_id,sequence))
eligibility_rules(
  id UUID PK, competition_id UUID * FK→competitions, rule_type VARCHAR(40) *,
  rule_params JSONB, version INT * DEFAULT 0)
submission_configs(
  id UUID PK, competition_id UUID * FK→competitions, category_id UUID NULL FK→competition_categories,
  config JSONB *, version INT * DEFAULT 0)
competition_participants(   -- DB-03 typed single table
  id UUID PK, competition_id UUID * FK→competitions,
  category_id UUID NULL FK→competition_categories,
  participant_type VARCHAR(10) * CK IN('USER','PROJECT'),
  user_id UUID NULL REF→users, project_id UUID NULL REF→creative_rooms,
  registered_at*, status VARCHAR(20) *,
  CK( (participant_type='USER' AND user_id IS NOT NULL AND project_id IS NULL)
   OR (participant_type='PROJECT' AND project_id IS NOT NULL AND user_id IS NULL) ),
  UQ(competition_id,category_id,user_id,project_id))
```

### M10 — Submissions (4)

```sql
submissions(
  id UUID PK, participant_id UUID * FK→competition_participants,
  competition_id UUID * REF→competitions, category_id UUID * REF→competition_categories,
  round_id UUID * REF→competition_rounds, state VARCHAR(20) *,
  submitted_at TIMESTAMPTZ, finalized_at NULL, version INT * DEFAULT 0)
submission_media(      -- frozen at finalize
  submission_id UUID * FK→submissions, media_id UUID * REF→media_assets,
  sort_order INT, PK(submission_id,media_id))
submission_contributors(   -- DB-02: live ref + evidence snapshot
  id UUID PK, submission_id UUID * FK→submissions,
  member_ref UUID * REF→project_members, role_ref UUID NULL REF→project_contribution_roles,
  snapshot_member_display VARCHAR(200), snapshot_role_name VARCHAR(80), captured_at TIMESTAMPTZ)
submission_history(    -- append-only trail
  id UUID PK, submission_id UUID * FK→submissions, from_state VARCHAR(20),
  to_state VARCHAR(20) *, actor_id UUID, reason TEXT, created_at*)  -- no updated_at
```

### M11 — Audience Voting (2)

```sql
votes(                 -- authoritative; team-entry = ONE vote row
  id UUID PK, voter_id UUID * FK→users, submission_id UUID * FK→submissions,
  competition_id UUID * REF→competitions, category_id UUID * REF, round_id UUID * REF,
  target_type VARCHAR(10) * CK IN('INDIVIDUAL','PROJECT'), cast_at*, client_msg_id VARCHAR(128),
  UQ(voter_id,submission_id,round_id), UQ(client_msg_id))
vote_configs(          -- M11 behavior config (M09 holds ref only)
  id UUID PK, version_no INT *, payload JSONB *, status VARCHAR(20) *,
  published_at NULL, UQ(id,version_no))
```

### M12 — Judge Management (3)

```sql
judges(
  id UUID PK, user_id UUID * UQ FK→users, status VARCHAR(20) *, created_at*)
judge_expertise(
  id UUID PK, judge_id UUID * FK→judges, skill_id UUID NULL REF→talent_skills,
  domain_label VARCHAR(120), verified BOOLEAN * DEFAULT false)
judge_assignments(     -- DB-08 inline scope — the authz boundary
  id UUID PK, judge_id UUID * FK→judges,
  competition_id UUID * REF→competitions, category_id UUID NULL REF, round_id UUID NULL REF,
  status VARCHAR(20) * CK IN('PENDING','ACTIVE','REVOKED','COMPLETED'),
  assigned_at*, revoked_at NULL,
  UQ(judge_id,competition_id,category_id,round_id))
```

### M13 — Rubrics (5)

```sql
evaluation_templates(
  id UUID PK, name VARCHAR(160) *, status VARCHAR(20) *, created_by UUID REF→users)
evaluation_template_versions(   -- immutable post-publish
  id UUID PK, template_id UUID * FK→evaluation_templates, version_no INT *,
  status VARCHAR(20) * CK IN('DRAFT','VALIDATED','PUBLISHED','RETIRED'),
  payload JSONB *, published_at NULL, UQ(template_id,version_no))
evaluation_criteria(
  id UUID PK, version_id UUID * FK→evaluation_template_versions,
  name VARCHAR(160) *, description TEXT, weight NUMERIC(5,2) *, max_score NUMERIC(6,2),
  sort_order INT)   -- publish-check: SUM(weight)=100 via app+validated state
judge_evaluations(     -- DB-09 one per judge+submission+round
  id UUID PK, judge_id UUID * REF→judges, assignment_id UUID * REF→judge_assignments,
  submission_id UUID * REF→submissions, rubric_version_id UUID * FK→evaluation_template_versions,
  competition_id UUID * REF, category_id UUID * REF, round_id UUID * REF,
  status VARCHAR(20) *, submitted_at NULL,
  UQ(judge_id,submission_id,round_id))
judge_evaluation_criterion_scores(
  evaluation_id UUID * FK→judge_evaluations, criterion_id UUID * FK→evaluation_criteria,
  score NUMERIC(8,3) *, comment TEXT, PK(evaluation_id,criterion_id))
```

### M14 — Scoring & Ranking (9)

```sql
scoring_configurations(     -- versioned; weights config never hardcoded
  id UUID PK, competition_id UUID * REF, version_no INT *,
  weight_audience NUMERIC(5,2) *, weight_judge NUMERIC(5,2) *,
  CK(weight_audience+weight_judge=100), status VARCHAR(20) *, published_at NULL,
  UQ(id,version_no))
tie_break_configurations(
  id UUID PK, competition_id UUID * REF, version_no INT *, criteria JSONB *,
  status VARCHAR(20) *, published_at NULL, UQ(id,version_no))
qualification_configurations(
  id UUID PK, competition_id UUID * REF, version_no INT *, rule_payload JSONB *,
  status VARCHAR(20) *, published_at NULL, UQ(id,version_no))
judge_score_aggregations(   -- derived
  id UUID PK, submission_id UUID * REF, round_id UUID * REF,
  config_version_id UUID * REF, aggregate NUMERIC(10,4), computed_at*)
audience_score_aggregations(-- derived from votes
  id UUID PK, submission_id UUID * REF, round_id UUID * REF,
  config_version_id UUID * REF, aggregate NUMERIC(10,4), computed_at*)
final_scores(               -- sealed, reproducible
  id UUID PK, submission_id UUID * REF, competition_id UUID * REF,
  category_id UUID * REF, round_id UUID * REF,
  scoring_config_id UUID * REF→scoring_configurations, score_version INT *,
  final_score NUMERIC(10,4) *, status VARCHAR(20) *, sealed_at NULL,
  UQ(submission_id,round_id,scoring_config_id,score_version))
rankings(                   -- derived snapshot
  id UUID PK, competition_id UUID *, category_id UUID *, round_id UUID *,
  submission_id UUID *, rank INT *, tie_break_applied JSONB, snapshot_version INT *,
  UQ(competition_id,category_id,round_id,submission_id,snapshot_version))
qualification_results(      -- consumed by M15
  id UUID PK, submission_id UUID *, round_id UUID *, config_version_id UUID * REF,
  qualified BOOLEAN *, decided_at*)
score_overrides(            -- audited adjustment, never overwrites source
  id UUID PK, final_score_id UUID * FK→final_scores, actor_id UUID *,
  before_value NUMERIC(10,4) *, after_value NUMERIC(10,4) *,
  reason TEXT *, created_at*)   -- append-only
```

### M15 — Round Progression (3)

```sql
progression_configurations( -- M15-owned (CM-06); immutable once progression begins
  id UUID PK, competition_id UUID * REF, version_no INT *, rule_payload JSONB *,
  status VARCHAR(20) *, frozen_at NULL, UQ(id,version_no))
progression_records(
  id UUID PK, submission_id UUID * REF, round_id UUID * REF,
  source_round_id UUID * REF, target_round_id UUID * REF,
  outcome VARCHAR(20) * CK IN('ADVANCED','ELIMINATED','PENDING'),
  config_version_id UUID * REF, finalized_at NULL,
  UQ(submission_id,round_id,config_version_id))
progression_overrides(      -- separate audit event from ScoreOverride
  id UUID PK, progression_record_id UUID * FK→progression_records,
  actor_id UUID *, before_outcome VARCHAR(20) *, after_outcome VARCHAR(20) *,
  reason TEXT *, created_at*)   -- append-only
```

### M16 — Leaderboards (3)

```sql
leaderboard_projections(    -- derived/rebuildable
  id UUID PK, competition_id UUID *, category_id UUID *, round_id UUID *,
  entry_ref UUID *, display_payload JSONB, rank INT, refreshed_at*)
leaderboard_publications(   -- authoritative visibility state
  id UUID PK, competition_id UUID *, category_id UUID *, round_id UUID *,
  status VARCHAR(20) * CK IN('HIDDEN','PUBLISHED','ARCHIVED'),
  published_at NULL, published_by UUID NULL)
leaderboard_snapshots(      -- sealed version-bound ref
  id UUID PK, leaderboard_publication_id UUID * FK→leaderboard_publications,
  result_version_refs JSONB *, captured_at*)
```

### M17 — Notifications (6)

```sql
notifications(
  id UUID PK, recipient_id UUID * FK→users, type VARCHAR(40) *,
  template_id UUID NULL REF, body TEXT, source_ref_type VARCHAR(40), source_ref_id UUID,
  deep_link VARCHAR(512), state VARCHAR(20) *, created_at*)
notification_preferences(
  user_id UUID * FK→users, type VARCHAR(40) *, channel VARCHAR(20) *,
  enabled BOOLEAN * DEFAULT true, PK(user_id,type,channel))
notification_templates(   -- versioned
  id UUID PK, code VARCHAR(60) *, version_no INT *, channel VARCHAR(20) *,
  body_template TEXT *, locale VARCHAR(10), status VARCHAR(20) *,
  UQ(code,channel,version_no,locale))
notification_delivery_attempts(
  id UUID PK, notification_id UUID * FK→notifications, channel VARCHAR(20) *,
  status VARCHAR(20) *, attempted_at*, retry_count INT * DEFAULT 0, failure_reason TEXT)
notification_read_states(
  notification_id UUID * FK→notifications, user_id UUID * FK→users,
  read_at TIMESTAMPTZ NULL, PK(notification_id,user_id))
notification_event_references(  -- dedup key
  id UUID PK, source_module VARCHAR(20) *, event_type VARCHAR(60) *,
  source_id VARCHAR(128) *, event_version VARCHAR(40),
  UQ(source_module,event_type,source_id,event_version))
```

### M18 — Moderation (7 — no appeal table per DB-07)

```sql
moderation_reports(
  id UUID PK, reporter_id UUID * FK→users, target_type VARCHAR(40) *,
  target_id UUID *, reason_code VARCHAR(40) *, detail TEXT,
  status VARCHAR(20) *, created_at*)
moderation_cases(
  id UUID PK, status VARCHAR(20) *, assigned_moderator_id UUID NULL REF→users,
  opened_at*, closed_at NULL)
moderation_decisions(       -- never overwritten
  id UUID PK, case_id UUID * FK→moderation_cases, moderator_id UUID * REF→users,
  decision_type VARCHAR(40) *, reason TEXT *, policy_version_id UUID NULL REF,
  created_at*)   -- append-only
moderation_actions(
  id UUID PK, decision_id UUID * FK→moderation_decisions,
  action_type VARCHAR(40) *, target_type VARCHAR(40) *, target_id UUID *,
  status VARCHAR(20) *, executed_at NULL, expires_at NULL)
moderation_evidence_references(
  id UUID PK, case_id UUID * FK→moderation_cases, evidence_type VARCHAR(40) *,
  ref_type VARCHAR(40) *, ref_id UUID *, captured_at*)
moderation_restrictions(    -- admin-enforced — NOT M06 UserBlock
  id UUID PK, target_type VARCHAR(40) *, target_id UUID *,
  restriction_type VARCHAR(40) *, status VARCHAR(20) *,
  starts_at*, expires_at NULL, created_by UUID * REF→users)
moderation_policy_references(
  id UUID PK, code VARCHAR(60) *, version_no INT *, payload JSONB,
  status VARCHAR(20) *, UQ(code,version_no))
```

### M21 — Social Engagement (4)

```sql
follows(                 -- DB-06 user->user only
  follower_id UUID * FK→users, followee_id UUID * FK→users, created_at*,
  PK(follower_id,followee_id), CK(follower_id <> followee_id))
likes(                   -- DB-01 targets: MEDIA,PORTFOLIO — extensible enum
  id UUID PK, user_id UUID * FK→users, target_type VARCHAR(20) *
  CK IN('MEDIA','PORTFOLIO'), target_id UUID *, created_at*,
  UQ(user_id,target_type,target_id))
comments(                -- DB-05 create+delete only
  id UUID PK, author_id UUID * FK→users, target_type VARCHAR(20) *
  CK IN('MEDIA','PORTFOLIO'), target_id UUID *, body TEXT *,
  status VARCHAR(20) * CK IN('ACTIVE','REMOVED'), created_at*)   -- soft-delete via status
engagement_counters(     -- derived/rebuildable
  target_type VARCHAR(20) *, target_id UUID *,
  follow_count BIGINT * DEFAULT 0, like_count BIGINT * DEFAULT 0,
  comment_count BIGINT * DEFAULT 0, refreshed_at*,
  PK(target_type,target_id))
```

### Platform Kernel (3)

```sql
audit_log(               -- append-only; all modules write via contract
  id UUID PK, actor_id UUID, actor_context VARCHAR(80),
  module VARCHAR(20) *, action VARCHAR(80) *,
  target_type VARCHAR(60), target_id VARCHAR(80),
  before_ref JSONB, after_ref JSONB, reason TEXT,
  correlation_id UUID, created_at*)   -- no updated_at; insert-only grants
platform_config(
  config_key VARCHAR(120) PK, config_value JSONB, updated_at*, updated_by UUID)
analytics_projections(   -- ADR-013 derived — never business-authoritative
  id UUID PK, projection_type VARCHAR(60) *, dims JSONB, metrics JSONB, refreshed_at*)
```

## 5. Module → Table Count Summary

M01:6 · M02:1 · M03:3 · M04:2 · M05:2(derived) · M06:7 · M07:9 · M08:4 · M09:7 · M10:4 · M11:2 · M12:3 · M13:5 · M14:9 · M15:3 · M16:3 · M17:6 · M18:7 · M21:4 · Kernel:3 = **90 physical tables** (~78 authoritative, ~9 derived/projection, 3 kernel)

## 6. Enum / Type Strategy (§24 decision)

| Value class | Mechanism | Why |
|-------------|-----------|-----|
| Lifecycle statuses (account/media/submission/case…) | `VARCHAR` + `CHECK` | cheap to extend via migration vs PG ENUM ALTER pain |
| `participant_type`, `target_type`, `outcome`, vote `target_type` | `VARCHAR` + `CHECK` | small fixed sets but still CHECK for consistency |
| `SystemRole.name`, `TalentSkill`, `SkillProficiency`, `EligibilityRule.rule_type`, policy codes, notification `type`/`channel` | **reference/config tables** | product-configurable taxonomies — never DB enums |
| `media_type`, `conversation.type` | `VARCHAR` + `CHECK` | fixed-technical sets |

## 7. Security Notes

- `otp_challenges.otp_hash`, `refresh_tokens.token_hash` — **hashes only**, never plaintext
- `reporter_id` present in `moderation_reports` — access-masked at API layer
- No PII duplication: profile display data lives only in `user_profiles`; snapshots (DB-02) are evidence-grade exceptions, documented
- `audit_log` insert-only grants — no UPDATE/DELETE permission
