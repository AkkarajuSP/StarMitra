# Module Design — 10: Submissions

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 10)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–09 without modification. Downstream mechanics (voting/judging/scoring/ranking/progression/leaderboards) are contract-referenced, not designed.

## 1. Purpose

Own the **actual competition entry** — the evidence submitted by an eligible participant. Competition defines the rules (MODULE 09); Submission is the entry itself.

## 2. Core Model — distinct concepts

```text
Competition (M09) → CompetitionCategory → CompetitionRound → CompetitionParticipant
                                                                │
                                                    Submission (this module) ──► SubmissionMedia → MediaAsset (M04)
                                                                │
                                          (later) Vote | JudgeEvaluation | Score | Ranking | Qualification
```

## 3. Module Boundary

| Owns | Does NOT own |
|------|--------------|
| `Submission`, `SubmissionMedia` (ref), `SubmissionContributor` (ref/snapshot), submission metadata, submission state/lifecycle | Competition/Category/Round/Participant (M09), TalentSkill (M03), Project/Member/ContributionRole (M07), MediaAsset (M04), Vote/Judge/Rubric/Score/Progression/Leaderboard (M11–16) |

## 4. Individual vs Team/Project — critical

| Mode | Shape |
|------|-------|
| **Individual** | `User → CompetitionParticipant → Submission` |
| **Team/Project** | `Project context → CompetitionParticipant → ONE Submission` |

**A team/project submission is a single competition entry** — never split into per-contributor submissions. FRS: votes attach to the entry, not divided among contributors `[FRS §18]`.

## 5. Contributors

`SubmissionContributor`: submissionId + memberRef + contributionRoleRef + *(optional)* snapshot fields. **Authoritative membership/contribution lives in M07** — Submission references it; an optional **submission-time snapshot** preserves evidence *(snapshot strategy = OPEN decision)*. Never duplicated as source of truth; never inferred from TalentSkill; no global role taxonomy.

## 6. Competition Context Validation

Every submission validates referential consistency: competition/category/round belong together; participant is of the same competition; participant type matches category mode; round is active/open for the submission window. No config duplication — reads M09.

## 7. Submission Rules (consumed, not redefined)

`SubmissionConfig` (M09) is authoritative — this module validates: allowed type, required evidence, required metadata, deadline, participation mode, category/round restrictions, attempt/count constraints. Rules never redefined here.

## 8. Deadlines

Server/database establishes the **authoritative timestamp** — client time never trusted. Deadline levels (competition/category/round) evaluated per config; **precedence order = OPEN** (FRS silent); boundary evaluation explicit (`<=`); no invented grace periods.

## 9. Submission Lifecycle — own state machine

`Draft → Submitted → UnderReview → Accepted | Rejected → Finalized; Withdrawn` *(state names = proposal — FRS behavior drives, not names)*. **Separate from** competition/round/voting/judging/ranking/qualification states — six independent dimensions.

## 10. Finalization & Immutability — critical

At finalization the submission becomes **immutable competition evidence**: media refs frozen, contributor info frozen, state auditable. Replacement **after** finalization = explicit new submission/version, never silent overwrite. Pre-finalization replacement allowed *(if product enables — open)*. Retention periods = open, not invented.

## 11. Media Integration (M04/ADR-008)

`SubmissionMedia` = ref link (submissionId + mediaId + role). Upload/process/access via MODULE 04 — direct upload, async processing, signed access after authz. Evidence media locked at finalization; no duplicated processing; no storage provider chosen.

## 12. Individual Semantics

