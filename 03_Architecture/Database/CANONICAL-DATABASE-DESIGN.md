# StarMitra — Canonical Database Design

**Status:** Canonical logical model — design only | **Inputs:** FRS v1.1 · Baseline v1.0 · ADR-001…013 · M01–M21 · Consistency Review + Addendum | **Companion docs:** [CANONICAL-ENTITY-INVENTORY.md](CANONICAL-ENTITY-INVENTORY.md) · [CANONICAL-ENTITY-RELATIONSHIP-MATRIX.md](CANONICAL-ENTITY-RELATIONSHIP-MATRIX.md) · [DATABASE-CONSTRAINT-REGISTER.md](DATABASE-CONSTRAINT-REGISTER.md) · [DATABASE-INDEX-STRATEGY.md](DATABASE-INDEX-STRATEGY.md) · [DATABASE-OPEN-DECISIONS.md](DATABASE-OPEN-DECISIONS.md)

> **This is the logical model — no migrations, no DDL, no ORM artifacts.** Physical schema follows only after acceptance.

## 1. Canonical Principles

1. **Single-owner rule:** every authoritative entity has exactly one owning module; other modules reference by ID or read-contract — never duplicate ownership.
2. **PostgreSQL is authoritative** (ADR-003); derived projections are always rebuildable from source.
3. **Media binaries live in object storage** (ADR-008) — the schema stores metadata/references only; no BLOBs.
4. **Versioned configuration:** published rubrics, scoring/tie-break/qualification/progression configs are immutable versions — results remain reproducible.
5. **State separation:** upload ≠ processing ≠ moderation ≠ visibility ≠ submission ≠ scoring ≠ qualification ≠ progression ≠ publication — independent dimensions, never collapsed.
6. **Audit ≠ telemetry ≠ analytics:** `AuditLog` is Platform Kernel-owned, append-only, written transactionally with business ops.
7. **Idempotency by constraint:** duplicates are prevented by unique constraints + idempotency keys — never application memory alone.
8. **Polymorphic references** use typed references (`targetType` + `targetId`); scope decisions that are product-open stay open (marked AMBER, not silently resolved).

## 2. Canonical Entity Inventory — by module

### M01 Authentication & Identity (6 owned + 1 deferred)

| Entity | Type | Key | Purpose | Notes |
|--------|------|-----|---------|-------|
| `User` | Authoritative | `userId` | identity + account status | status: Active/Suspended/Blocked/Deactivated `[FRS]` |
| `SystemRole` | Authoritative | `roleId` | capability roles (User/Admin/SuperAdmin/Judge…) | taxonomy = config, not code |
| `UserSystemRole` | Authoritative | (userId,roleId) | role assignment | unique pair |
| `RefreshToken` | Authoritative | `tokenId` | opaque persisted session | rotation+revocation+reuse detection |
| `OTPChallenge` | Authoritative | `challengeId` | OTP challenge state | ephemeral lifecycle |
| `AuthenticationAuditEvent` | Authoritative | `eventId` | auth events → feeds kernel `AuditLog` | append-only |
| `PasswordCredential` | **Deferred/open** | — | optional password support | open PO decision — not modeled |

### M02 User Profile (1 owned)

| `UserProfile` | Authoritative | `userId` (unique) | presentation: display name, bio, location, avatar **mediaId ref**, visibility state | blocked/suspended effects mirror M01 state |

### M03 Talent Skills (3 owned)

| `TalentSkill` | Authoritative | `skillId` | capability taxonomy (admin-configured) | configurable list — never hard-coded |
| `UserTalentSkill` | Authoritative | (userId,skillId) | user↔skill association | unique pair; multi-skill allowed; no primary-skill dependency |
| `SkillProficiency` | Authoritative (config) | `proficiencyId` | optional proficiency levels | optional — open whether used |

### M04 Media Management (2 owned)

| `MediaAsset` | Authoritative | `mediaId` | binary lifecycle metadata — object-key, type, size, checksum, state dims | 4D state: upload/processing/moderation/visibility — separate fields, never one status |
| `MediaVariant` | Authoritative | `variantId` | processed derivatives (thumbnail/preview/transcode) | regenerable; refs MediaAsset |

