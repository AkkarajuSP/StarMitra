# Module Design — 21: Social Engagement

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 21 — restores baseline domain D4 per CM-01)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–20 without modification. M21 owns **social engagement only** — not a generic social-network module.

## 1. Purpose

Own the platform's **social engagement actions** — `Follow`, `Like`, `Comment`, and the derived `EngagementCounter` — consumed by profile display, discovery/feed signals, media `Followers` visibility, and notifications `[FRS §4][§11]`. Restores baseline domain D4 left unassigned until CM-01.

## 2. Module Boundary

| Owns | Does NOT own |
|------|--------------|
| `Follow`, `Like`, `Comment`, `EngagementCounter` (derived) | User/Profile (M01/M02), TalentSkill (M03), MediaAsset (M04), messaging/block *(M06 `UserBlock`)*, rooms (M07), portfolio (M08), **competition votes (M11)**, scoring/results (M14–16), moderation *decisions* (M18), notifications delivery (M17), analytics |

## 3. Follow

`Follow`: followerId → followeeId (user), createdAt, active state.

- **Target scope:** FRS supports **user→user** follows (`Followers` visibility exists in media privacy `[FRS §9][§10]`); other target types = **OPEN — not invented**
- **Uniqueness/idempotency:** unique (follower+followee); repeat follow = idempotent no-op; unfollow deactivates/removes
- **Authorization:** authenticated user; **follow requires no skill/role** — open to all users subject to target's account state (blocked/suspended/deactivated can't be newly followed)
- **Blocked interaction:** M06 `UserBlock` — a blocker→blocked pair cannot follow (either direction *(rule open)*); existing follow behavior on block = **open**
- **Moderation:** restricted accounts can't be followed per M18 state
- **Signals:** → M05 feed ("followed creators" candidates), → M17 (`UserFollowed` notification), → M04 `Followers` visibility ACL
- **Privacy:** follow-list visibility = **open** (public/mutual/private)

## 4. Like

`Like`: userId → targetRef *(typed reference to likeable content)*, createdAt.

- **Targets:** FRS-supported likeable content (media/portfolio items as surfaced by owning modules); exact target-type set = **OPEN**
- **Uniqueness/idempotency:** unique (user+target); repeat = idempotent; unlike removes
- **Authorization:** authenticated + target visible/accessible to the user per owning module's visibility/authz — **like never grants or bypasses access**
- **Moderation/visibility:** can't like moderation-restricted or non-visible content
- **Media relationship:** references `MediaAsset` via M04 ID — never stores media
- **Signals:** → M05 engagement signal, → M17 (`ContentLiked` — if product requires)
- **`Like` ≠ `Vote`:** competition voting is **exclusively M11** — a social like never counts as or converts to a competition vote

## 5. Comment

`Comment`: id, authorId → targetRef, body, createdAt/editedAt *(edit semantics open)*, status (active/removed).

- **Targets:** commentable content per FRS/architecture (media/portfolio/content items); target set = **OPEN**
- **Create/update/delete:** create supported; edit/delete = **OPEN** (not assumed); author + moderation removal
- **Reply/threading:** **OPEN — not invented** (flat comments MVP unless product requires)
- **Authorization:** authenticated + target-visible; comment never grants access
- **Moderation:** comments are reportable/moderatable via M18 (decision) — M21 enforces resulting removal/visibility
- **Signals:** → M17 (`CommentAdded`), → M05 (engagement signal)
- **Audit:** creation/removal audited; author+actor+target+timestamp

## 6. EngagementCounter — derived, never authoritative

`EngagementCounter`: targetRef → `{followCount, likeCount, commentCount}` — **rebuildable projection** from `Follow`/`Like`/`Comment` source rows.

- **Source entities are truth; counters are a performance/read projection**
- Update: incremental on engagement write (transactional where possible) or async refresh *(mechanism open)*; full rebuild supported
- **Stale counters:** acceptable briefly — never presented as authoritative; never a source for business decisions
- **Cache:** never authoritative (ADR-010)
- **Never determines:** competition votes, scores, rankings, qualification, progression — engagement is social only

## 7. Cross-Module Contracts

