# StarMitra — Domain Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Draft for review

The 27 FRS capability areas consolidate into 15 bounded domains. Consolidation principle: **a domain exists where there is a distinct ownership boundary and data lifecycle — not merely because a menu item or screen exists** `[Proposed]`. Under the proposed modular-monolith topology, each domain is an internal module with exclusive data ownership and explicit contracts.

## Domain Map

```text
D1 Identity & Access ──────────────────────── every domain depends on it
D2 Talent Profile ────────── D4 Social Engagement ── D5 Discovery
        │                          │
D3 Media ────────────────────┘                          │
        │                                               │
D7 Collaboration ◄──── D6 Messaging (StarMitra Connect) │
        │                                               │
D8 Competition ── D9 Submission ── D10 Voting ──┐       │
                       │           D11 Judging ─┤       │
                       └──────────► D12 Scoring & Ranking ◄┘
D13 Notification ◄──── (events from nearly all domains)
D14 Moderation ──────── (references content/users/messages across domains)
D15 Administration & Audit ── (config for many domains; audit observes all)
```

---

## D1 — Identity & Access

| Aspect | Detail |
|--------|--------|
| **Purpose** | Account lifecycle, authentication, system-role authorization `[FRS §6][§8]` |
| **Responsibilities** | Registration (mobile/email + OTP or configured mechanism); login/logout; password reset where enabled; account status machine (Active/Suspended/Blocked/Deactivated); consent capture (Terms, Privacy, guidelines); assignment and enforcement basis for system roles |
| **Major entities** | `User`, `SystemRole`, `UserSystemRole`, `AuthCredential`/`OtpChallenge` `[Proposed]`, `ConsentRecord` `[Proposed]` |
| **Dependencies** | OTP/SMS/email providers `[OD-5]`; consumed by every domain for authN/authZ |
| **APIs/events** | `POST /auth/register`, `/auth/otp/verify`, `/auth/login`, `/auth/logout`, `/auth/password-reset`; admin role management endpoints. Events: `UserRegistered`, `AccountStatusChanged` |
| **Ownership boundary** | Owns identity credentials and system-role assignments. Does NOT own talent skills (D2) or judge-competition assignment (D11). A user's "Judge" system role lives here; *what* they judge lives in D11 `[FRS §19]` |
| **Data ownership** | `User`, `SystemRole`, `UserSystemRole`, auth artifacts |

`[FRS §6]` System roles: Audience/User, Creator/Talent capability, Judge, Admin, Super Admin. A user may hold several simultaneously; creator capability + judge permission may coexist per business rules.

## D2 — Talent Profile

| Aspect | Detail |
|--------|--------|
| **Purpose** | Public-facing identity: profile, talent skills, skill-grouped portfolio `[FRS §7][§9]` |
| **Responsibilities** | Profile fields (photo, banner, display name, bio, location); multi-skill selection with optional proficiency (Beginner/Intermediate/Advanced/Professional); admin-configured skill taxonomy with parent grouping, display order, competition/judge-rubric eligibility flags; portfolio items grouped by skill; awards/achievements/external links; followers/following counts; competition history & project credits (read projections); privacy controls; verified badges |
| **Major entities** | `Profile` (or fields on `User` `[Open]`), `TalentSkill`, `UserTalentSkill`, `SkillProficiency`, `PortfolioItem`, `PortfolioCredit` `[Proposed]` |
| **Dependencies** | D1 (identity), D3 (portfolio media), D7 (project credits source), D8 (participation history source) |
| **APIs/events** | Profile CRUD; `PUT /users/me/skills`; `GET /users/{id}/portfolio`; taxonomy admin endpoints. Events: `SkillAdded/Removed`, `PortfolioItemPublished`, `ProfileUpdated` |
| **Ownership boundary** | Owns *claims* about the person. Credits/history are projections of D7/D8 facts — D2 never originates competition or project truth |
| **Data ownership** | `TalentSkill` taxonomy, `UserTalentSkill`, `PortfolioItem`, `PortfolioCredit` (projection `[Open]`) |