### M05 Discovery / Search / Feed (0 authoritative — 2 derived)

| `DiscoveryProjection` | Derived | — | read model for discovery surfaces | rebuildable |
| `FeedProjection` | Derived | — | feed read model | rebuildable; never authoritative |

### M06 StarMitra Connect (7 owned)

| `Conversation` | Authoritative | `conversationId` | 1:1/group/project-linked | `projectId` ref → M07 |
| `ConversationMember` | Authoritative | (conversationId,userId) | membership | unique pair; standard name per CM-04 |
| `Message` | Authoritative | `messageId` (server) + `clientMessageId` | message + ordering | per-conversation sequence |
| `MessageReceipt` | Authoritative | (messageId,userId) | delivered/read state | idempotent transitions |
| `MessageAttachment` | Authoritative | `attachmentId` | message→media link | mediaId ref → M04 |
| `UserBlock` | Authoritative | (blockerId,blockedId) | **user-initiated privacy** | ≠ M18 `ModerationRestriction` (CM-03) |
| `MessageIdempotency` | Authoritative | `clientMessageId` | send dedup | unique constraint |

### M07 Creative Rooms (9 owned)

| `CreativeRoom` | Authoritative | `roomId` | project container | lifecycle = proposal/open |
| `ProjectMember` | Authoritative | (roomId,userId) | membership — **authoritative** | M06/M10 read-contract it; never duplicated |
| `ProjectContributionRole` | Authoritative | `roleId` | contextual contribution | **≠ TalentSkill**; explicit assignment |
| `RequiredSkill` | Authoritative | (roomId,skillId) | declared skill need | ref → M03 |
| `ProjectInvitation` | Authoritative | `invitationId` | invite lifecycle | unique active invite per room+invitee |
| `ProjectTask` | Authoritative | `taskId` | work item | lean scope — no PM-suite |
| `ProjectAsset` | Authoritative | `assetId` | room→media link | mediaId ref → M04 |
| `FinalOutput` | Authoritative | `outputId` | completed output | mediaId ref; cardinality open |
| `ProjectCredit` | Authoritative | `creditId` | **verified contribution record** | consumed by M08; never claimable unilaterally |

### M08 Portfolio (4 owned)

| `Portfolio` | Authoritative | `userId` | showcase container | **one-per-user accepted — `UQ(userId)` (DB-04)**; extensible to multi later |
| `PortfolioItem` | Authoritative | `itemId` | showcase entry | skill refs; visibility; ordering |
| `PortfolioItemMedia` | Authoritative | (itemId,mediaId) | item→media link | M04 ref |
| `PortfolioItemContribution` | Authoritative | `refId` | verified-credit link | → M07 `ProjectCredit` — never manufactures contribution |

### M09 Competitions (7 owned)

| `Competition` | Authoritative | `competitionId` | definition + lifecycle | 3-dim state: config/participation/round |
| `CompetitionCategory` | Authoritative | `categoryId` | grouping | multiple per competition |
| `CompetitionCategorySkill` | Authoritative | (categoryId,skillId) | skill association | ref → M03; eligibility/discovery only |
| `CompetitionRound` | Authoritative | `roundId` | round structure + config refs | **owned here — M15 never duplicates** |
| `EligibilityRule` | Authoritative | `ruleId` | entry rules | business rule ≠ authorization |
| `SubmissionConfig` | Authoritative | `configId` | submission rules | consumed authoritatively by M10 |
| `CompetitionParticipant` | Authoritative | `participantId` | participant (user XOR project) | `participantType` + `userId`/`projectId` nullable + `CK` exactly-one **(DB-03 confirmed)** |

### M10 Submissions (4 owned)

| `Submission` | Authoritative | `submissionId` | the competition entry | finalized→immutable evidence |
| `SubmissionMedia` | Authoritative | `refId` | submission→media link | frozen at finalize |
| `SubmissionContributor` | Authoritative | `refId` | contributor context | → M07 membership/role **+ evidence-grade snapshot** (`snapshotMemberDisplay`, `snapshotRoleName`, `capturedAt`) populated at finalization **(DB-02)** |
| `SubmissionHistory` | Authoritative | `historyId` | state trail | append-only audit of transitions |

