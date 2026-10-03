# Flyway Schema Reconciliation — Migration → PostgreSQL Objects

**Verified:** fresh `starmitra_mig_test` database on PostgreSQL 17.6 — all 21 migrations executed clean, 90 tables created, 0 deferred objects present. **RED: 0.**

Per-entity mapping is identical to [PHYSICAL-SCHEMA-RECONCILIATION.md](PHYSICAL-SCHEMA-RECONCILIATION.md) — every canonical entity → physical table is **GREEN** (verified live in `pg_tables`); this document records the **migration → object** layer plus deltas discovered during execution.

## Migration → Module Map

| Migration | Module | Tables created |
|-----------|--------|----------------|
| V1.0 | M01 | users, system_roles, user_system_roles, refresh_tokens, otp_challenges, authentication_audit_events (6) |
| V1.1 | M03 | talent_skills, skill_proficiencies, user_talent_skills (3) |
| V1.2 | M04 | media_assets, media_variants (2) |
| V1.3 | M02 | user_profiles (1) + `pg_trgm` extension |
| V1.4 | M06 | conversations, conversation_members, messages, message_receipts, message_attachments, user_blocks, message_idempotency (7) |
| V1.5 | M07 | creative_rooms, project_members, project_contribution_roles, required_skills, project_invitations, project_tasks, project_assets, final_outputs, project_credits (9) |
| V1.6 | M08 | portfolios, portfolio_items, portfolio_item_media, portfolio_item_contributions (4) |
| V1.7 | M09 | competitions, competition_categories, competition_category_skills, competition_rounds, eligibility_rules, submission_configs, competition_participants (7) |
| V1.8 | M10 | submissions, submission_media, submission_contributors, submission_history (4) |
| V1.9 | M11 | votes, vote_configs (2) |
| V1.10 | M12 | judges, judge_expertise, judge_assignments (3) |
| V1.11 | M13 | evaluation_templates, evaluation_template_versions, evaluation_criteria, judge_evaluations, judge_evaluation_criterion_scores (5) |
| V1.12 | M14 | scoring_configurations, tie_break_configurations, qualification_configurations, judge_score_aggregations, audience_score_aggregations, final_scores, rankings, qualification_results, score_overrides (9) |
| V1.13 | M15 | progression_configurations, progression_records, progression_overrides (3) |
| V1.14 | M16 | leaderboard_projections, leaderboard_publications, leaderboard_snapshots (3) |
| V1.15 | M17 | notifications, notification_preferences, notification_templates, notification_delivery_attempts, notification_read_states, notification_event_references (6) |
| V1.16 | M18 | moderation_reports, moderation_cases, moderation_decisions, moderation_actions, moderation_evidence_references, moderation_restrictions, moderation_policy_references (7) |
| V1.17 | M21 | follows, likes, comments, engagement_counters (4) |
| V1.18 | KERNEL | audit_log, platform_config, analytics_projections (3) |
| V1.19 | M05 | discovery_projections, feed_projections (2) |
| V1.20 | SEED | system_roles×4, skill_proficiencies×4 (idempotent) |
| **Total** | | **90 tables** |

## Deltas vs Physical Design Document (AMBER — implementation refinements, all documented)

| ID | Delta | Rationale |
|----|-------|-----------|
| **AMBER-1** | `vote_configs`: added `series_key VARCHAR(80)` + `UQ(series_key, version_no)` — replaces physically-vacuous `UQ(id, version_no)` | a PK column can't also be in a meaningful UQ; a series key is required for real version uniqueness |
| **AMBER-2** | Versioned configs (`scoring/tie_break/qualification/progression_configurations`) use `UQ(competition_id, version_no)` — replaces `UQ(id, version_no)` | `competition_id` is the natural config-series scope; guarantees one version sequence per competition |
| **AMBER-3** | `NULLS NOT DISTINCT` on `competition_participants`, `judge_assignments`, `notification_templates`, `notification_event_references` UQs | PostgreSQL treats NULLs as distinct → XOR/scoped UQs would silently allow duplicates without it (PG15+ syntax, within target) |
| **AMBER-4** | Table count = **90** (not 87) — corrected arithmetic; earlier "87" figure was a doc error | verified live: `SELECT count(*) FROM pg_tables` = 90, matching `PHYSICAL-DATABASE-SCHEMA.md` §5's own enumeration |
| **AMBER-5** | FK→`users` on cross-module user refs (voter, author, member, reporter, recipient, judge…) | per physical design's explicit `FK→` classification — `users` is the foundation table; `REF→` (no FK) used for all non-foundation cross-module refs exactly as designed |

## Confirmed Absent (deferred — verified `deferred_present = 0`)

`password_credentials` (open) · `moderation_appeals` (DB-07) · `assignment_scope` (DB-08)

## Confirmed Present — acceptance-critical

- `competition_participants` XOR `CK` (`ck_cp_xor`) + `UQ NULLS NOT DISTINCT` — DB-03 ✓
- `submission_contributors` snapshot columns — DB-02 ✓
- `portfolios` `UQ(user_id)` — DB-04 ✓
- `comments` no `edited_at`/`parent_comment_id`; `likes`/`comments` `CK target IN (MEDIA,PORTFOLIO)`; `follows` user-pair only — DB-01/05/06 ✓
- `judge_assignments` inline scope, no scope table — DB-08 ✓
- `judge_evaluations` `UQ(judge_id,submission_id,round_id)` — DB-09 ✓
- `audit_log` single kernel table, no `updated_at` (append-only) ✓
- `search_vector` generated `TSVECTOR` + GIN + `pg_trgm` — ADR-011 ✓

**Result: 0 RED · 5 documented AMBER refinements · all acceptance constraints verified live.**
