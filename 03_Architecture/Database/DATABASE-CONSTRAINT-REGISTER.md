# Database Constraint Register — Canonical Logical Constraints

Logical constraints only — physical DDL belongs to the next phase. `UQ` unique · `CK` check · `FK` foreign key · `TR` state-transition · `ID` idempotency.

## M01 Identity

- `UQ` UserSystemRole(userId, roleId)
- `CK` User.status ∈ {Active, Suspended, Blocked, Deactivated} `[FRS]`
- `UQ` RefreshToken(tokenHash); token rotation chain consistent; reuse-detection flags
- `UQ` OTPChallenge active-per-user (one outstanding challenge)
- `TR` account status transitions via M01 operations only

## M02/M03 Profile & Skills

- `UQ` UserProfile(userId) — one profile per user
- `UQ` UserTalentSkill(userId, skillId)
- `FK` UserTalentSkill.skillId → active TalentSkill enforced at write

## M04 Media

- `UQ` MediaAsset(objectKey) — backend-generated keys only
- `CK` upload/processing/moderation/visibility are **independent state fields** — never one combined status
- `FK` MediaVariant.mediaAssetId → MediaAsset
- `CK` checksum/size present post-upload-verification

## M06 Connect

- `UQ` ConversationMember(conversationId, userId)
- `UQ` Message(conversationId, sequence) — per-conversation ordering
- `UQ` MessageIdempotency(clientMessageId) → same messageId returned
- `UQ` MessageReceipt(messageId, userId)
- `TR` receipt transitions idempotent (delivered→read only forward)
- `UQ` UserBlock(blockerId, blockedId); `CK` blocker ≠ blocked
- `FK` MessageAttachment.mediaId → MediaAsset
- `TR` send rejected when UserBlock exists (either direction — rule open)

## M07 Creative Rooms

- `UQ` ProjectMember(roomId, userId)
- `UQ` ProjectInvitation(roomId, inviteeId) where status=Pending (one active invite)
- `FK` RequiredSkill.skillId → TalentSkill; `FK` ContributionRole.memberId → ProjectMember
- `TR` invite transitions Pending→{Accepted,Declined,Withdrawn,Expired} only
- `FK` ProjectTask.assigneeId → ProjectMember
- `UQ` FinalOutput room-scope *(cardinality open — if one-only then UQ(roomId))*
- `FK` ProjectCredit → ProjectMember + ProjectContributionRole (verified records only)

## M08 Portfolio

- `UQ` Portfolio(userId) — one-per-user **(DB-04 accepted)**
- `UQ` PortfolioItemMedia(itemId, mediaId)
- `FK` PortfolioItemContribution.contributionRef → M07 `ProjectCredit` **belonging to the same user** — cannot manufacture credit
- `CK` item visibility within allowed enum

## M09 Competitions

- `UQ` CompetitionCategory(competitionId, name)
- `UQ` CompetitionRound(competitionId, sequence)
- `UQ` CompetitionParticipant(competitionId, categoryId, participantRef)
- `CK` participant = userId XOR projectId (exactly one populated) — **DB-03 confirmed**; `participantType` present
- `CK` eligibility/submission configs versioned; published immutable
- `TR` competition config/participation/round states transition via module ops only

## M10 Submissions

- `UQ` Submission(participantId, roundId, attemptNo) — per `SubmissionConfig` limits *(limits open)*
- `FK` submission consistency: competitionId/categoryId/roundId/participantId all same-competition — cross-competition impossible
- `TR` `Finalized` immutable — post-finalize edits rejected; replacement = new version/submission
- `CK` server-generated `submittedAt` — never client-supplied
- `FK` SubmissionContributor.memberRef → M07 ProjectMember (live ref) **+ snapshot columns `snapshotMemberDisplay`,`snapshotRoleName`,`capturedAt` required at finalize (DB-02)**

## M11 Voting

