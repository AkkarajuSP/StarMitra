# Canonical Entity Inventory — M01…M21 + Platform Kernel

Flat inventory companion to [CANONICAL-DATABASE-DESIGN.md](CANONICAL-DATABASE-DESIGN.md). Every authoritative entity has exactly one owner. `O` = owned/authoritative · `D` = derived/projection · `R` = referenced-not-owned.

## M01 Authentication & Identity

| Entity | Type | PK | Lifecycle | Versioned | Audited | Retention | Referenced by |
|--------|------|-----|-----------|-----------|---------|-----------|---------------|
| User | O | userId | Active/Suspended/Blocked/Deactivated | — | status changes | soft-delete/deactivate | all |
| SystemRole | O | roleId | config lifecycle | taxonomy config | assign/remove | retention-open | M12, M19/20 |
| UserSystemRole | O | (userId,roleId) | assign/revoke | — | yes | history-open | all authz |
| RefreshToken | O | tokenId | issue→rotate→revoke/expire | — | rotation+reuse | expiry policy-open | — |
| OTPChallenge | O | challengeId | issue→verify/expire | — | attempts+lockout | short-lived | — |
| AuthenticationAuditEvent | O | eventId | append-only | — | is-audit | open/legal | kernel→AuditLog |
| PasswordCredential | deferred | — | open | — | — | open | — |

## M02 User Profile

| UserProfile | O | userId | active→restricted→deactivated(mirror) | — | visibility changes | profile retention-open | M05, M08, M18 |

## M03 Talent Skills

| TalentSkill | O | skillId | active/inactive (deactivate vs delete) | — | taxonomy admin | deactivation>deletion | M05, M07, M09, M12 |
| UserTalentSkill | O | (userId,skillId) | add/remove | — | changes | open | M02, M05, M08 |
| SkillProficiency | O | proficiencyId | config | optional-open | — | config | M03 |

## M04 Media Management

| MediaAsset | O | mediaId | 4D: upload/processing/moderation/visibility | — | authz-sensitive ops | retention-open | M02,M06,M07,M08,M10,M18,M20 |
| MediaVariant | O | variantId | regenerable | — | — | regenerable | consumers via M04 |

## M05 Discovery / Search / Feed

| DiscoveryProjection | D | — | refresh/rebuild | — | — | rebuildable | consumers |
| FeedProjection | D | — | refresh/rebuild | — | — | rebuildable | consumers |

## M06 StarMitra Connect

| Conversation | O | conversationId | create→active→archive | — | key transitions | retention-open | M07 (projectId), M17 |
| ConversationMember | O | (convId,userId) | join/leave/remove | — | membership changes | leftAt retained | all messaging authz |
| Message | O | messageId | sent(final) | per-conv sequence | state transitions | retention-open | M06 only |
| MessageReceipt | O | (messageId,userId) | sent→delivered→read | — | transitions | open | — |
| MessageAttachment | O | attachmentId | link | — | — | with message | →M04 |
| UserBlock | O | (blockerId,blockedId) | block/unblock | — | yes | open | M02,M05,M06,M21 |
| MessageIdempotency | O | clientMessageId | dedup | — | — | open | — |

## M07 Creative Rooms

| CreativeRoom | O | roomId | lifecycle proposal | — | transitions | open | M05,M06,M09,M10,M18 |
| ProjectMember | O | (roomId,userId) | join/leave/remove | — | changes | open | M06(read),M10 |
| ProjectContributionRole | O | roleId | assign/revoke | — | assignment | open | M08,M10 |
| RequiredSkill | O | (roomId,skillId) | declare/remove | — | — | open | →M03 |
| ProjectInvitation | O | invitationId | Pending→Accepted/Declined/Withdrawn/Expired | — | transitions | expiry-open | — |
| ProjectTask | O | taskId | create→done | — | — | open | →ProjectMember |
| ProjectAsset | O | assetId | link/unlink | — | — | open | →M04 |
| FinalOutput | O | outputId | finalize | — | finalization | open | →M04 |
| ProjectCredit | O | creditId | verified record | — | creation/verification | immutable-ish | →M08 |

## M08 Portfolio

| Portfolio | O | userId | active/restricted | — | visibility | open | M02,M05 |
| PortfolioItem | O | itemId | create/edit/remove | — | changes | open | M05,M18 |
| PortfolioItemMedia | O | (itemId,mediaId) | link | — | — | open | →M04 |
| PortfolioItemContribution | O | refId | link verified credit | — | — | open | →M07 |

## M09 Competitions

| Competition | O | competitionId | 3-dim lifecycle | — | transitions | open | all competition modules |
| CompetitionCategory | O | categoryId | draft→active | — | — | open | M10–M16 |
| CompetitionCategorySkill | O | (categoryId,skillId) | link | — | — | open | →M03 |
| CompetitionRound | O | roundId | sequence + timing | — | — | open | M10–M16 (**M15 never duplicates**) |
| EligibilityRule | O | ruleId | config | — | — | open | consumed M10 |
| SubmissionConfig | O | configId | config | versioned | — | open | consumed M10 |
| CompetitionParticipant | O | participantId | register→active | — | registration | open | M10–M16 (user XOR project) |

## M10 Submissions

| Submission | O | submissionId | Draft→Submitted→…→Finalized (proposal) | — | all transitions + finalize | evidence retention-open | M11–M16,M18,M20 |
| SubmissionMedia | O | refId | frozen at finalize | — | changes pre-finalize | immutable after | →M04 |
| SubmissionContributor | O | refId | set at submit | snapshot-open | — | open | →M07 |
| SubmissionHistory | O | historyId | append-only | — | is-audit | open | — |