### M11 Audience Voting (2 owned + derived)

| `Vote` | Authoritative | `voteId` | the audience action | targets `Submission` — **never split for team entries** |
| `VoteConfig` | Authoritative | `configId` | voting **behavior** config | M09 holds only structural ref (CM-05) |
| `VoteCount` *(projection)* | Derived | — | aggregated counts | rebuildable from `Vote` — never authoritative |

### M12 Judge Management (3 owned + 1 optional)

| `Judge` | Authoritative | `judgeId` (userId unique) | domain judge identity | requires `SystemRole=Judge` |
| `JudgeExpertise` | Authoritative | `expertiseId` | judging qualification | ≠ `TalentSkill` — never auto-access |
| `JudgeAssignment` | Authoritative | `assignmentId` | scope grant | **the authz boundary** — scoped comp/cat/round |
| ~~`AssignmentScope`~~ | **not created** | — | inline scope on JudgeAssignment | **DB-08 confirmed** — `competitionId`+nullable `categoryId`/`roundId` inline; submission-level scope = future |

### M13 Judge Rubrics (5 owned)

| `EvaluationTemplate` | Authoritative | `templateId` | rubric container | |
| `EvaluationTemplateVersion` | Authoritative + **immutable** | `versionId` | published snapshot | never modified post-publish |
| `EvaluationCriterion` | within version | `criterionId` | scored dimension | weights total 100% at publish |
| `JudgeEvaluation` | Authoritative | `evaluationId` | completed evaluation | binds `rubricVersionId` exactly |
| `JudgeEvaluationCriterionScore` | Authoritative | (evaluationId,criterionId) | per-criterion input | unique pair |

### M14 Scoring & Ranking (9 owned)

| `ScoringConfiguration` | Authoritative + versioned | `configId+version` | audience/judge weighting | weights=100%; published immutable |
| `TieBreakConfiguration` | Authoritative + versioned | version | ordered criteria | deterministic |
| `QualificationConfiguration` | Authoritative + versioned | version | threshold/count rules | |
| `JudgeScoreAggregation` | Derived | — | judge aggregate per submission | rebuildable |
| `AudienceScoreAggregation` | Derived | — | audience aggregate | rebuildable from M11 votes |
| `FinalScore` | Authoritative (sealed result) | `resultId` | reproducible weighted outcome | bound to config versions |
| `Ranking` | Derived snapshot | — | deterministic order | snapshot per context |
| `QualificationResult` | Derived | — | qualifies/not | consumed by M15 |
| `ScoreOverride` | Authoritative (audit) | `overrideId` | authorized adjustment | never overwrites source Vote/Eval |

### M15 Round Progression (3 owned)

| `ProgressionConfiguration` | Authoritative + versioned | version | progression rules | **M15-owned (CM-06)**; immutable once begun |
| `ProgressionRecord` | Authoritative | `recordId` | advance/eliminate decision | references M14 result versions |
| `ProgressionOverride` | Authoritative (audit) | `overrideId` | authorized progression change | ≠ `ScoreOverride` — separate audit event |

### M16 Leaderboards (3 owned)

| `LeaderboardProjection` | Derived | — | rebuildable read model | every field traced upstream |
| `LeaderboardPublication` | Authoritative | `publicationId` | visibility state | publication ≠ finalization |
| `LeaderboardSnapshot` | Derived (sealed ref) | `snapshotId` | historical reference | → result/config versions |

### M17 Notifications (6 owned)

| `Notification` | Authoritative | `notificationId` | delivered item | source-ref + deep-link (never grants access) |
| `NotificationPreference` | Authoritative | (userId,type,channel) | user prefs | can't suppress mandatory |
| `NotificationTemplate` | Authoritative + versioned | `templateId+version` | content template | no arbitrary scripting |
| `NotificationDeliveryAttempt` | Authoritative | `attemptId` | delivery record | durable retry state |
| `NotificationReadState` | Authoritative | (notificationId,userId) | read state | idempotent |
| `NotificationEventReference` | Authoritative | `eventRefId` | source dedup key | `{module,type,sourceId,version}` |

