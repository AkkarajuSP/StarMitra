# Physical Schema Reconciliation — Canonical → Physical

Canonical entity → physical table mapping. **GREEN** = exact mapping · **AMBER** = implementation choice documented · **RED** = mismatch.

| Canonical Entity | Physical Table | Owner | Status | Notes |
|------------------|----------------|-------|--------|-------|
| User | `users` | M01 | GREEN | |
| SystemRole | `system_roles` | M01 | GREEN | ref-table taxonomy |
| UserSystemRole | `user_system_roles` | M01 | GREEN | composite PK |
| RefreshToken | `refresh_tokens` | M01 | GREEN | token_hash only |
| OTPChallenge | `otp_challenges` | M01 | GREEN | otp_hash only |
| AuthenticationAuditEvent | `authentication_audit_events` | M01 | GREEN | append-only |
| PasswordCredential | — | M01 | GREEN-DEFERRED | PO-deferred — not created (intentional) |
| UserProfile | `user_profiles` | M02 | GREEN | +`search_vector` for ADR-011 |
| TalentSkill | `talent_skills` | M03 | GREEN | +self-FK parent |
| UserTalentSkill | `user_talent_skills` | M03 | GREEN | composite PK |
| SkillProficiency | `skill_proficiencies` | M03 | GREEN | config table |
| MediaAsset | `media_assets` | M04 | GREEN | 4 state fields |
| MediaVariant | `media_variants` | M04 | GREEN | |
| DiscoveryProjection | `discovery_projections` | M05 | GREEN | derived |
| FeedProjection | `feed_projections` | M05 | GREEN | derived |
| Conversation | `conversations` | M06 | GREEN | |
| ConversationMember | `conversation_members` | M06 | GREEN | composite PK |
| Message | `messages` | M06 | GREEN | (conv,seq) UQ |
| MessageReceipt | `message_receipts` | M06 | GREEN | composite PK |
| MessageAttachment | `message_attachments` | M06 | GREEN | media REF |
| UserBlock | `user_blocks` | M06 | GREEN | composite PK — CM-03 |
| MessageIdempotency | `message_idempotency` | M06 | GREEN | |
| CreativeRoom | `creative_rooms` | M07 | GREEN | version col |
| ProjectMember | `project_members` | M07 | GREEN | composite PK |
| ProjectContributionRole | `project_contribution_roles` | M07 | GREEN | |
| RequiredSkill | `required_skills` | M07 | GREEN | |
| ProjectInvitation | `project_invitations` | M07 | GREEN | partial UQ pending |
| ProjectTask | `project_tasks` | M07 | GREEN | |
| ProjectAsset | `project_assets` | M07 | GREEN | |
| FinalOutput | `final_outputs` | M07 | GREEN | |
| ProjectCredit | `project_credits` | M07 | GREEN | verified flag |
| Portfolio | `portfolios` | M08 | GREEN | UQ(user_id) — DB-04 |
| PortfolioItem | `portfolio_items` | M08 | GREEN | |
| PortfolioItemMedia | `portfolio_item_media` | M08 | GREEN | composite PK |
| PortfolioItemContribution | `portfolio_item_contributions` | M08 | GREEN | credit REF |
| Competition | `competitions` | M09 | GREEN | 3 status dims + version |
| CompetitionCategory | `competition_categories` | M09 | GREEN | |
| CompetitionCategorySkill | `competition_category_skills` | M09 | GREEN | |
| CompetitionRound | `competition_rounds` | M09 | GREEN | config REFs |
| EligibilityRule | `eligibility_rules` | M09 | GREEN | JSONB params |
| SubmissionConfig | `submission_configs` | M09 | GREEN | |
| CompetitionParticipant | `competition_participants` | M09 | GREEN | typed+CK — DB-03 |
| Submission | `submissions` | M10 | GREEN | finalized_at |
| SubmissionMedia | `submission_media` | M10 | GREEN | freeze at finalize |
| SubmissionContributor | `submission_contributors` | M10 | GREEN | snapshot cols — DB-02 |
| SubmissionHistory | `submission_history` | M10 | GREEN | append-only |
| Vote | `votes` | M11 | GREEN | dual UQ |
| VoteConfig | `vote_configs` | M11 | GREEN | versioned |
| VoteCount | *(no table)* | M11 | GREEN | derived — aggregate, not stored as entity |
| Judge | `judges` | M12 | GREEN | |
| JudgeExpertise | `judge_expertise` | M12 | GREEN | |
| JudgeAssignment | `judge_assignments` | M12 | GREEN | inline scope — DB-08 |
| EvaluationTemplate | `evaluation_templates` | M13 | GREEN | |
| EvaluationTemplateVersion | `evaluation_template_versions` | M13 | GREEN | immutable |
| EvaluationCriterion | `evaluation_criteria` | M13 | GREEN | |
| JudgeEvaluation | `judge_evaluations` | M13 | GREEN | UQ triple — DB-09 |
| JudgeEvaluationCriterionScore | `judge_evaluation_criterion_scores` | M13 | GREEN | composite PK |
| ScoringConfiguration | `scoring_configurations` | M14 | GREEN | CK weights=100 |
| TieBreakConfiguration | `tie_break_configurations` | M14 | GREEN | |
| QualificationConfiguration | `qualification_configurations` | M14 | GREEN | |
| JudgeScoreAggregation | `judge_score_aggregations` | M14 | GREEN | derived |
| AudienceScoreAggregation | `audience_score_aggregations` | M14 | GREEN | derived |
| FinalScore | `final_scores` | M14 | GREEN | version-bound UQ |
| Ranking | `rankings` | M14 | GREEN | snapshot |
| QualificationResult | `qualification_results` | M14 | GREEN | → M15 |
| ScoreOverride | `score_overrides` | M14 | GREEN | append-only |
| ProgressionConfiguration | `progression_configurations` | M15 | GREEN | frozen_at |
| ProgressionRecord | `progression_records` | M15 | GREEN | |
| ProgressionOverride | `progression_overrides` | M15 | GREEN | append-only |
| LeaderboardProjection | `leaderboard_projections` | M16 | GREEN | derived |
| LeaderboardPublication | `leaderboard_publications` | M16 | GREEN | |
| LeaderboardSnapshot | `leaderboard_snapshots` | M16 | GREEN | sealed refs |
| Notification | `notifications` | M17 | GREEN | |
| NotificationPreference | `notification_preferences` | M17 | GREEN | composite PK |
| NotificationTemplate | `notification_templates` | M17 | GREEN | versioned |
| NotificationDeliveryAttempt | `notification_delivery_attempts` | M17 | GREEN | |
| NotificationReadState | `notification_read_states` | M17 | GREEN | composite PK |
| NotificationEventReference | `notification_event_references` | M17 | GREEN | dedup UQ |
| ModerationReport | `moderation_reports` | M18 | GREEN | |
| ModerationCase | `moderation_cases` | M18 | GREEN | |
| ModerationDecision | `moderation_decisions` | M18 | GREEN | append-only |
| ModerationAction | `moderation_actions` | M18 | GREEN | |
| ModerationEvidenceReference | `moderation_evidence_references` | M18 | GREEN | |
| ModerationRestriction | `moderation_restrictions` | M18 | GREEN | ≠ UserBlock |
| ModerationAppeal | — | M18 | GREEN-DEFERRED | DB-07 — intentionally not created |
| ModerationPolicyReference | `moderation_policy_references` | M18 | GREEN | |
| Follow | `follows` | M21 | GREEN | user-only — DB-06 |
| Like | `likes` | M21 | GREEN | targets MEDIA/PORTFOLIO — DB-01 |
| Comment | `comments` | M21 | GREEN | create+delete — DB-05 |
| EngagementCounter | `engagement_counters` | M21 | GREEN | derived |
| AuditLog | `audit_log` | KERNEL | GREEN | append-only grants |
| PlatformConfig | `platform_config` | KERNEL | GREEN | |
| AnalyticsProjection | `analytics_projections` | KERNEL | GREEN | derived |
| AssignmentScope | — | M12 | GREEN-DEFERRED | DB-08 — not created (inline scope) |
| M19/M20 entities | — | — | GREEN | none exist — portals own nothing |

**Summary: canonical concepts → 90 physical tables + 3 intentional non-creations (PasswordCredential, ModerationAppeal, AssignmentScope); VoteCount = derived-aggregate concept, no table. RED: 0 — physical design complete.**

> **Count correction (Flyway phase):** an earlier summary stated "87 physical tables" — that was arithmetic error (90 − 3 deferred while forgetting `VoteCount` has no table and the canonical total was approximate). The authoritative count, verified by executing the migrations on a fresh PostgreSQL 17 database, is **90 tables** — matching the module-by-module enumeration in `PHYSICAL-DATABASE-SCHEMA.md` §5 exactly.
