# Module Design — 11: Audience Voting

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 11)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–10 without modification. Judge-side mechanics are contract-referenced only.

## 1. Purpose

Own the **audience voting mechanism** — cast, dedupe, constrain, and audit votes against explicit competition entries. Downstream modules (M14 scoring, M16 leaderboards) consume the authoritative vote records; they own what happens *to* them.

## 2. FRS Scope `[FRS §18]` — traced

Audience voting · separate individual + team/project voting · **configurable limits** · duplicate/abuse controls · auditable · votes attach to explicit target · **project/team votes are NOT split among contributors**.

## 3. Individual vs Project/Team Target — critical rule

```text
INDIVIDUAL:  Vote → Individual Submission
PROJECT/TEAM: Vote → Project/Team Submission (ONE vote, attached to the entry)
```

**Never:** split votes across contributors, per-member votes, credit redistribution, or talent-vote conversion. This rule applies across boundary/data-model/API/invariants/downstream/testing/acceptance.

## 4. Module Boundary

| Owns | Does NOT own |
|------|--------------|
| `Vote`, `VoteConfig` (voting-behavior config — M11-owned; M09 holds only a structural reference), derived count projections (rebuildable), abuse/audit references | Competition/Category/Round/Participant (M09), Submission (M10), Media (M04), Project membership (M07), JudgeEvaluation/Rubric/Score/Ranking/Progression/Leaderboard (M12–16), moderation policy, notification delivery |

## 5. Voting Configuration — separate from Vote record

`VoteConfig` — **M11-owned** (voting *behavior*): voter eligibility, limits, duplicate rules, abuse toggles, target type, window *enforcement details*. **No fixed model invented** — "1 vote/day/competition/submission" etc. are all *configurable options*, never assumptions. Config ≠ Vote. Boundary (CM-05): **M09 owns voting *structure*** — whether a round permits voting and which `VoteConfig` applies (a reference, not the config itself); **M11 owns the `VoteConfig` entity** — how votes behave.

## 6. Voter Eligibility

Authenticated user + competition/round/submission eligibility + window + configured dimensions. **Never:** TalentSkill, ProjectContributionRole, project-membership, SystemRole as automatic voting authority. **Judge-as-audience-voter:** logically independent + auditable — allowed only if product explicitly permits *(open)*.

## 7. Duplicate / Abuse Controls

Configurable framework: duplicate prevention, configured limits, repeated-request handling, rate-limiting boundary, suspicious-activity hooks + audit. **Not invented:** CAPTCHA, device fingerprinting, IP blocking, fraud ML, Redis, Kafka — all future implementation/product decisions.

## 8. Idempotency

PostgreSQL constraints + `Idempotency-Key` + transactions + deterministic dedup — client retry/timeout/refresh safe. **Database is authoritative; in-memory is never authority.**

## 9. Vote Immutability / Reversal

Policy **OPEN** — FRS silent. If reversal supported: reversal is an auditable state transition, never silent deletion; audit trail intact regardless.

## 10. Voting Window

Competition/round-scoped; **server-authoritative timestamps**; explicit boundary evaluation; closed-window rejection; no invented grace periods; round-specific window preserved.

## 11. Vote Data Model

`Vote`: id, voterId, competitionId, categoryId, roundId, submissionId, targetType *(individual|project)*, createdAt, idempotencyKey, audit/moderation refs. **For project votes, `submissionId` points to the project entry — no contributor-allocation collection.**

## 12. Validation

Vote validated against: submission exists + belongs to competition/category/round + eligible state + voting config + open window + target-type match. Inconsistent combos rejected; no config duplication.

## 13. Security / Authorization

Authenticated voter; eligibility rules; IDOR; tampering; enumeration; replay (idempotency); abuse/rate-limit *(thresholds open)*; audit. **Skills/roles/membership never grant voting permission.**

## 14. Privacy

Vote record (internal) ≠ public count ≠ voter identity ≠ audit. Public voter list **not invented**; identity-visibility = open product policy.