Key rules: skills add/remove must not delete portfolio `[UAT-03]`; skill changes never alter system permissions `[BR-02]`; taxonomy extensible without code changes `[FRS §7]`.

## D3 — Media

| Aspect | Detail |
|--------|--------|
| **Purpose** | Upload, process, store, deliver creative media `[FRS §10]` — detail in [MEDIA-ARCHITECTURE](MEDIA-ARCHITECTURE.md) |
| **Responsibilities** | Upload intake (video/audio/image/documents); metadata (title, description, skill/category, tags, visibility); status lifecycle Draft→Processing→Published→Hidden/Rejected/Removed; ownership/attribution; engagement counters; delivery with visibility enforcement |
| **Major entities** | `MediaAsset`, `MediaVariant` (transcodes/thumbnails `[Proposed]`), `MediaModerationRef` |
| **Dependencies** | Storage/CDN/transcode providers `[OD-6]`; D14 moderation hooks; referenced by D2, D7, D9 |
| **APIs/events** | Upload-init (pre-signed), upload-complete, metadata CRUD, status transitions. Events: `MediaUploaded`, `MediaProcessed`, `MediaPublished`, `MediaRejected` |
| **Ownership boundary** | Owns binary lifecycle + metadata. Other domains *reference* MediaAsset by ID; they never store binaries |
| **Data ownership** | `MediaAsset`, `MediaVariant` |

## D4 — Social Engagement

| Aspect | Detail |
|--------|--------|
| **Purpose** | Audience interaction primitives `[FRS §4.1][§11]` |
| **Responsibilities** | Follow/unfollow; likes; comments; engagement counters feeding discovery; report hooks to D14 |
| **Major entities** | `Follow`, `Like`, `Comment`, `EngagementCounter` `[Proposed]` |
| **Dependencies** | D1, D2 (profiles), D3 (content), D14 (comment moderation) |
| **APIs/events** | `POST /follows`, `/likes`, `/comments`. Events: `UserFollowed`, `ContentLiked`, `CommentAdded` |
| **Ownership boundary** | Generic engagement on profiles/content. Competition *votes* are NOT engagement — they live in D10 with audit and abuse rules `[BR-14]` |
| **Data ownership** | `Follow`, `Like`, `Comment` |

## D5 — Discovery

| Aspect | Detail |
|--------|--------|
| **Purpose** | Feed, browse, search, trending `[FRS §11]` |
| **Responsibilities** | Personalized feed (follows, skills, engagement); browse by skill/category; trending; competition content; project/room showcases; search across talent/content/projects/competitions; filters |
| **Major entities** | Read models only `[Proposed]`: `FeedEntry`, `SearchDocument`, `TrendingSnapshot` |
| **Dependencies** | Consumes events from D2/D3/D4/D7/D8; search engine `[Open OD-10/OD-12 adjacent]` |
| **APIs/events** | `GET /feed`, `/discover`, `/search`. Consumes most publish events |
| **Ownership boundary** | Read-optimized projections; never a system of record |
| **Data ownership** | Its own derived/read-model stores only |

## D6 — Messaging (StarMitra Connect)

| Aspect | Detail |
|--------|--------|
| **Purpose** | Internal communication layer `[FRS §12]` |
| **Responsibilities** | 1:1, group, and project-linked conversations; text, image, audio, short video, document messages; timestamps, delivery/read status; report/block controls; per-user notification controls |
| **Major entities** | `Conversation`, `ConversationParticipant`, `Message`, `MessageAttachment` (→ `MediaAsset`), `Block` |
| **Dependencies** | D1, D3 (attachments), D7 (project-linked threads), D13 (notifications) |
| **APIs/events** | Conversation CRUD, `POST /conversations/{id}/messages`, read receipts. Events: `MessageSent`, `ConversationCreated`. Real-time transport `[Open OD-8]` |
| **Ownership boundary** | Transport of communication. A room's *collaboration state* (tasks, credits) is D7, not D6 |
| **Data ownership** | `Conversation`, `Message`, blocks |

## D7 — Collaboration (Creative Rooms & Projects)