- `UQ` Vote(voterId, submissionId, roundId) — per configured rule (dedup enforced at DB)
- `CK` targetType ∈ {individual, project} matching submission type
- `ID` Idempotency-Key uniqueness on cast endpoint
- `TR` reversal only via authorized state transition — never silent delete
- `CK` vote.castAt within configured window (server-side)

## M12 Judge Management

- `UQ` Judge(userId)
- `CK` Judge requires UserSystemRole=Judge
- `UQ` JudgeAssignment(judgeId, competitionId, categoryId, roundId) — no dup scope
- `TR` Revoked assignment → immediate access denial

## M13 Rubrics

- `CK` Σ criterion weights = 100% **required at publish** (draft may be incomplete)
- `IM` `EvaluationTemplateVersion` + `EvaluationCriterion` **immutable once published** — changes → new version
- `FK` JudgeEvaluation.rubricVersionId → published version only
- `FK` JudgeEvaluation.assignmentId → active JudgeAssignment covering submission
- `UQ` JudgeEvaluation(judgeId, submissionId, roundId) — one eval per judge/entry/round **(DB-09 accepted)**; corrections via authorized reopen/amend
- `UQ` JudgeEvaluationCriterionScore(evaluationId, criterionId)
- `CK` score within configured scale

## M14 Scoring

- `CK` ScoringConfiguration: weightA + weightJ = 100% exactly
- `IM` published Scoring/TieBreak/Qualification configs immutable
- `UQ` FinalScore(submissionId, configVersionIds) — reproducible result
- `FK` ScoreOverride → FinalScore; reason mandatory; before/after recorded — **never touches Vote/JudgeEvaluation**

## M15 Progression

- `UQ` ProgressionRecord(submissionId, roundId, configVersion) — one decision per entry/round/version
- `IM` finalized ProgressionRecord immutable — correction via ProgressionOverride only
- `FK` ProgressionRecord references M14 FinalScore + QualificationResult versions
- `CK` team entry progresses as one — `participantId` resolves to entry, never member-split

## M16 Leaderboards

- `FK` LeaderboardProjection rows → M14 result + M15 progression refs — never standalone values
- `CK` publication state independent of result state (finalization ≠ publication)
- `IM` LeaderboardSnapshot sealed — references versions

## M17 Notifications

- `UQ` NotificationEventReference(sourceModule, eventType, sourceId, version) — dedup
- `UQ` NotificationPreference(userId, type, channel)
- `UQ` NotificationReadState(notificationId, userId)
- `TR` delivery transitions idempotent; terminal states respected

## M18 Moderation

- `FK` report.targetRef typed — must reference a reportable entity
- `IM` ModerationDecision never overwritten — appeal creates new record
- `UQ` ModerationRestriction(target, type, active-window) — no duplicate active restrictions
- `CK` actor + reason mandatory on every decision/action

## M21 Social Engagement

- `UQ` Follow(followerId, followeeId); `CK` follower ≠ followee — **user-only targets (DB-06)**
- `UQ` Like(userId, targetType, targetId); `CK` targetType ∈ {MEDIA, PORTFOLIO} **(DB-01)**; `Comment` same target enum; no `parentCommentId`/`editedAt` **(DB-05)**
- `FK` target refs must resolve to a live, visible, non-restricted entity at write
- `CK` EngagementCounter never written as business input — derived only

## Platform Kernel

- `IM` AuditLog append-only — no UPDATE/DELETE; insert-only pattern `[FRS §30]`
- `UQ` PlatformConfig(configKey)
- every AuditLog row: actor + action + target + timestamp + correlationId mandatory; before/after where applicable

## Global

- All PKs: UUIDv7-style time-ordered (server-generated)
- `clientMessageId`/idempotency keys are dedup keys, never PKs
- Cross-module references validated at application layer + documented read-contracts (no cross-module FK enforcement per ADR-001 module boundaries)
