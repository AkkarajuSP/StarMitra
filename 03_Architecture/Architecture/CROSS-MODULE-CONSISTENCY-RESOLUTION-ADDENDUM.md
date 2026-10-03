# StarMitra Cross-Module Consistency Resolution Addendum

**Parent:** [CROSS-MODULE-CONSISTENCY-REVIEW.md](CROSS-MODULE-CONSISTENCY-REVIEW.md) (`2f109ff`) | **Status:** All findings resolved — no ADR changes required

## 1. Purpose

Document the explicit resolution of findings CM-01…CM-07 from the cross-module consistency review. No architecture was redesigned; no FRS boundary changed; no open/proposed state was promoted to accepted requirement.

## 2. Resolution Summary

| ID | Finding | Resolution | New entity/module? |
|----|---------|-----------|-------------------|
| CM-01 | Social engagement unowned | **M21 Social Engagement** — restores baseline domain D4 | Module added (baseline domain, not invented) |
| CM-02 | AuditLog/PlatformConfig unowned | **Platform Kernel** (cross-cutting, non-business) | No module — platform concern documented |
| CM-03 | Block omitted | `UserBlock` added to M06 | Entity added to existing module |
| CM-04 | Naming drift | `ConversationMember` standardized | — |
| CM-05 | VoteConfig boundary | M09=structure ref / M11=behavior config | — |
| CM-06 | ProgressionConfiguration open | **M15-owned** confirmed | — |
| CM-07 | Judge namespace drift | `/api/v1/judges/me` standardized | — |

## 3. CM-01 — Social Engagement Ownership → **M21 Social Engagement**

**Decision:** the baseline's **D4 Social Engagement** domain (`Follow`, `Like`, `Comment`, `EngagementCounter`) is assigned to a new module **M21 — Social Engagement**.

**Why a module (justification):** D4 is an existing accepted baseline domain that was inadvertently left unassigned — assigning it restores the baseline, it doesn't invent capability. Placements evaluated and rejected:

- **M02 User Profile — rejected:** M02 owns *profile presentation/identity data*; `Follow`/`Like`/`Comment` are **engagement actions with their own lifecycle** (create/remove, target-generic relationships, counters). Folding them into Profile would give M02 two unrelated authorities (presentation + engagement-write), violating single-ownership.
- **M05 Discovery/Feed — rejected:** M05 is explicitly **read-only/derived**; it consumes engagement as a signal and must never write domain state.
- **M06 Connect — rejected:** engagement is platform-wide (media, portfolio, submissions, content), not messaging-scoped.

**Scope (charter — full spec pending):**

- **Entities owned:** `Follow` (followerId→followeeId), `Like` (userId→targetRef), `Comment` (userId→targetRef, body, status), `EngagementCounter` (targetRef→aggregate counts — **derived/authoritative-hybrid: counter record owned here, rebuildable from Like/Comment/Follow rows**)
- **Lifecycle authority:** create/remove follow; like/unlike; comment add/remove (moderation-aware); counters recomputed/rebuildable
- **APIs owned:** `/api/v1/social/follows` · `/likes` · `/comments` · `/engagement` (read)
- **M02 relationship:** Profile *reads* follow/follower counts via contract — never owns
- **M05 relationship:** Discovery consumes `EngagementCounter`/follow signals for feed + trending — never writes
- **M17 relationship:** emits `UserFollowed`/`ContentLiked`/`CommentAdded` signals — M17 delivers
- **M04 relationship:** likes/comments target media by **reference** — no media ownership
- **Authorization:** `SystemRole` only — `TalentSkill`/`ProjectContributionRole` never authorize engagement actions; target visibility enforced per owning module
- **Counters:** `EngagementCounter` is the authoritative counter record but **rebuildable** from source `Like`/`Comment`/`Follow` rows — source rows are the ultimate truth

## 4. CM-02 — AuditLog / PlatformConfig Ownership → **Platform Kernel**

**Decision:** `AuditLog`, `PlatformConfig`, and platform-level analytics projections are owned by a **Platform Kernel** — a documented cross-cutting concern, **not a business module**. This resolves the gap without forcing an unrelated business domain to absorb platform infrastructure.

**Model:**

```text
Every module (M01–M21)
   └─ appends audit events via Platform Kernel write-contract (transactional, append-only)
         ↓
   Platform Kernel: AuditLog (append-only) · PlatformConfig · analytics projections
         ↑ read-contract
   M19 Admin Portal — views only (presentation, never writes)
```

