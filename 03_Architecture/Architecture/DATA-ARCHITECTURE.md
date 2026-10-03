# StarMitra — Data Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Draft for review

## 1. Approach

`[Accepted]` Single relational database as system of record for all domain data (see SYSTEM-ARCHITECTURE §2). PostgreSQL — inside the OD-02 accepted backend stack; OD-03 detailed review completed with recommendation to accept (formal decision pending). Selection rationale: transactional integrity for scoring/voting `[FRS §36]`, relational fit for the FRS §31 model, JSONB for configuration-driven entities (rubrics, round config), mature replication/backup.

Media binaries are never stored in the database — object storage with metadata rows `[FRS §10][MEDIA-ARCHITECTURE]`.

## 2. Logical Model — Core Spine

Extends FRS §31 recommended entities. Status: `[FRS §31]` for listed entities; specific attributes are `[Proposed]`.

```text
USER & IDENTITY (D1)
  User ──────────────── core account identity, status [Active/Suspended/Blocked/Deactivated]
  SystemRole ────────── authorization catalog [User/Talent, Judge, Admin, SuperAdmin]
  UserSystemRole ────── user ──< many >── system roles   (authz source of truth)

TALENT PROFILE (D2)
  TalentSkill ───────── configurable taxonomy [name*, description, parent, active,
                       displayOrder, competitionEligible, judgeRubricEligible]
  UserTalentSkill ───── user ──< many >── skills
  SkillProficiency ──── optional level per user-skill [Beginner/Intermediate/Advanced/Professional]
  PortfolioItem ─────── work linked to one/more skills; survives skill removal [UAT-03]
  PortfolioCredit ───── recorded contribution credit from finalized projects [BR-15]

MEDIA (D3)
  MediaAsset ────────── ownership, metadata, visibility, status lifecycle
  MediaVariant ──────── processed renditions/thumbnails

SOCIAL (D4) / DISCOVERY (D5)
  Follow, Like, Comment, EngagementCounter
  FeedEntry / SearchDocument / TrendingSnapshot  (derived read models only)

MESSAGING (D6)
  Conversation ──< ConversationMember
  Message ──────── sender, timestamps, delivery/read state
  MessageAttachment → MediaAsset

COLLABORATION (D7)
  CreativeRoom ─── workspace def, visibility, required skills
  Project ──────── creative project backing a room/submission
  ProjectMember ── user membership in project
  ProjectContributionRole ── contextual capacity per member per project [BR-03/04]
  Task, RoomAsset, ContributionCredit

COMPETITION (D8)
  Competition ──── master: modes, eligibility, rules, status machine
  CompetitionCategory ── skill/category eligibility per competition
  CompetitionRound ─── sequence + per-round config:
                        submission rules/deadline, voting mode, judging config,
                        scoring weights, qualification rule

SUBMISSION (D9)
  Submission ───── IMMUTABLE link: competition + round + category;
                   owner = User (individual) | Project (team)
  SubmissionMedia, SubmissionContributor (snapshot)

VOTING (D10)
  Vote ─────────── immutable, auditable; target = submission; voter + limits context
  VotingConfig ─── per-round: mode [individual/project/both], limits, visibility
  VoteAggregate ── counts for scoring input

JUDGING (D11)
  Judge ─────────── authorization reference (user + Judge system role)
  JudgeAssignment ── judge ↔ competition/round/submissions
  EvaluationTemplate ───── name, category, status
  EvaluationTemplateVersion ── IMMUTABLE published definition
  EvaluationCriterion ──────── per version: name, order, weight (Σ=100%),
                                scale, comment required flag
  JudgeEvaluation ── evaluation by judge on submission against a version;
                     locked on submit [BR-12]
  EvaluationScore ── per-criterion score within an evaluation

SCORING & RANKING (D12)
  ScoringConfig ── audience/judge weights (Σ=100% [BR-11]), aggregation rules
  Score ────────── computed per submission/round
  Ranking ──────── per competition/round/category
  QualificationDecision ── advance/eliminate, incl. authorized overrides [BR-13]
  OverrideRecord ── who/what/why/when

SUPPORTING (D13–D15)
  Notification, NotificationTemplate, NotificationPreference, DeliveryAttempt
  Report, ModerationCase, ModerationAction
  AuditLog ──────── append-only [FRS §30]
  PlatformConfig ── global admin configuration
```

## 3. Key Relationships

`[FRS §31.1]` — binding:

```text
User            ──< UserTalentSkill >── TalentSkill            (M:N)
User            ──< UserSystemRole  >── SystemRole             (M:N)
Project         ──< ProjectMember   >── User                   (M:N via member)
ProjectMember   ──< ProjectContributionRole                    (contextual)
Competition     ──< CompetitionCategory >── TalentSkill        (M:N)
Submission      ──► User (individual) | Project (team)
Submission      ──► CompetitionRound  (immutable [FRS §16])
JudgeAssignment ──► Judge + Submission/Round
JudgeEvaluation ──► EvaluationTemplateVersion + Submission + Judge
Vote            ──► Submission (per configured voting mode [BR-07])
```

## 4. Data Rules

| Rule | Mechanism `[Proposed]` | Source |
|------|------------------------|--------|
| Rubric version immutability | Published versions reject UPDATE/DELETE; changes create new version | `[BR-10]` |
| Submission association immutability | FK columns set once; no update path | `[FRS §16]` |
| Vote/evaluation append-only | No update/delete paths post-submit; corrections via authorized re-evaluation workflow | `[FRS §18][§21]` |
| Content soft-lifecycle | Status column transitions, hard delete only by policy | `[FRS §10]` |
| AuditLog append-only | Insert-only grants/patterns | `[FRS §30]` |
| Σ weights = 100% | Check constraints + domain validation | `[BR-11][FRS §20]` |
| Team vote attaches to entry, not members | Vote targets Submission only | `[BR-08]` |
| Skill removal preserves portfolio | UserTalentSkill deletion decoupled from PortfolioItem | `[UAT-03]` |

## 5. Consistency & Integrity

`[Proposed]`

- Foreign keys and unique constraints enforced at DB level for all system-of-record relationships; uniqueness on `TalentSkill.name` `[FRS §7]`, one vote per voter per target per configured limit `[FRS §18]`.
- Score computation writes `Score` + `Ranking` + `QualificationDecision` + `AuditLog` in one transaction where possible `[FRS §36]`.
- Derived stores (feed, search, trending, leaderboard views, analytics projections) are rebuildable from system-of-record data + events.
- Optimistic concurrency or explicit locking on high-contention counters (vote aggregates) `[Open implementation detail]`.

## 6. Retention & Privacy

`[FRS §9][§10][§26][§36]`

- Privacy controls per profile field and per content item; visibility enum enforced at read path.
- Eliminated entries remain historically visible per configured privacy rules `[FRS §23]`.
- `[Open]` Data-retention/deletion policy (e.g., account deactivation vs erasure, message retention) — needs legal/product input.

## 7. Migration & Governance

- Schema evolves via versioned migrations in `06_Database/Migrations/` — **none created in this phase** per phase scope.
- Data dictionary maintained in `06_Database/Data-Dictionary/` during schema design phase.
- Rubric/config schema changes are data migrations, not code releases `[AP-4]`.
