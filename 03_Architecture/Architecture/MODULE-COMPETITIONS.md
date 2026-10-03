# Module Design — 09: Competitions

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 09)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–08 without modification. Downstream competition mechanics (Submissions/Voting/Judging/Scoring/Ranking/Progression/Leaderboards) are contract-referenced, **not** designed here.

## 1. Purpose

Own the **competition definition + lifecycle**: structure, categories, eligibility rules, participation configuration, submission rules, deadlines, and round *configuration* `[FRS §14–§16]`. The "what a competition is" module — not the "how it's judged/scored" modules.

## 2. Core Model — distinct concepts

```text
Competition (definition + lifecycle)
 ├── CompetitionCategory (competition-scoped grouping)
 ├── CompetitionRound   (sequence + config references)
 ├── EligibilityRule    (who may enter)
 ├── ParticipationMode  (individual | team/project | both)
 └── SubmissionConfig   (what/how entries are submitted)
        ↑ consumed by MODULE 10 Submissions
```

`Competition` ≠ `CompetitionCategory` ≠ `CompetitionRound` ≠ `Submission` — Submission is MODULE 10.

## 3. FRS Scope `[FRS §14–§16]` — traced

Multi-skill competitions · categories · individual + team/project participation · eligibility · submission rules + deadlines · multiple rounds · per-round voting/judging/qualification · competition statuses.

## 4. Module Boundary

| Owns | Does NOT own |
|------|--------------|
| `Competition`, `CompetitionCategory`, `CompetitionRound`, `EligibilityRule`, `SubmissionConfig`, `CompetitionParticipant` | User, TalentSkill (M03), Project/Member (M07), **Submission, Vote, JudgeEvaluation, Score, Ranking, Leaderboard** (future modules), media (M04), moderation policy |

## 5. Competition Lifecycle

Configuration state vs participation state vs round state — **separate dimensions**, not one machine:

| Dimension | States *(proposal/open)* |
|-----------|--------------------------|
| Configuration | `Draft → Configured → Published → (Paused/Cancelled)` |
| Participation | `Closed → Open → RegistrationClosed → InProgress → Ended` |
| Round | per-round state (see §11) |

Draft is never publicly visible until published.

## 6. Competition Categories

`CompetitionCategory`: name, description, associated skills (refs→M03), category-scoped eligibility/participation/submission overrides. **Multiple categories per competition; no hard-coded names; no duplicate skill taxonomy.**

## 7. Multi-Skill Competitions

Category/Competition references `TalentSkill` IDs for eligibility/discovery — **capability ≠ authorization ≠ entry**. Eligibility is rule-evaluation, never automatic-permission; no auto-matching/recommendation invented.

## 8. Participation Mode

`Individual | Team/Project | Both` — declared per competition/category. **Team ≠ Creative Room unless the competition's rules link them** — relationship defined per competition (OPEN where FRS is silent); never duplicate project membership.

## 9. Eligibility

`EligibilityRule`: configurable dimensions (skill, category, user/system criteria, participation mode). **Eligibility is a business rule, not authorization** — failure affects only that competition entry, not platform permissions. Dimensions beyond FRS (age/location/followers) = **OPEN, not invented**.

## 10. Submission Rules (config only)

`SubmissionConfig`: allowed types, required media/evidence, deadline, category, attempt/count *(open)*, team/individual constraints, required metadata. **Submission entity itself = MODULE 10** — this is the *configuration contract* it consumes.

## 11. Deadlines & Time

Explicit timestamps at competition/category/round level *(configurability open)*; timezone handling + boundary-condition evaluation explicit (e.g., `<= deadline`); no invented defaults/grace periods — OPEN.

## 12. Rounds

`Competition → Round 1..N`: name, sequence, start/end, scope, submission-config ref, voting/judging-mode ref, qualification config ref. **Rounds are configuration containers** — the mechanisms that *act on* them (voting/judging/scoring/progression) are future modules consuming these refs.

## 13. Round-State Boundary — six dimensions, never collapsed

`CompetitionState` ≠ `RoundState` ≠ `SubmissionState` ≠ `VotingState` ≠ `JudgingState` ≠ `Ranking/QualificationState` — each its own owner/machine.

## 14. Participant / Registration Model

`CompetitionParticipant`: userId OR projectId (polymorphic by participation mode), category, registration status, eligibility-result, timestamps. **No duplicate project membership** — team participation references MODULE 07 project + members by contract. Cardinality (once-only, team size) = **OPEN**.

## 15. Team / Project Participation

Team competitions reference a Creative Room project where appropriate; **project membership remains M07-authoritative**; contribution roles preserved; a member's `TalentSkill` never determines participation role — explicit participant context only. Competition participation ≠ project collaboration.

