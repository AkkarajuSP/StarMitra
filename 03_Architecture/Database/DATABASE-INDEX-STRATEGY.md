# Database Index Strategy — Logical Index Requirements

Logical index requirements only — no physical DDL. Per ADR-003/011: PostgreSQL-native indexes; GIN for FTS; `pg_trgm` where justified; **no speculative indexes, no partitioning at MVP**.

## Priority-Ordered Requirements

### Authentication / Session (M01)
- `User` — login-identifier lookup (unique)
- `RefreshToken` — tokenHash unique; userId (revocation queries)
- `OTPChallenge` — userId + expiry

### Profile / Discovery (M02/M05)
- `UserProfile` — userId unique; displayName (`pg_trgm` fuzzy search per ADR-011); visibility + status filters; location *(if search dimension confirmed — open)*
- FTS vector column(s) — `tsvector` + GIN over documented searchable fields

### Talent Skills (M03)
- `TalentSkill` — name unique; status
- `UserTalentSkill` — (skillId) for "users with skill X"; (userId) for profile display

### Social Engagement (M21)
- `Follow` — followerId (my follows); followeeId (my followers + counts)
- `Like` — (targetType, targetId) for target counts; userId for user's likes
- `Comment` — (targetType, targetId) + createdAt ordering *(no parentCommentId in MVP — DB-05; targets = MEDIA/PORTFOLIO — DB-01)*
- `EngagementCounter` — targetRef unique

### Messaging (M06)
- `ConversationMember` — userId (my conversations); conversationId
- `Message` — **(conversationId, sequence) unique** — history cursor pagination
- `MessageReceipt` — (messageId, userId)
- `MessageIdempotency` — clientMessageId unique
- `UserBlock` — (blockerId, blockedId)

### Creative Rooms (M07)
- `CreativeRoom` — ownerId; status; visibility (discovery queries)
- `ProjectMember` — roomId; userId
- `ProjectInvitation` — inviteeId (my invites); (roomId,inviteeId,status=Pending) unique
- `ProjectTask` — roomId; assigneeId
- `RequiredSkill` — roomId; skillId

### Portfolio (M08)
- `Portfolio` — userId unique
- `PortfolioItem` — portfolioId + ordering; visibility; skill-tag refs

### Competitions (M09)
- `Competition` — status + visibility (discovery listing)
- `CompetitionCategory` — competitionId
- `CompetitionRound` — (competitionId, sequence)
- `CompetitionParticipant` — competitionId+categoryId; participantRef lookups
- `EligibilityRule`/`SubmissionConfig` — context refs

### Submissions (M10)
- `Submission` — competitionId+roundId (round listing); participantId (my submissions); state (admin/review queues); submittedAt (deadline audits)
- `SubmissionMedia` — submissionId
- `SubmissionContributor` — submissionId; memberRef

### Voting (M11)
- `Vote` — **(voterId, submissionId, roundId) unique** — dedup; submissionId (count aggregation); competitionId+roundId
- `VoteConfig` — context refs

### Judge (M12)
- `Judge` — userId unique
- `JudgeAssignment` — judgeId (my assignments); (competitionId, categoryId, roundId) for scope resolution — **inline scope fields, no scope table (DB-08)**; status

### Rubrics / Evaluations (M13)
- `EvaluationTemplateVersion` — templateId + version; context refs (applicability resolution)
- `JudgeEvaluation` — judgeId (my evals); submissionId; (judgeId,submissionId,roundId) unique
- `JudgeEvaluationCriterionScore` — evaluationId

### Scoring / Progression / Leaderboard (M14–M16)
- `FinalScore` — (competitionId, categoryId, roundId) context; submissionId
- `Ranking` — context + rank (cursor ordering)
- `ProgressionRecord` — (roundId, submissionId) unique; context
- `LeaderboardProjection` — (competitionId, categoryId, roundId) + rank (cursor)
- `LeaderboardPublication` — context + state

### Notifications (M17)
- `Notification` — recipientId + createdAt (inbox cursor); (recipientId, readState) unread counts
- `NotificationDeliveryAttempt` — notificationId; status+retry
- `NotificationEventReference` — dedup key unique

### Moderation (M18)
- `ModerationReport` — targetRef; status+queue; createdAt
- `ModerationCase` — status (queue); assignedModeratorId
- `ModerationRestriction` — targetRef + active window

### Audit (Platform Kernel)
- `AuditLog` — targetRef; actorId; correlationId; createdAt — **append-only**; query patterns per M19 audit views

## Explicitly Deferred / Rejected

- Partitioning — deferred per ADR-003 (trigger-based future)
- Redis cache indexes — no distributed cache (ADR-010)
- Elasticsearch/OpenSearch — rejected for MVP (ADR-011)
- Speculative indexes on low-volume entities — avoided