### M18 Moderation (8 owned)

| `ModerationReport` | Authoritative | `reportId` | report → typed target ref | reporter privacy protected |
| `ModerationCase` | Authoritative | `caseId` | case grouping | merge/escalation open |
| `ModerationDecision` | Authoritative | `decisionId` | the ruling | never overwritten (appeal = separate) |
| `ModerationAction` | Authoritative | `actionId` | enforcement instruction | owning module applies state transition |
| `ModerationEvidenceReference` | Authoritative | `evidenceId` | evidence link | refs only — never copied binaries |
| `ModerationRestriction` | Authoritative | `restrictionId` | **admin-enforced** restriction | ≠ M06 `UserBlock` |
| ~~`ModerationAppeal`~~ | **DEFERRED (DB-07)** | — | extension point only | not an MVP physical table; add later |
| `ModerationPolicyReference` | Authoritative + versioned | `policyVersion` | policy taxonomy | historical decisions reproducible |

### M19 Admin Portal / M20 Judge Portal — **0 entities**

Presentation/orchestration only. No persistence beyond their consumed domains.

### M21 Social Engagement (4 owned)

| `Follow` | Authoritative | (followerId,followeeId) | user→user follow | other targets open |
| `Like` | Authoritative | (userId,targetRef) | like on target | **≠ Vote (M11)** — hard boundary |
| `Comment` | Authoritative | `commentId` | comment on target | edit/reply open |
| `EngagementCounter` | Derived | targetRef | counts projection | rebuildable — never business-authoritative |

### Platform Kernel (3 owned)

| `AuditLog` | Authoritative, append-only | `auditId` | platform audit trail | all modules append via contract |
| `PlatformConfig` | Authoritative | `configKey` | platform configuration | kernel-owned |
| `AnalyticsProjection` | Derived | — | ADR-013 reporting projections | never business-authoritative |

## 3. Identity Model (canonical)

`User` (identity + account status) ↔ `UserSystemRole` ↔ `SystemRole`. Sessions via `RefreshToken` (rotation/revocation/reuse-detection); `OTPChallenge` for primary OTP auth; `PasswordCredential` **deferred/open**. Preserved: `User` ≠ `SystemRole` ≠ `TalentSkill` ≠ `ProjectContributionRole`. `SystemRole` grants capability; `JudgeAssignment` grants judge scope; nothing else grants access.

## 4. Talent Model (canonical)

`User 1—N UserTalentSkill N—1 TalentSkill` (+ optional `SkillProficiency`). Multi-skill supported; no primary-skill column; skills never authorization. `UserTalentSkill` unique (user,skill).

## 5. Profile

`UserProfile` owns presentation only; avatar/banner via `mediaId` refs → M04; blocked/suspended presentation mirrors M01 account state (never owns it); skills displayed via M03 read-contract.

## 6. Media — attachment pattern (canonical)

**M04 owns `MediaAsset` + `MediaVariant` (binary lifecycle).** Business modules own their attachment-link entities:

| Link entity | Owner | Target |
|-------------|-------|--------|
| profile `avatarMediaId`/`bannerMediaId` | M02 | field on UserProfile |
| `PortfolioItemMedia` | M08 | item→media |
| `SubmissionMedia` | M10 | submission→media (frozen at finalize) |
| `ProjectAsset` | M07 | room→media |
| `MessageAttachment` | M06 | message→media |
| `ModerationEvidenceReference` | M18 | case→media/any target |

Access: backend authz → short-lived signed URL; storage ACL second layer. No BLOBs.

## 7. Social Engagement (M21)

`Follow` user→user **(MVP-locked — DB-06)**; `Like`/`Comment` polymorphic `targetRef` with `targetType ∈ {MEDIA, PORTFOLIO}` **(MVP — DB-01)**; `Comment` = create+delete only, no edit/threading **(DB-05)** — extensible `targetRef`/`parentCommentId`-addable shapes preserved for future. `EngagementCounter` derived from source rows. **`Like` ≠ `Vote` — separate entities, separate modules.**