| Module | Contract |
|--------|----------|
| M01 | user identity + authz principal |
| M02 | profile reads follower/following counts + follow-state for display (read contract) |
| M04 | `Followers` visibility ACL evaluates follow relationship; like/comment target refs |
| M05 | consumes follows + engagement counters as feed/trending signals — read-only |
| M06 | `UserBlock` consulted before follow/interactions; M21 respects block state |
| M07 | *(potential future room-membership engagement — open)* |
| M08 | portfolio items as like/comment targets |
| M09 | *(competition entity engagement targets — open)* |
| **M11** | **hard boundary — competition votes never via M21** |
| M17 | consumes `UserFollowed`/`ContentLiked`/`CommentAdded` signals |
| M18 | moderation decisions enforced on M21 records (comment removal, restricted-user rules) |
| M19 | admin views via M21 APIs — orchestration only |
| M20 | none (no judge-facing engagement) |

## 8. Authorization

`SystemRole` governs capability; **`TalentSkill`/`ProjectContributionRole`/`JudgeExpertise` never grant engagement permission.** Action-level authz: authenticated user + target visibility (owning module's rules) + not-blocked + not-restricted. **M06 `UserBlock` = user privacy control; M18 `ModerationRestriction` = admin enforcement — never merged.**

## 9. Moderation Boundary

M18 decides; M21 enforces on its own records: comment removed → M21 marks it removed per M18 decision; restricted user → engagement actions blocked. **No `ModerationCase`/`ModerationDecision` duplicated inside M21.**

## 10. Visibility ≠ Authorization — preserved

Engagement existence ≠ content visibility ≠ authorization ≠ moderation state — four independent dimensions. Hidden/private/restricted content never becomes visible because it has engagement; engagement on restricted content follows the owning module's visibility rules.

## 11. API Endpoint Inventory (conceptual — `/api/v1/social`)

| Endpoint | Purpose | Status |
|----------|---------|--------|
| `POST /api/v1/social/follows/{userId}` | follow | defined |
| `DELETE /api/v1/social/follows/{userId}` | unfollow | defined |
| `GET /api/v1/social/follows/me` | my follows/followers (cursor) | defined |
| `POST /api/v1/social/likes` | like (target ref) | defined |
| `DELETE /api/v1/social/likes/{id}` | unlike | defined |
| `POST /api/v1/social/comments` | comment | defined |
| `PUT/DELETE /api/v1/social/comments/{id}` | edit/remove | **OPEN — if product approves** |
| `GET /api/v1/social/engagement/{targetRef}` | counts | defined (derived) |

RFC 9457 errors (`ALREADY_FOLLOWING`, `NOT_FOLLOWING`, `ALREADY_LIKED`, `TARGET_NOT_LIKEABLE`, `TARGET_RESTRICTED`, `BLOCKED`) + idempotent writes + cursor pagination.

## 12. Entity Ownership Table

| Entity | Owner | Purpose | Authoritative? | Referencing modules |
|--------|-------|---------|---------------|---------------------|
| `Follow` | **M21** | user→user relationship | ✅ | M02, M04, M05, M17, M18 |
| `Like` | **M21** | user→target engagement | ✅ | M05, M17, M18 |
| `Comment` | **M21** | user→target comment | ✅ | M05, M17, M18 |
| `EngagementCounter` | **M21** | derived counts | derived/rebuildable | M02, M05 |

## 13. Dependency Graph

```text
M21 → M01 (authz), M04/M08 (target refs), M06 (block state), M18 (restriction state)
consumers → M21: M02 (counts), M05 (signals), M17 (events), M19 (admin views)
```

**No circular dependency** — M21 consumes M06 block state; M06 does not consume M21. M02/M05 consume M21; M21 does not consume M02/M05 for truth.

## 14. Non-Goals

Authentication, system roles, talent skills, portfolio, messaging, Creative Rooms, **competition voting (M11)**, judge evaluations, scoring, ranking, progression, leaderboards, moderation decisions, analytics-as-domain — all explicitly outside M21.

## 15. Open Product Owner Decisions

Follow target types (user-only vs others), like target types, comment target types, comment edit/delete, reply/threading, engagement visibility rules, block-interaction semantics, notification trigger scope, counter freshness SLA, engagement retention/deletion on account/media removal, follow-list privacy.

## 16. FRS Traceability

`[FRS §4]` social engagement · `[§9][§10]` `Followers` visibility + privacy · `[§11]` discovery feed signals · `[§12]` blocking interaction · `[§26]` moderation · `[§30]` audit. **FRS mandates follows/likes/comments as capabilities; target-type sets and edit/thread semantics are not FRS-specified → OPEN.**

## 17. Database Readiness

**Ready.** Entities, uniqueness rules, and ownership are unambiguous; target polymorphism pattern is established (typed refs, same as `PortfolioItemMedia`/`ModerationEvidenceReference`); counters are explicitly derived. The open decisions affect *constraints/scope*, not the schema shape — schema design can proceed with target-type as a configurable enum.