| Aspect | Detail |
|--------|--------|
| **Purpose** | Structured multi-talent collaboration `[FRS §13][§14]` — the heart of the multi-skill model |
| **Responsibilities** | Creative Room lifecycle (title, description, project type, visibility); required skills definition; invite/accept members; **project contribution roles per member** (contextual, never overwriting profile `[BR-03][BR-04]`); shared assets (scripts, lyrics, media, docs); task list/status; final output and **contribution credits**; collaboration applications/eligibility `[FRS §3.2]` |
| **Major entities** | `CreativeRoom`, `Project`, `ProjectMember`, `ProjectContributionRole`, `RequiredSkill` `[Proposed]`, `RoomAsset`, `Task`, `ContributionCredit` |
| **Dependencies** | D1, D2 (skill claims inform eligibility), D3 (room assets), D6 (room discussion), D8 (project submissions), D9 |
| **APIs/events** | Room/project CRUD, membership, `POST /projects/{id}/members` with contribution role, applications. Events: `RoomCreated`, `MemberJoined`, `ContributionRoleAssigned`, `ProjectFinalized`, `CreditRecorded` |
| **Ownership boundary** | Owns who contributes *what* to a project. The competition *entry* of a project is a D9 Submission referencing the project |
| **Data ownership** | `CreativeRoom`, `Project`, `ProjectMember`, `ProjectContributionRole`, `Task`, `RoomAsset`, `ContributionCredit` |

`[BR-15]` Finalized project credits must record the actual contribution role and flow into portfolio projections (D2).

## D8 — Competition

| Aspect | Detail |
|--------|--------|
| **Purpose** | Competition lifecycle and configuration `[FRS §15]` |
| **Responsibilities** | Competition CRUD; multi-skill category association; participation modes (Individual/Team/Both); eligibility criteria; submission rules and deadlines; round definitions with per-round submission/voting/judging/qualification config; status machine Draft→Scheduled→Open→Voting→Judging→Results→Completed→Cancelled; scoring mode (audience/judge/combined) |
| **Major entities** | `Competition`, `CompetitionRound`, `CompetitionCategory`, `RoundConfig` `[Proposed]`, eligibility/rule config objects |
| **Dependencies** | D2 (skill taxonomy), D15 (admin), downstream D9–D12 consume its config |
| **APIs/events** | Admin competition/round CRUD; public competition views. Events: `CompetitionPublished`, `RoundOpened`, `RoundClosed`, `VotingOpened/Closed`, `JudgingOpened/Closed` |
| **Ownership boundary** | Owns competition *definition and schedule*. Entries are D9; scores are D12 |
| **Data ownership** | `Competition`, `CompetitionRound`, `CompetitionCategory`, round/voting/judging config |

## D9 — Submission

| Aspect | Detail |
|--------|--------|
| **Purpose** | Competition entries — individual and project `[FRS §16][§17]` |
| **Responsibilities** | Entry creation (user picks eligible skill/category; or project owner picks project); required media/metadata; eligibility, format, size, deadline validation; **immutable competition/round/category association**; moderation review before publication; contributor snapshot for project entries |
| **Major entities** | `Submission` (individual → `User`; team → `Project`), `SubmissionMedia`, `SubmissionContributor` snapshot `[Proposed]` |
| **Dependencies** | D8 (round/category eligibility), D2 (skills), D7 (project + contribution roles), D3 (media), D14 (pre-publish moderation) |
| **APIs/events** | `POST /submissions`, draft save, status transitions. Events: `SubmissionCreated`, `SubmissionApproved`, `SubmissionRejected` |
| **Ownership boundary** | Owns the entry record and its immutable associations. Vote/evaluation/score records attach *to* the submission but are owned by D10/D11/D12 |
| **Data ownership** | `Submission`, `SubmissionMedia`, `SubmissionContributor` |

`[FRS §38]` APIs must allow one user to submit in multiple eligible categories without duplicate identities; competition rules may restrict entries per user/category/round `[FRS §15.1]`.

## D10 — Voting