## 8. Creative Rooms

`CreativeRoom → ProjectMember → ProjectContributionRole`; `RequiredSkill` refs M03; `ProjectInvitation` unique-active constraint; `ProjectAsset`/`FinalOutput` ref M04; `ProjectCredit` = verified record consumed by M08. Membership authoritative here — M06 project-conversations validate via read-contract.

## 9. Portfolio

`Portfolio → PortfolioItem → {PortfolioItemMedia, PortfolioItemContribution}` — contribution items **reference M07 `ProjectCredit`**; never manufacture contribution. One-portfolio-per-user = proposal.

## 10. Competition Domain

`Competition → CompetitionCategory → {CompetitionCategorySkill→M03}; Competition → CompetitionRound → config refs; EligibilityRule + SubmissionConfig consumed downstream; CompetitionParticipant` polymorphic (user XOR project→M07). 3 state dims separate; `CompetitionRound` never duplicated in M15.

## 11. Submissions

`CompetitionParticipant → Submission → {SubmissionMedia→M04, SubmissionContributor→M07}`. **Team = ONE entry** — never split; votes attach to the entry. Finalize → immutable evidence; post-finalize change = new submission/version, never silent overwrite.

## 12. Audience Voting

`Vote` authoritative; targets `Submission` (+competition/category/round context + targetType); `clientMessageId`-style idempotency via unique `(voterId, submissionId, roundId)` per configured rules; counts derived. `VoteConfig` = M11 behavior config (M09 holds structural ref). **No contributor allocation collection — project vote = one record on the entry.**

## 13. Judge Management

`Judge (userId unique, requires SystemRole=Judge) → JudgeAssignment (judgeId + compId + optional catId + optional roundId)`. `JudgeExpertise` = qualification refs only. Assignment = the authz boundary; expertise never grants access.

## 14. Judge Rubrics

`EvaluationTemplate → EvaluationTemplateVersion (immutable) → EvaluationCriterion (weights=100% at publish)`. `JudgeEvaluation` binds `rubricVersionId` + `assignmentId` + `submissionId`; `JudgeEvaluationCriterionScore` unique (eval,criterion). Criterion weight ≠ audience/judge weight — different layers.

## 15. Scoring / Ranking

Three config entities versioned+immutable (`Scoring`/`TieBreak`/`Qualification`); aggregates derived; `FinalScore` sealed reproducible result bound to input+config versions; `Ranking` derived snapshot; `QualificationResult` consumed by M15; `ScoreOverride` audited never-overwrites-source.

## 16. Round Progression

`ProgressionConfiguration` M15-owned+versioned; `ProgressionRecord` references M14 result/config versions + tie outcome + overrides; `ProgressionOverride` separate audit event. M09 rounds / M14 qualification consumed — never recalculated.

## 17. Leaderboard

`LeaderboardProjection` (derived read model) + `LeaderboardPublication` (authoritative visibility state) + `LeaderboardSnapshot` (sealed ref to versions). Publication ≠ finalization; never authoritative for score/rank.

## 18. Notifications

`Notification` (recipient+type+body+sourceRef+deepLink), `NotificationPreference`, `NotificationTemplate` (versioned), `NotificationDeliveryAttempt`, `NotificationReadState`, `NotificationEventReference` (dedup key). Source business state stays with owning module; delivery failure never mutates it.

## 19. Moderation

`Report → Case → Decision → Action` + `EvidenceReference` + `Restriction` + `Appeal` (open) + `PolicyReference` (versioned). Report target = typed polymorphic ref. **M18 decides; owning module enforces.** `UserBlock`(M06, user-privacy) ≠ `ModerationRestriction`(M18, admin).

## 20. Portals

M19/M20 own **zero entities** — their persistence requirements are entirely satisfied by their consumed domains.

## 21. Platform Kernel

`AuditLog` (append-only, transactional writes via kernel contract — **not telemetry, not analytics**), `PlatformConfig`, `AnalyticsProjection` (ADR-013 derived). M19 reads audit; never owns it.