## 16. Visibility / Discovery (MODULE 05)

Competition owns visibility + lifecycle + public config; Discovery reads *published* competitions only — drafts never leak. Discovery never authoritative for competition state.

## 17. Moderation

Competition definition/content/category/described-media moderation consumed; policy owned by Moderation — no invented workflow.

## 18. Notifications

Emits signals: `CompetitionPublished`, `RegistrationConfirmed`, `RoundOpened/Closed`, `DeadlineReminder`, `QualificationResult` — Notifications owns delivery; provider/preferences not chosen.

## 19. Admin Management Boundary

Domain operations an authorized admin needs: create, configure, edit, publish, pause/cancel*(if supported — open)*, manage categories/rounds/eligibility/submission-rules. **Admin Portal is a separate module** — these are the domain operations it will call. System-role authorization explicit — `TalentSkill`/`ProjectContributionRole` never grant competition administration.

## 20. Data Model (conceptual)

| Entity | Purpose | Invariants |
|--------|---------|-----------|
| `Competition` | definition+lifecycle | title, desc, status, visibility, timestamps |
| `CompetitionCategory` | grouping | competitionId; category-scoped config |
| `CompetitionCategorySkill` | skill assoc | categoryId+skillId (→M03) |
| `CompetitionRound` | round config | competitionId, sequence, timing, config refs |
| `EligibilityRule` | entry rules | competitionId/categoryId, type, params |
| `SubmissionConfig` | submission rules | competitionId/categoryId/roundId, type/deadline/limits |
| `CompetitionParticipant` | participant | competitionId+categoryId+participantRef unique; eligibility result |

Authoritative; all PG.

## 21. API Surface (conceptual — ADR-007)

`POST/GET/PUT /api/v1/competitions` · `/{id}/categories` · `/{id}/rounds` · `/{id}/eligibility` · `/{id}/submission-config` · `/{id}/publish` · `POST /{id}/participants` (register) · `GET /{id}/participants` · public `GET /competitions` discovery surface. Explicit state-transition endpoints; DTOs; RFC 9457; cursor pagination.

## 22. Concurrency / Consistency

Duplicate-participation → unique constraint; registration race → tx + constraint; admin concurrent edits → optimistic locking; deadline evaluation = explicit comparison at submit-time; publish/cancel = explicit state transitions; eligibility re-check on category/config change. PG-only.

## 23. Security

Unauthorized admin ops (system-role-gated); draft-competition enumeration (uniform errors); eligibility bypass (server-side evaluation); participant/category tampering; project-membership spoofing (M07-validated); IDOR; deadline manipulation (server-side timestamps, never client-supplied). **Skills/roles never grant competition authority.**

## 24. Future-Module Contracts

| Module | Competition provides | Consumes |
|--------|---------------------|----------|
| **10 Submissions** | SubmissionConfig, deadlines, participant context | submission lifecycle, media evidence |
| **11 Voting** | Round voting-structure config (whether voting applies + `VoteConfig` reference); M11 owns `VoteConfig` (CM-05) | vote collection/counters |
| **12 Judge Mgmt** | Judge assignment scope (round/category) | assignments |
| **13 Rubrics** | Rubric config reference per round/category | evaluation criteria |
| **14 Scoring/Ranking** | Scoring config reference | scoring outputs |
| **15 Progression** | Qualification config per round | advancement decisions |
| **16 Leaderboards** | Competition/round context | leaderboard projections |

## 25. Open Decisions (Product Owner)

Status taxonomy, lifecycle states, category taxonomy, participant cardinality, registration-vs-direct-entry, team size/eligibility, eligibility dimensions, attempt limits, deadline rules/timezone/grace, round timing, pause/cancel semantics, ownership transfer, visibility options, moderation requirements, withdrawal/re-entry/replacement, notification rules.

## 26. Acceptance Validation

Competition=definition owner ✓ · categories configurable ✓ · multi-skill refs ✓ · individual/team distinct ✓ · no duplicated room membership ✓ · eligibility≠authz ✓ · Submission/Vote/Judge/Rubric/Score/Progression/Leaderboard all deferred to their modules ✓ · 6-state separation ✓ · PG authoritative ✓ · no infra ✓ · no impl ✓

## 27. Traceability

- **FRS:** §14 competition structure · §15 categories/rounds · §16 submissions/deadlines · §17 participation · BR-2
- **ADRs:** ADR-001 boundary · ADR-003 PG · ADR-007 API · ADR-008 evidence-media
- **Modules:** M01 authz · M03 skill refs · M07 project contract · M05 discovery · Moderation · Notifications