- **`AuditLog`:** authoritative audit history — **not telemetry, not analytics**; append-only; every module writes its own domain events through the kernel contract; no module owns the shared record
- **`PlatformConfig`:** platform-level configuration — kernel-owned
- **Analytics projections:** kernel-owned derived data feeding ADR-013 reporting surfaces — never business-authoritative
- **M01:** emits `AuthenticationAuditEvent` → AuditLog via the kernel contract — does **not** own the log (avoids accidental whole-audit ownership)
- **M18:** writes moderation decisions to AuditLog via the contract — owns `ModerationDecision`, not the log
- **M19:** reads/views audit — presentation-only, unchanged

## 5. CM-03 — Block Ownership → **M06 `UserBlock`**

`UserBlock` added to M06 ownership: `blockerId → blockedId`, createdAt, active state — **user-initiated privacy control.**

- **Lifecycle:** user creates/removes blocks; per-user relationship record
- **Messaging impact:** blocker→blocked message-send rejected
- **Discovery/feed impact:** blocked-user content suppressed — M02 visibility + M05 discovery consume
- **Notification impact:** no notifications to/from blocked relationships
- **Distinct from M18 `ModerationRestriction`:** block = user privacy choice (M06); restriction = admin-enforced moderation (M18) — **never merged, different authority and reversibility**

## 6. CM-04 — ConversationMember Naming → **standardized**

`ConversationMember` adopted; `ConversationParticipant` retired everywhere it meant the same concept. Updated: `DOMAIN-ARCHITECTURE.md`, `REALTIME-ARCHITECTURE.md`, `DATA-ARCHITECTURE.md`. (`CompetitionParticipant` is a **different concept** — unchanged.)

## 7. CM-05 — VoteConfig Boundary → **documented split**

| Owner | Owns |
|-------|------|
| **M09** | Voting *structure* — whether a competition/category/round permits voting; the `VoteConfig` **reference** attached to the round |
| **M11** | Voting *behavior* — the `VoteConfig` entity itself: voter eligibility, limits, dedup, abuse toggles, window enforcement, target type |

M09's round carries a **reference to** `VoteConfig`; the config record lives in M11. Updated in both module specs. No duplicated ownership.

## 8. CM-06 — ProgressionConfiguration → **M15-owned**

Confirmed: `ProgressionConfiguration` is owned by **M15 Round Progression** (versioned, immutable once progression begins). M09 owns `CompetitionRound` structure; M14 owns scoring/ranking/qualification; M15 owns progression config + decision; M16 consumes for presentation. M15 spec updated; no `CompetitionRound` lifecycle duplicated.

## 9. CM-07 — Judge API Namespace → **`/api/v1/judges/me`**

Judge self-service surface standardized on `/api/v1/judges/me/...` — M20 updated from `/api/v1/judge/me/...`. Convention: `/api/v1/judges` = admin surface; `/api/v1/judges/me` = judge self-service. Single namespace, consistent across M12/M13/M20.

## 10. Updated Ownership Matrix (deltas)

| Entity | Owner | Change |
|--------|-------|--------|
| `Follow`, `Like`, `Comment`, `EngagementCounter` | **M21** | previously unassigned |
| `AuditLog`, `PlatformConfig`, analytics projections | **Platform Kernel** | previously ambiguous |
| `UserBlock` | **M06** | added |
| `VoteConfig` | **M11** | boundary clarified (M09=ref only) |
| `ProgressionConfiguration` | **M15** | confirmed |
| `ConversationMember` | M06 | naming standardized |

## 11. Updated Dependency Implications

- **M21** consumes: M01 (authz), M04/M08/M10 targets (refs); → M02 (counts read), M05 (engagement signals), M17 (events), M18 (comment moderation)
- **Platform Kernel** consumed by: all modules (audit writes), M19 (audit views)
- **M06** `UserBlock` consumed by: M02, M05 — dependency direction unchanged
- No new circular dependencies; dependency graph remains a DAG.

## 12. Remaining Open Product Owner Decisions

Module count for M21 spec (full spec + sequencing), engagement feature scope (comments on which targets), all previously-listed open decisions unchanged — **no PO decision was resolved silently**.

## 13. Database Design Readiness

**Ready — all ownership is now assigned.** Every entity in the ownership matrix has exactly one authoritative owner; naming is standardized; config boundaries are explicit. Recommended next step: write the **M21 Social Engagement module spec** (charter defined here), then proceed to Canonical Database Design.

**ADR impact:** none — no contradiction with ADR-001…013 was found; no ADR modified.