## 22. Polymorphic References — all flagged

| Relationship | Model | Status |
|--------------|-------|--------|
| `Like`/`Comment` target | `targetType`+`targetId` typed ref | **RESOLVED (DB-01)** — enum = {MEDIA, PORTFOLIO} MVP |
| `Follow` target | followerId→followeeId | **RESOLVED (DB-06)** — user-only MVP; extensible shape retained |
| `CompetitionParticipant` | `participantType` + userId XOR projectId | **RESOLVED (DB-03)** — single typed table confirmed |
| `SubmissionContributor` | memberRef + snapshot fields | **RESOLVED (DB-02)** — snapshot columns required at finalize |
| `ModerationEvidenceReference`/`Report.target` | typed ref to any reportable entity | consistent pattern |
| `Notification.sourceRef` + deepLink | typed ref | presentation-only |
| `LeaderboardProjection` source | refs to result/entry | derived |

## 23. Primary Keys

UUIDv7-style time-ordered identifiers for all primary keys — externally safe (non-enumerable), naturally sortable, monolithic-friendly. Server-generated always; `clientMessageId` is a *dedup key*, not an ID. **Physical type decision deferred to schema phase.**

## 24. High-Volume Entities (evaluated — no partitioning per ADR-003)

`Message`, `Vote`, `Like`, `Comment`, `Notification`, `AuditLog`, `EngagementCounter`, read-model projections — access patterns + index needs documented in index strategy; **partitioning deferred** per ADR-003 trigger model.

## 25. Versioning / Immutability — reproducibility chain

`EvaluationTemplateVersion` (rubric) → `JudgeEvaluation`(binds versionId) → `Scoring/TieBreak/Qualification` config versions → `FinalScore`(binds all versions) → `ProgressionConfiguration` version → `LeaderboardSnapshot` (binds result versions) → publication. Historical results reproducible at every layer; `Submission` evidence immutable post-finalize.

## 26. Audit / Retention / Tenancy

- **Audit:** one kernel `AuditLog` — actor, action, target, before/after, reason, correlationId — modules never duplicate
- **Retention:** classification only (hard-delete / soft-delete / immutable / archive / **policy-open**) — no durations invented; PO/legal review flagged for evidence/moderation/auth data
- **Tenancy:** single-tenant — multi-tenancy **OPEN future decision**, not modeled

## 27. Readiness Matrix Summary

- **GREEN** (~100% of MVP entities): DB-01…DB-09 resolved — every entity is now sufficiently defined for physical schema
- **AMBER** (remaining — **config-value level only, never schema-shape**): lifecycle status vocabularies, retention durations, scoring formulas/tie-breaks, vote reversal, deadline precedence, taxonomies (DB-10…DB-20)
- **RED:** none

Full registers: [DATABASE-OPEN-DECISIONS.md](DATABASE-OPEN-DECISIONS.md).

## 28. Final Review

**A. Canonical entity count:** ~90 concepts
**B. Authoritative:** ~72
**C. Derived/projection:** ~12 (M05×2, M11 counts, M14 aggregates+ranking+qualification, M16 projection+snapshot, M21 counter, kernel analytics)
**D. Platform Kernel:** `AuditLog` + `PlatformConfig` + `AnalyticsProjection` (3)
**E. AMBER decisions:** all resolved (DB-01…DB-09 — see `DATABASE-OPEN-DECISIONS.md`); remaining open items are config-value level (DB-10…DB-20)
**F. RED:** none
**G. Constraints needing resolution before physical schema:** **none — DB-01…DB-09 gate cleared**; evidence-retention *durations* remain open policy (columns/flags exist, values = PO)
**H. Recommended next phase:** **Physical Database / Schema Design** → Flyway strategy → implementation schema → validation. **Not started.**

---

*Consistency note: this model presumes the CM-01…CM-07 resolutions — `UserBlock` (M06), `ConversationMember` (standard), `VoteConfig` (M11), `ProgressionConfiguration` (M15), `/api/v1/judges/me` (uniform), M21 owner of social engagement, Platform Kernel owner of audit/config.*
