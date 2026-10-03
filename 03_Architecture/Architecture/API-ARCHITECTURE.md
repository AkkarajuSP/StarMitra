# StarMitra — API Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Draft for review | **Decision status:** under review as **OD-07** in the [Decision Register](../ADR/ARCHITECTURE-DECISION-REGISTER.md)

## 1. Style & Contracts

`[Proposed]` REST over HTTPS, JSON payloads, **OpenAPI 3.x** contracts maintained in `05_API-Specifications/OpenAPI/` — contract-first where practical, generated docs per release. Contract tests guard compatibility `[Proposed]`.

`[Open]` GraphQL for feed/discovery aggregation was considered but deferred — REST suffices for MVP; revisit if client query flexibility demands it.

## 2. Versioning

`[Proposed]` URI major-version prefix `/api/v1`. Breaking changes → new major version; additive changes in place. Mobile clients make forced-upgrade expensive — deprecate, don't break.

## 3. API Surfaces by Audience

`[FRS §5][§6]` — separate surfaces, separate authZ policies, shared backend:

| Surface | Base | Audience | Notes |
|---------|------|----------|-------|
| Public | `/api/v1/public/…` | Visitors | Discovery, public profiles/competitions; no authN; rate-limited |
| App | `/api/v1/…` | Authenticated users | Creator + audience capabilities |
| Judge | `/api/v1/judge/…` | Judge system role | Only assigned submissions/evaluations `[FRS §19]` |
| Admin | `/api/v1/admin/…` | Admin/Super Admin | Config, moderation, ops, audit `[FRS §27]` |

## 4. Resource Map (indicative, not exhaustive)

`[Proposed]` — aligns with `05_API-Specifications/` folder structure:

```text
/auth            register, otp/verify, login, logout, password-reset  (D1)
/users           profiles, /me/skills, portfolio, follows             (D1/D2/D4)
/skills          taxonomy read; admin CRUD under /admin/skills        (D2)
/media           upload-init, complete, metadata, status              (D3)
/feed, /discover, /search                                             (D5)
/conversations   + /messages, read state, blocks                      (D6)
/rooms, /projects  members, contribution-roles, tasks, assets         (D7)
/competitions    list/detail; admin CRUD, rounds, categories          (D8)
/submissions     create, drafts, status; admin review                 (D9)
/votes           cast; counts (visibility-gated)                      (D10)
/judge           assignments, evaluations draft/submit                (D11)
/evaluation-templates  admin rubric builder + publish                 (D11)
/results, /leaderboards                                               (D12)
/notifications   list, read, preferences                              (D13)
/reports         file reports; admin moderation queue                 (D14)
/admin           users, roles, config, audit queries                  (D15)
```

## 5. Cross-Cutting API Rules

| Rule | Status |
|------|--------|
| Authorization middleware checks `UserSystemRole`/permissions — **never** talent skills or categories | `[BR-02][FRS §38]` |
| One user may submit in multiple eligible categories without duplicate identities | `[FRS §38]` |
| Judge endpoints expose only assigned submissions | `[FRS §19]` |
| Visibility gating (Public/Followers/Collaboration-Only/Private) applied server-side on every content read | `[FRS §9][§10]` |
| Vote counts hidden/shown per competition config | `[FRS §18]` |
| Idempotency keys on vote/submission/evaluation POSTs to prevent duplicates | `[Proposed][BR-14]` |
| Consistent error model (`code`, `message`, `details`, `traceId`) | `[Proposed]` |
| Pagination: cursor-based for feeds/lists; offset acceptable for admin tables | `[Proposed]` |
| Bulk/expensive endpoints rate-limited; voting endpoints specifically hardened | `[BR-14][Proposed]` |

## 6. Real-Time

`[Open OD-8]` Candidates for near-real-time: chat delivery/read receipts `[FRS §12]`, live vote counts where enabled `[FRS §18]`, in-app notifications `[FRS §25]`. Options: WebSocket channel (bidirectional, chat-friendly), SSE (simpler, server-push), or polling for MVP. Decision deferred to D6 design — evaluate against mobile battery/reconnection cost and scale.

## 7. Events (internal contract surface)

`[Proposed]` Domain events are the cross-module contract — the same names will back notifications, audit, and projections:

```text
UserRegistered, AccountStatusChanged, ProfileUpdated
MediaUploaded, MediaProcessed, MediaPublished, MediaRejected
RoomCreated, MemberJoined, ContributionRoleAssigned, ProjectFinalized, CreditRecorded
CompetitionPublished, RoundOpened/Closed, VotingOpened/Closed, JudgingOpened/Closed
SubmissionCreated/Approved/Rejected
VoteRecorded/VoteRejected, EvaluationSubmitted, TemplatePublished
ScoresComputed, RankingPublished, QualificationDecided, OverrideApplied
ReportFiled, ContentHidden, UserSuspended, MessageSent, NotificationDelivered/Failed
```

## 8. Non-Functional API Requirements

`[FRS §36]` / `[Proposed]`

- Stateless auth tokens (mechanism pending OD-5) for horizontal scaling.
- Response-time budgets defined per surface during API design phase.
- Every mutating endpoint that affects votes/evaluations/overrides writes an `AuditLog` entry `[FRS §30]`.
- OpenAPI diffs reviewed in PRs; breaking changes require explicit sign-off.