Identify participant + submitting user + category/round + evidence + skill context *(submission's skill context ≠ user's full profile — it's the declared competition context)*. TalentSkill never authorizes.

## 13. Team/Project Semantics

Identify participant + project/team context + **authoritative M07 membership/contribution** + submitting actor + contributor context. One entry; contributors referenced, not split.

## 14. Duplicates / Multiple Submissions

Enforced transactionally: unique constraint on (participant + category + round + attempt) per configured constraints; concurrent submits race → constraint wins; `SubmissionConfig` defines attempt limits *(open)*; concurrent attempts resolved by constraint, never app logic alone.

## 15. Participant Snapshot — evaluation

Evidence may require submission-time snapshot (participant identity, category, round, project, contributor list, contribution roles) — **live references vs immutable snapshot distinguished explicitly; strategy = OPEN** pending evidence-integrity review.

## 16. API Surface (conceptual — ADR-007)

`POST /api/v1/submissions` (draft) · `GET/PUT /{id}` · `PUT /{id}/media` (attach ref) · `POST /{id}/submit` (finalize) · `POST /{id}/withdraw` · `GET /{id}/history` · `GET /api/v1/participants/{id}/submissions` · `GET /api/v1/competitions/{id}/submissions` (authz-gated listing). Explicit state transitions; RFC 9457; idempotent submit.

## 17. Idempotency / Concurrency

Duplicate submit → idempotent (same submissionId); timeout retry → idempotency-key dedup; concurrent attempts → constraint; simultaneous finalize → state-transition guard; duplicate media attach → unique constraint; deadline race → server timestamp decides. No distributed locks/caches.

## 18. Authorization / Security

| Action | Authz |
|--------|-------|
| Create draft | eligible participant |
| Edit draft | participant/submitting member |
| Finalize | participant + before deadline + valid state |
| Withdraw | participant + allowed state *(open)* |
| View | public-gated / judge-assigned / admin / participant |
| Admin | system-role-gated |

Protected: IDOR, tampering, impersonation, membership-spoofing, deadline-bypass, media leakage, duplicate submission, unauthorized finalization, cross-competition access. **Skills/roles never authorize submission ops; participation ≠ blanket submission rights.**

## 19. Downstream Contracts

| Module | Consumes from Submission |
|--------|--------------------------|
| 11 Voting | eligible submission ID, competition/category/round, individual-vs-project target, evidence |
| 12 Judge Mgmt | submission scope/context |
| 13 Rubrics | category/round context |
| 14 Scoring | finalized eligible submission, judging/voting inputs |
| 15 Progression | submission status/result |
| 16 Leaderboards | participant/project identity + result refs |

## 20. Moderation

Evidence/content moderated via Moderation (owns policy); **submission-validity ≠ content-moderation-state** — kept separate; moderation restriction never silently alters official competition results.

## 21. Notifications

Signals: `DraftCreated`, `SubmissionReceived`, `SubmissionFinalized`, `SubmissionRejected`, `StatusChanged`, `DeadlineReminder` — delivery owned by Notifications.

## 22. Observability / Audit

Submission/participant/competition/category/round/correlation/actor IDs on all events; state-transition + finalization + evidence-change audit; `AuditLog` separate; result-affecting ops auditable.

## 23. Data Model

| Entity | Purpose | Notes |
|--------|---------|-------|
| `Submission` | entry | competitionId, categoryId, roundId, participantId, type, state, submittedAt, finalizedAt |
| `SubmissionMedia` | evidence link | submissionId+mediaId+role; frozen at finalize |
| `SubmissionContributor` | contributor context | submissionId+memberRef+roleRef(+snapshot) |
| `SubmissionHistory` | state trail | transitions, actor, timestamp |

## 24. Testing (not implemented)

Individual/team entry, required-media validation, rules enforcement, deadline-boundary, draft lifecycle, finalize immutability, duplicate/concurrent submit, idempotent retry, contributor consistency, M07 validation, authz matrix, IDOR/impersonation, moderation restriction, downstream contract correctness.

## 25. Open Decisions

Status taxonomy, draft requirement, attempts, replacement/resubmission, withdrawal, deadline precedence, timezone, contributor/role snapshots, post-finalization replacement, moderation timing, evidence retention, visibility, membership-change handling, participant replacement.

## 26. Acceptance Validation

Submission=M10-owned ✓ · M09 rules authoritative ✓ · M04 media ✓ · M07 membership ✓ · individual/team distinct + team=one-entry ✓ · contributors preserved ✓ · skills/roles never authorize ✓ · finalized evidence protected ✓ · downstream contracts clear ✓ · PG authoritative ✓ · no infra ✓ · no impl ✓

## 27. Traceability

- **FRS:** §16 submissions · §18 voting attachment rule · BR-2
- **ADRs:** ADR-003 PG · ADR-007 API · ADR-008 evidence-media
- **Modules:** M09 config (authoritative) · M07 membership · M04 media · M01 authz · Moderation · Notifications