## M11 Audience Voting

| Vote | O | voteId | cast (reversal-open) | — | every vote | open/legal | M14 |
| VoteConfig | O | configId | config | versioned | changes | open | consumed at cast-time |
| VoteCount | D | — | derived | — | — | rebuildable | M14,M16 |

## M12 Judge Management

| Judge | O | judgeId | active/inactive | — | create/deactivate | open | M13,M19,M20 |
| JudgeExpertise | O | expertiseId | config | — | changes | open | M12 only |
| JudgeAssignment | O | assignmentId | Pending→Active→Revoked→Completed | — | all transitions | open | M12,M13,M20 authz |
| AssignmentScope | O (optional) | — | — | — | — | open | — |

## M13 Judge Rubrics

| EvaluationTemplate | O | templateId | draft→published→retired | versioned | all changes | open | — |
| EvaluationTemplateVersion | O + **immutable** | versionId | published | **immutable** | publish | preserved | M13,M14,M20 |
| EvaluationCriterion | O (within version) | criterionId | frozen at publish | in-version | — | preserved | — |
| JudgeEvaluation | O | evaluationId | open→submitted→locked (proposal) | binds versionId | all ops | open | M14 |
| JudgeEvaluationCriterionScore | O | (evalId,criterionId) | within eval | — | — | open | — |

## M14 Scoring & Ranking

| ScoringConfiguration | O + versioned | configId+version | draft→published | **immutable pub.** | changes | preserved | consumed at scoring |
| TieBreakConfiguration | O + versioned | version | same | immutable | — | preserved | — |
| QualificationConfiguration | O + versioned | version | same | immutable | — | preserved | M15 |
| JudgeScoreAggregation | D | — | derived | — | — | rebuildable | M14→FinalScore |
| AudienceScoreAggregation | D | — | derived | — | — | rebuildable | M14→FinalScore |
| FinalScore | O (sealed) | resultId | provisional→final | result-version | calc/override | preserved | M15,M16 |
| Ranking | D (snapshot) | — | per context | version-bound | — | preserved | M16 |
| QualificationResult | D | — | qualifies/not | version-bound | — | preserved | M15 |
| ScoreOverride | O (audit) | overrideId | applied | — | **always** | preserved | — |

## M15 Round Progression

| ProgressionConfiguration | O + versioned | version | config | immutable-once-begun | — | preserved | — |
| ProgressionRecord | O | recordId | →finalized | bound to inputs | all | preserved | M16 |
| ProgressionOverride | O (audit) | overrideId | applied | — | **always** | preserved | — |

## M16 Leaderboards

| LeaderboardProjection | D | — | refresh/rebuild | — | — | rebuildable | consumers |
| LeaderboardPublication | O | publicationId | Hidden→Published→Archived (proposal) | — | transitions | open | — |
| LeaderboardSnapshot | D (sealed) | snapshotId | sealed ref | version-bound | publish | preserved | history |

## M17 Notifications

| Notification | O | notificationId | Created→Pending→Delivered/Failed→Read/Expired (proposal) | — | delivery transitions | retention-open | recipients |
| NotificationPreference | O | (userId,type,channel) | set | — | changes | open | — |
| NotificationTemplate | O + versioned | templateId+version | draft→active | versioned | changes | open | — |
| NotificationDeliveryAttempt | O | attemptId | retry lifecycle | — | attempts | open | — |
| NotificationReadState | O | (notifId,userId) | read/unread | — | transitions | open | — |
| NotificationEventReference | O | eventRefId | dedup key | — | — | open | all modules emit |

## M18 Moderation

| ModerationReport | O | reportId | Reported→…(proposal) | — | transitions | evidence retention-open | all targets |
| ModerationCase | O | caseId | Open→UnderReview→Actioned/Dismissed→Closed (proposal) | — | all | open | — |
| ModerationDecision | O | decisionId | decided (appeal-open) | — | **always** | preserved | — |
| ModerationAction | O | actionId | issued→enforced | — | **always** | preserved | →owning modules |
| ModerationEvidenceReference | O | evidenceId | attached | — | — | **legal-open** | →M04/others |
| ModerationRestriction | O | restrictionId | active/expired | — | **always** | open | enforced by owners |
| ModerationAppeal | O *(open)* | appealId | appeal flow | — | yes | open | — |
| ModerationPolicyReference | O + versioned | policyVersion | config | versioned | — | preserved | — |

## M19 / M20 — no entities (presentation/orchestration only)

## M21 Social Engagement

| Follow | O | (followerId,followeeId) | follow/unfollow | — | block/moderation-aware | open | M02,M04,M05,M17,M18 |
| Like | O | (userId,targetRef) | like/unlike | — | — | open | M05,M17,M18 — **≠M11 Vote** |
| Comment | O | commentId | create(+edit/delete open) | — | create/remove | open | M05,M17,M18 |
| EngagementCounter | D | targetRef | derived | — | — | rebuildable | M02,M05 |

## Platform Kernel

| AuditLog | O (kernel) | auditId | append-only | immutable | is-the-audit | open/legal | all modules write via contract; M19 reads |
| PlatformConfig | O (kernel) | configKey | config | — | changes | open | all |
| AnalyticsProjection | D (kernel) | — | derived | — | — | rebuildable | M19 views |

**Totals:** ~90 concepts · ~72 authoritative · ~12 derived · 3 kernel · M19/M20 = 0.
