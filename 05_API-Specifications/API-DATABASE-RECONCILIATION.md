# API ↔ Database Reconciliation

Every API operation maps to a domain owner whose persistence it may touch. Cross-module writes are always via the owning module's command — never shared-table writes. **Result: 0 RED.**

## Summary

| Namespace | Owner | Primary tables touched | Status |
|-----------|-------|------------------------|--------|
| /auth/* | M01 | users, system_roles, user_system_roles, refresh_tokens, otp_challenges, authentication_audit_events | GREEN |
| /profiles/* | M02 | user_profiles (REF→users, media_assets) | GREEN |
| /skills*, /users/me/skills | M03 | talent_skills, skill_proficiencies, user_talent_skills | GREEN |
| /media/* | M04 | media_assets, media_variants (read REF only from other modules) | GREEN |
| /search, /discovery, /feed | M05 | discovery_projections, feed_projections + read joins | GREEN |
| /conversations*, /users/me/blocks | M06 | conversations, conversation_members, messages, message_receipts, message_attachments, user_blocks, message_idempotency | GREEN |
| /rooms*, /invitations* | M07 | creative_rooms, project_members, project_contribution_roles, required_skills, project_invitations, project_tasks, project_assets, final_outputs, project_credits | GREEN |
| /portfolios/* | M08 | portfolios, portfolio_items, portfolio_item_media, portfolio_item_contributions (REF→project_credits verified) | GREEN |
| /competitions* | M09 | competitions, competition_categories, competition_category_skills, competition_rounds, eligibility_rules, submission_configs, competition_participants | GREEN |
| /submissions*, /competitions/{id}/submissions | M10 | submissions, submission_media, submission_contributors, submission_history | GREEN |
| /votes*, /vote-configs | M11 | votes, vote_configs (VoteCount derived — no table) | GREEN |
| /judges* | M12 | judges, judge_expertise, judge_assignments | GREEN |
| /judges/me* | M20→M12/M13/M10 read | judge_assignments + reads across | GREEN |
| /evaluation-templates*, /evaluations* | M13 | evaluation_templates, evaluation_template_versions, evaluation_criteria, judge_evaluations, judge_evaluation_criterion_scores | GREEN |
| /scoring*, /tie-break*, /qualification* | M14 | scoring_configurations, tie_break_configurations, qualification_configurations, judge_score_aggregations, audience_score_aggregations, final_scores, rankings, qualification_results, score_overrides | GREEN |
| /progression* | M15 | progression_configurations, progression_records, progression_overrides | GREEN |
| /leaderboards* | M16 | leaderboard_projections, leaderboard_publications, leaderboard_snapshots | GREEN |
| /notifications* | M17 | notifications, notification_preferences, notification_templates, notification_delivery_attempts, notification_read_states, notification_event_references | GREEN |
| /moderation/* | M18 | moderation_reports, moderation_cases, moderation_decisions, moderation_actions, moderation_evidence_references, moderation_restrictions, moderation_policy_references | GREEN |
| /admin/* | M19 (view) | audit_log + cross-module reads — **no writes** | GREEN |
| /social/* | M21 | follows, likes, comments, engagement_counters | GREEN |

## Verified absences (deferred entities have no endpoints)

`password_credentials` · `moderation_appeals` · `assignment_scope` — **zero endpoints** reference them.

## AMBER notes (documented, non-blocking)

- **A1** `/votes` writes to `votes` but reads `submissions`/`competition_rounds` via M10/M09 *read* access — allowed (read ≠ write); domain write stays M11.
- **A2** `/judges/me/submissions` is an M20 orchestration read spanning M12 scope + M10 data — no write; M20 owns no tables.
- **A3** `/admin/audit` reads kernel `audit_log` — read-only view; append-only writes remain internal.
- **A4** `engagement_counters` served from derived table — flagged `derived:true` in contract; never authoritative.

## Cross-checks

- Every write endpoint maps to exactly one owning module's tables — **no cross-module table writes**
- Every `REF` (non-FK) column surfaced as an ID in DTOs is populated only by the owning module's read — no raw table leakage
- Polymorphic `targetType`/`targetId` constrained to `MEDIA|PORTFOLIO` at API + DB level (DB-01)