| Aspect | Detail |
|--------|--------|
| **Purpose** | Audience voting `[FRS §18]` |
| **Responsibilities** | Two distinct targets: individual-talent voting vs team/project voting — never conflated `[PD-04][BR-07]`; per-competition/round vote limits (per user/day/round/candidate); duplicate/abusive vote controls `[BR-14]`; live counts shown/hidden per config; auditable vote records; team votes attach to the project entry — **not auto-split among contributors** `[BR-08]` |
| **Major entities** | `Vote` (immutable record), `VotingConfig`, `VoteAggregate`, `VoteAnomalyFlag` `[Proposed]` |
| **Dependencies** | D8 (voting config per round), D9 (vote targets), D1 (voter identity/eligibility), D12 (feeds aggregation) |
| **APIs/events** | `POST /votes`, `GET vote counts` (visibility-gated). Events: `VoteRecorded`, `VoteRejected`, `VotingWindowClosed` |
| **Ownership boundary** | Owns raw vote records and counting rules. The *weighted score* combining votes + judging is D12 |
| **Data ownership** | `Vote`, `VoteAggregate`, `VotingConfig` (scoped to round) |

## D11 — Judging

| Aspect | Detail |
|--------|--------|
| **Purpose** | Judge management, assignment, configurable rubrics, evaluations `[FRS §19–21]` — see [JUDGE-RUBRIC-MODEL](JUDGE-RUBRIC-MODEL.md) |
| **Responsibilities** | Judge accounts/assignment to categories/competitions/rounds (independent of talent skills `[FRS §19]`); judges see only assigned submissions; evaluation template CRUD — category, criteria, order, weights (=100%), scoring scale (MVP 1–10), mandatory/optional comments; **immutable published template versions** `[BR-10]`; evaluation workflow: view → dynamic form → per-criterion score → weighted judge score → submit → lock `[BR-12]`; draft/save where allowed; authorized re-evaluation workflow |
| **Major entities** | `Judge` (reference to user + Judge system role), `JudgeAssignment`, `EvaluationTemplate`, `EvaluationTemplateVersion`, `EvaluationCriterion`, `JudgeEvaluation`, `EvaluationScore` (per criterion) |
| **Dependencies** | D1 (Judge system role), D8 (assignments scope), D9 (submissions), D11 rubric versions used by D12 |
| **APIs/events** | Assignment admin endpoints; `GET /judge/assignments`, `POST /evaluations` (draft/submit). Events: `JudgeAssigned`, `EvaluationSubmitted`, `TemplatePublished` |
| **Ownership boundary** | Owns evaluation *input*. Aggregation into competition scores is D12 |
| **Data ownership** | `JudgeAssignment`, template/version/criterion tables, `JudgeEvaluation`, `EvaluationScore` |

## D12 — Scoring & Ranking

| Aspect | Detail |
|--------|--------|
| **Purpose** | Aggregation, ranking, qualification, leaderboards `[FRS §22–24]` |
| **Responsibilities** | Configurable audience/judge weights (must total 100% `[BR-11]`); multi-judge aggregation rules; per competition/round/category ranking; qualification count/threshold config; tie-break rules (configurable, auditable); **manual overrides require authorization + audit** `[BR-13]`; leaderboards: talent, project, category, round, overall — visibility configurable `[FRS §24]`; round progression eligibility `[FRS §23]` |
| **Major entities** | `Score`, `ScoringConfig`, `Ranking`, `QualificationDecision`, `OverrideRecord` `[Proposed]`, `LeaderboardView` (projection) |
| **Dependencies** | D8 (round config), D10 (vote aggregates), D11 (judge scores), D15 (audit on overrides) |
| **APIs/events** | Results/leaderboard queries; admin override endpoints. Events: `ScoresComputed`, `RankingPublished`, `QualificationDecided`, `OverrideApplied` |
| **Ownership boundary** | The only domain that combines vote + judge inputs into official results. Vote/evaluation raw data stays in D10/D11 |
| **Data ownership** | `Score`, `Ranking`, `QualificationDecision`, `OverrideRecord`, leaderboard projections |

## D13 — Notification

