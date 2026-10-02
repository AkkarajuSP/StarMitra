# StarMitra — System Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Draft for review

## 1. Topology

`[Proposed]` — pending ADR-TBD-1.

```text
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│ Mobile   │  │ Public   │  │ Admin    │  │ Judge    │
│ App      │  │ Web      │  │ Web      │  │ Web      │
└────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
     └─────────────┴──────┬──────┴─────────────┘
                          │ HTTPS /api/v1 (REST+JSON)
                          │ (+ real-time channel — Open OD-8)
                   ┌──────▼───────────────┐
                   │   API / Edge layer   │  authN, authZ middleware,
                   │                      │  rate limiting, validation
                   └──────┬───────────────┘
                   ┌──────▼───────────────┐
                   │  Modular Monolith    │
                   │  ┌─────────────────┐ │
                   │  │ D1 Identity     │ │
                   │  │ D2 Profile      │ │   Each module owns:
                   │  │ D3 Media        │ │   API endpoints +
                   │  │ ... D15         │ │   business logic +
                   │  │ (see DOMAIN-    │ │   data
                   │  │  ARCHITECTURE)  │ │
                   │  └─────────────────┘ │
                   │  ┌─────────────────┐ │
                   │  │ Cross-cutting:  │ │
                   │  │ authz, audit,   │ │
                   │  │ events, jobs    │ │
                   │  └─────────────────┘ │
                   └──┬────┬────┬────────┘
              ┌───────▼┐ ┌──▼───┴──┐ ┌───▼────────┐
              │  DB    │ │ Object │ │ Job/queue  │
              │(system │ │storage │ │ mechanism  │
              │of rec.)│ │ + CDN  │ │(async work)│
              └────────┘ └────────┘ └────────────┘
```

## 2. Deployment Units

`[Proposed]`

| Unit | Contains | Why together/separate |
|------|----------|----------------------|
| Backend service | All domain modules D1–D15 | FRS flows are tightly coupled (submission→vote→evaluate→score→rank→qualify→notify); single transactional boundary preserves consistency `[FRS §36]` |
| Media worker | Transcode/thumbnail/scan pipeline | Async, CPU-bound, independently scalable — may be same deployable in MVP via job queue `[Open OD-12]` |
| Web frontends | Static/SSR bundles per channel | Independent deployables |
| Mobile app | Store-distributed | Independent |

### Why modular monolith over microservices `[Proposed]`

- FRS consistency requirements (score aggregation, vote integrity, override audit) favor shared transactions `[FRS §22][§30][§36]`.
- Competition traffic is spiky and cross-domain; premature distribution adds distributed-transaction cost without an ownership or scale driver.
- Module boundaries are drawn as if services might split later: one data owner per entity, no cross-module table access, contracts via queries/events.

### When to split `[Proposed]`

Extract a module into its own service only when: independent scaling need is demonstrated, a distinct team owns it, or it has a different availability/security envelope. Candidates, in order: media pipeline, notification delivery, messaging (if real-time scale demands).

## 3. Cross-Cutting Mechanisms

`[Proposed]`

| Mechanism | Purpose | Notes |
|-----------|---------|-------|
| AuthZ middleware | System-role permission checks on every request | Never reads talent/skill data `[BR-02]` |
| Domain events | In-process or queued events: `SubmissionPublished`, `VoteRecorded`, `EvaluationSubmitted`, `RoundClosed`, `RankingComputed` | Decouple side-effects (notifications, audit, aggregation triggers) |
| Job runner | Scheduled/async: media processing, score aggregation, notification delivery, deadline transitions | Must be observable and retryable |
| Audit writer | Append-only AuditLog entries with business ops `[FRS §30]` | See §18 baseline |
| Configuration engine | Generic pattern for admin-configured data (skills, rubrics, scoring weights, voting rules, round settings) `[AP-4]` | Single consistent pattern across domains |

## 4. Request Flow Example (competition scoring)

```text
Judge web → POST /evaluations → API layer (authN+authZ: Judge role)
  → D11 Judging module (validate assignment, rubric version, deadline)
  → persist JudgeEvaluation (immutable) + AuditLog entry
  → emit EvaluationSubmitted event
  → D12 Scoring consumes event → recompute aggregate when round closes
  → Ranking updated → RoundClosed/RankingComputed events
  → D13 Notification → results notifications
```

## 5. Consistency Strategy

`[Proposed]`

- **Strong consistency** inside a module's transaction boundary: submissions, evaluations, votes, scoring writes, audit entries.
- **Eventual consistency** across modules via events: notifications, leaderboard read models, feed/trending counters, analytics.
- **Deadline-driven transitions** (round close, voting close, evaluation close) via scheduled jobs that emit state-change events — never via lazy checks alone, so audit trails are complete `[FRS §30]`.