## 15. Vote Counting

`Vote` records = authoritative. Counts/read models = **derived, rebuildable from Vote rows**. Cached counts, client counters, UI totals **never** official results. M14 scoring formulas are out of scope here.

## 16. Downstream Scoring Contract (→ M14)

Supplies: valid vote count, voting context, competition/category/round, submission, target type, aggregation state. **Project/team aggregation belongs to the entry — never redistributed.** How audience-votes combine with judge-scores = M14's formula — not designed here.

## 17. Judge Separation

No judge votes/scores/rubrics in M11. Judge-as-audience-voter only if product permits; independently auditable.

## 18. Moderation / Abuse

Signals → Moderation (suspicious voting, reported submission, abusive voter). Moderation may invalidate votes *(authority + audit)* — but **never silently deletes**; audit preserved; invalidation policy = open.

## 19. Notifications

Optional signals (`VotingOpened`, `VotingClosed`, `ResultAvailable`) — delivery owned by Notifications; no provider logic.

## 20. Data Ownership

**Owns:** `Vote` (authoritative), voting-specific config where not M09-owned, derived count projections (rebuildable), abuse/audit refs. **References:** Competition/Category/Round (M09), Submission (M10), Project context (M07 — context only). **Never duplicated.**

## 21. API Surface (conceptual — ADR-007)

`POST /api/v1/votes` (cast — idempotent) · `GET /api/v1/votes/me` (own votes — if allowed) · `GET /api/v1/votes/eligibility?competition=…` · `GET /api/v1/competitions/{id}/votes/counts` (authz-gated) · `GET /api/v1/votes/config` · Errors: `VOTE_DUPLICATE`, `VOTE_LIMIT_EXCEEDED`, `VOTING_CLOSED`, `VOTE_FORBIDDEN`, `VOTE_TARGET_INVALID` + RFC 9457.

## 22. Concurrency / Consistency

Concurrent votes → unique constraint + tx; duplicate retry → idempotent; multi-device → same dedup; window-boundary race → server timestamp; count updates → derived-refresh not authoritative; moderation changes → state transition. No distributed locks/cache authority.

## 23. Observability / Audit

Audited: cast, rejected, duplicate, limit-exceeded, window-violation, suspicious signal, authorized invalidation/reversal — all with vote/voter/submission/competition/category/round/correlation IDs. `AuditLog` separate from telemetry.

## 24. Performance / Scalability

Vote inserts + dedup via constraints; counts via derived models; round aggregation; public reads. **No Redis/Kafka/RabbitMQ/ES/external-voting-service** — PG authoritative; triggers only (high-volume counter contention → future read-model/replica decision).

## 25. Testing (not implemented)

Individual/project vote, target correctness, limits, duplicates, retry/idempotency, window-boundary, invalid combos, unauthorized voter, IDOR, replay, concurrent votes, **project-vote-not-split**, derived counts, moderation, audit, privacy.

## 26. Open Decisions

Reversal policy, vote visibility, public voter identity, limit dimensions, window ownership, anonymous-vs-authenticated, abuse policy, IP/device controls, suspicious handling, invalidation authority, count freshness SLA, retention, judge-as-voter, result timing.

## 27. Acceptance Validation

Vote=authoritative audience action ✓ · individual/project targets explicit ✓ · **project votes never split** ✓ · M09 config authoritative ✓ · M10 entries authoritative ✓ · limits configurable ✓ · dedup/abuse supported ✓ · PG authoritative ✓ · counts derived ✓ · judge-separate ✓ · skills/roles never authorize ✓ · audited ✓ · no infra ✓ · no impl ✓

## 28. Traceability

- **FRS:** §18 audience voting · §16 submissions · §30 audit · BR-2
- **ADRs:** ADR-001 boundary · ADR-003 PG · ADR-006 auth · ADR-007 API/idempotency · ADR-010 no-cache-authority
- **Modules:** M09 config · M10 submission · M07 project context · M14/16 consumers · Moderation · Notifications
