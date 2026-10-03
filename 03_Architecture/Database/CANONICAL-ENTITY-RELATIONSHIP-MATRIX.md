# Canonical Entity Relationship Matrix

Companion to [CANONICAL-DATABASE-DESIGN.md](CANONICAL-DATABASE-DESIGN.md). FK strategy: **same-DB foreign keys within module**; **cross-module by ID reference validated at application layer + documented read-contracts** (modular-monolith boundary — ADR-001; Spring Modulith enforcement). Delete behavior: soft-deactivation preferred; hard-delete only for ephemeral records.

## Identity / Skills / Profile

| A → B | Cardinality | Owner-of-rel | Notes |
|-------|-------------|--------------|-------|
| UserSystemRole → User + SystemRole | N—1 each | M01 | unique pair; role taxonomy config |
| UserTalentSkill → User + TalentSkill | N—1 each | M03 | unique pair; optional proficiencyId → SkillProficiency |
| UserProfile → User | 1—1 | M02 | userId unique; avatarMediaId/bannerMediaId → MediaAsset (ref) |
| Judge → User | 1—1 | M12 | userId unique; requires UserSystemRole(Judge) |
| RefreshToken / OTPChallenge / AuthAuditEvent → User | N—1 | M01 | session/auth history |

## Media & Attachments

| A → B | Cardinality | Notes |
|-------|-------------|-------|
| MediaVariant → MediaAsset | N—1 (M04) | variants regenerable |
| UserProfile.avatar/banner → MediaAsset | N—1 (ref) | M02 field-ref |
| PortfolioItemMedia → PortfolioItem + MediaAsset | N—1 each | M08 link |
| SubmissionMedia → Submission + MediaAsset | N—1 each | M10 link; **frozen at finalize** |
| ProjectAsset → CreativeRoom + MediaAsset | N—1 each | M07 link |
| FinalOutput → CreativeRoom + MediaAsset | N—1 each | M07 link |
| MessageAttachment → Message + MediaAsset | N—1 each | M06 link |
| ModerationEvidenceReference → Case + any target | polymorphic | M18 typed ref |

## Social Engagement (M21)

| A → B | Cardinality | Notes |
|-------|-------------|-------|
| Follow → followerId + followeeId | unique pair (M21) | user→user MVP; target-extensible |
| Like → userId + targetRef | unique (user,target) | **≠ Vote** |
| Comment → authorId + targetRef | N—1 | target-type enum open |
| EngagementCounter → targetRef | 1—1 per target | derived/rebuildable |

## Messaging (M06)

| Conversation ↔ ConversationMember ↔ Message → MessageReceipt / MessageAttachment | standard messaging chain | UserBlock independent per-user |
| Conversation.projectId → CreativeRoom | N—1 (ref→M07) | membership validated vs M07 |

## Creative Rooms (M07)

| CreativeRoom → ProjectMember → ProjectContributionRole | 1—N—N | contribution contextual |
| CreativeRoom → RequiredSkill → TalentSkill | N—1 ref→M03 | never a permission |
| CreativeRoom → ProjectInvitation / ProjectTask / ProjectAsset / FinalOutput / ProjectCredit | 1—N each | all room-scoped |
| ProjectCredit → memberId + contributionRoleId | verified record | → consumed by M08 |

## Portfolio (M08)

| Portfolio → PortfolioItem → PortfolioItemMedia / PortfolioItemContribution | 1—N—N | contribution refs M07 `ProjectCredit` — never manufactures |

## Competition (M09)

| Competition → CompetitionCategory → CompetitionCategorySkill(→M03) | 1—N—N | |
| Competition → CompetitionRound | 1—N ordered | round config refs → M11 VoteConfig, M13 rubric, M15 progression |
| Competition → EligibilityRule / SubmissionConfig / CompetitionParticipant | 1—N | participant: userId XOR projectId(→M07) |

## Submission (M10)

| CompetitionParticipant → Submission | 1—N (constraints per config) | **team = ONE entry** |
| Submission → SubmissionMedia / SubmissionContributor / SubmissionHistory | 1—N | contributor → M07 member+role refs (+snapshot-open) |
| Submission → Competition/Category/Round | N—1 refs→M09 | context validated |

## Voting → Scoring → Progression → Leaderboard (M11→M16)

| Vote → Submission + competition/category/round + voterId | N—1 (M11) | idempotency: unique(voter,submission,round) |
| VoteConfig ← referenced by CompetitionRound | M09 ref → M11 entity | structural vs behavioral split |
| JudgeAssignment → Judge + comp/cat/round | N—1 refs (M12) | scope = authz |
| EvaluationTemplateVersion ← CompetitionRound/context | applicability | immutable |
| JudgeEvaluation → JudgeAssignment + Submission + rubricVersionId | N—1 each (M13) | **binds exact version** |
| JudgeEvaluationCriterionScore → evaluation + criterion | unique pair | |
| FinalScore → Submission + config versions + eval/vote aggregates | 1—1 per context (M14) | sealed, reproducible |
| Ranking / QualificationResult → FinalScore | derived snapshot | M14 |
| ScoreOverride → FinalScore | N—1 audit | never overwrites source |
| ProgressionRecord → Submission + round + M14 result refs | 1—1 per entry/round (M15) | finalized immutable |
| ProgressionOverride → ProgressionRecord | N—1 audit | separate from ScoreOverride |
| LeaderboardProjection/Publication/Snapshot → result+context refs | derived (M16) | never authoritative |

## Notifications / Moderation / Kernel

| Notification → recipientId + sourceRef + templateId | N—1 (M17) | deep-link never grants access |
| NotificationEventReference → source {module,type,sourceId,version} | dedup unique | |
| ModerationReport → reporterId + targetRef(typed) | N—1 (M18) | case linkage |
| ModerationCase → Decision → Action / Evidence / Restriction / Appeal | 1—N chain | owner enforces |
| AuditLog ← all modules | kernel contract | append-only |

## Cross-Module Reference Contracts (no FK — boundary)

- M06 → M07: project-membership validation (read-contract)
- M10 → M07: contributor membership/role + participant project-ref
- M10 → M09: competition/category/round/participant/config validation
- M11 → M09/M10: submission/competition context
- M12 → M09: assignment scope validation
- M13 → M09/M10/M12: context/submission/assignment refs
- M14 → M09–M13: all scoring inputs
- M15 → M09/M10/M14: rounds, entries, results
- M16 → M09–M15: presentation refs
- M17 → all: source-event refs
- M18 → all: target refs
- M21 → M04/M08 targets; M06 block; M18 restriction
- M19/M20 → orchestration across all

**No circular dependency** — kernel + M17/M18 are sinks; M19/M20 orchestrate.