| Aspect | Detail |
|--------|--------|
| **Purpose** | All user notifications `[FRS §25]` — detail in [NOTIFICATION-ARCHITECTURE](NOTIFICATION-ARCHITECTURE.md) |
| **Responsibilities** | Event families: account, submission status, competition open/close, round progression, voting windows, judge assignment/pending evals, results/qualification, messages/room activity, project invitations, announcements; channel rendering (in-app, push, email, SMS); per-user notification controls `[FRS §12]` |
| **Major entities** | `Notification`, `NotificationTemplate`, `NotificationPreference`, `DeliveryAttempt` `[Proposed]` |
| **Dependencies** | Subscribes to events from all domains; providers `[OD-7]` |
| **APIs/events** | `GET /notifications`, mark-read, preference CRUD. Consumes domain events; emits `NotificationDelivered/Failed` |
| **Ownership boundary** | Owns rendering/delivery state; never originates business facts |
| **Data ownership** | `Notification`, templates, preferences, delivery logs |

## D14 — Moderation

| Aspect | Detail |
|--------|--------|
| **Purpose** | Safety and content governance `[FRS §26]` |
| **Responsibilities** | User reports on content/profiles/messages; admin review queues; actions: hide/reject/remove content, suspend/block users; moderation audit trail; community guideline acknowledgement tracking; submission pre-publish review `[FRS §16]` |
| **Major entities** | `ModerationCase`, `Report`, `ModerationAction`, `ContentRestriction` `[Proposed]` |
| **Dependencies** | References entities from D1/D2/D3/D6/D9 — by ID only; actions executed via owning domain's API or status transition |
| **APIs/events** | `POST /reports`, admin case queue, action endpoints. Events: `ReportFiled`, `ContentHidden`, `UserSuspended` (with D1) |
| **Ownership boundary** | Owns cases/decisions. Effecting a status change calls the owning domain (e.g., media hidden via D3 transition) — keeps lifecycle rules in one place |
| **Data ownership** | `Report`, `ModerationCase`, `ModerationAction` |

## D15 — Administration & Audit

| Aspect | Detail |
|--------|--------|
| **Purpose** | Admin portal backend, platform configuration, audit, reporting `[FRS §27][§29][§30]` |
| **Responsibilities** | Admin dashboard/KPIs; user management; system-role management; skill taxonomy admin (delegates D2); competition ops; judge management; rubric admin; moderation console; report/analytics queries; notification management; platform configuration; **audit log access** `[FRS §30]` |
| **Major entities** | `AuditLog` (append-only), `PlatformConfig` `[Proposed]`, `KpiSnapshot` `[Proposed]` |
| **Dependencies** | Facade over all domains' admin APIs |
| **APIs/events** | `/admin/*` surface; audit query API; config endpoints |
| **Ownership boundary** | Admin UI/API orchestration + audit records + platform config. Business entities remain owned by their domains — D15 does not reach into their tables |
| **Data ownership** | `AuditLog`, `PlatformConfig`, analytics projections |

Reporting & analytics `[FRS §29]`: registered users, active creators, skill popularity, uploads/engagement, competition participation, voting activity, judge completion, per-criterion averages, round progression, collaboration stats, top-talent metrics. `[Open OD-10]` in-app queries vs dedicated analytics store.

---

## Cross-Domain Invariants

| Rule | Enforced by | Source |
|------|-------------|--------|
| AuthZ reads only `UserSystemRole` | API middleware + D1 | `[BR-02][FRS §38]` |
| Skill removal never deletes portfolio/work | D2 | `[UAT-03][FRS §3.2]` |
| Submission competition/round association immutable | D9 write path + DB | `[FRS §16]` |
| Published rubric versions immutable | D11 write path + DB | `[BR-10]` |
| Votes immutable & auditable | D10 + D15 | `[FRS §18][§30]` |
| Team votes not split across contributors | D10/D12 | `[BR-08]` |
| Weights total 100% (rubrics; scoring mix) | D11/D12 validation | `[BR-11][FRS §20]` |
| Judge independence until close | D11 | `[BR-12]` |
| Overrides authorized + audited | D12 + D15 | `[BR-13]` |
| Contribution roles contextual, never overwrite profile | D7 | `[BR-04]` |
