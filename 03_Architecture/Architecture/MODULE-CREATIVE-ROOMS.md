# Module Design — 07: Creative Rooms (Project Collaboration)

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 07)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–06 without modification.

## 1. Purpose

Own StarMitra's **project/collaboration domain** — Creative Rooms where users form and participate in creative projects using **contextual contribution roles** `[FRS §13]`. This is the home of `ProjectContributionRole` — the concept that must never collapse into `TalentSkill` or `SystemRole`.

## 2. Core Product Model — the separation is the design

```text
User: TalentSkills = [Singer, Actor, Director, DialogueWriter]   ← MODULE 03 (capability)
        │
        ├── Project A → contribution = Singer
        ├── Project B → contribution = Director                  ← THIS MODULE (contextual)
        └── Project C → contribution = DialogueWriter
```

- `TalentSkill` = **capability** (what the user *can do* — stable, profile-level)
- `ProjectContributionRole` = **contribution** (what the user *did in this project* — contextual, project-scoped)
- Never inferred from skills; **explicitly assigned**; never a global permission; never grants project authorization by itself.

## 3. FRS Scope `[FRS §13]` — traced

Creative Room creation · title/description/project type · required skills · invitations + acceptance · project membership · contextual contribution roles · project messaging · project assets · tasks · final output · credits · visibility/access.

## 4. Module Boundary

| Owns | Does NOT own |
|------|--------------|
| `CreativeRoom`, `ProjectMember`, `ProjectContributionRole`, `ProjectInvitation`, `ProjectTask`, `ProjectAsset` (ref), `FinalOutput` (ref), `ProjectCredit` | User/roles (M01), TalentSkill/UserTalentSkill (M03), MediaAsset lifecycle (M04), Conversation/Message (M06), notification delivery, moderation policy, portfolio display |

## 5. Creative Room Lifecycle

`Draft → Published/Open → Active → Completing → Completed → Archived` *(state names are design proposal — FRS describes behavior, not status names; exact set = open)*. Not every room is public — visibility designed per §16.

## 6. Project Type

`[FRS §13]` includes project type. **No fixed taxonomy invented** — configurable values; taxonomy = open/admin-config decision.

## 7. Required Skills

Room declares `RequiredSkill` → **references `TalentSkill` IDs from MODULE 03** (read contract, never a duplicate Skill table). Optional requirement metadata *(proposal)*. Deactivated skill → existing requirement remains but stops matching new invites. Required skills inform invitations/discovery — **never authorization**.

## 8. Project Members

`ProjectMember`: userId, roomId, joinedAt, membership state, invitation origin. **Contribution-role cardinality explicitly OPEN** — design supports one-or-more roles per member; do not impose single-role limitation.

## 9. Project Contribution Roles — critical

`ProjectContributionRole`: roomId, memberId, roleName (from contribution-role taxonomy — aligned to but distinct from skill names; taxonomy = open/admin-config). Explicit assignment by project admin/moderator or self-declared-subject-to-approval *(workflow open)*. Contextual to the room — the `Director`-of-Project-A ≠ `Director`-of-Project-B automatically. **Examples (Singer/Actor/Director/Writer) are illustrative, not a fixed list** — configurable taxonomy avoids conflict with MODULE 03.

## 10. Invitations

`ProjectInvitation`: roomId, inviterId, inviteeId, required-skill context*(optional)*, status (`Pending → Accepted | Declined | Withdrawn | Expired`), timestamps, expiry *(duration open)*. Duplicate-invite prevented (unique on room+invitee+active-status). **Inviter authz = project admin/owner — never skill possession.**

## 11. Authorization

Auth = authenticated user + project-ownership/admin + membership + invitation state + project visibility + moderation. **`TalentSkill`, `SystemRole`, `ProjectContributionRole` are never substitutes** — project admin comes from project ownership/explicit admin role; system roles grant admin surfaces only.

## 12. Project Messaging (MODULE 06 boundary)

Room owns `projectId` → Connect holds `Conversation.projectId`; **membership validated against Rooms** (the authoritative source) — Connect reads, never duplicates. No messaging model here.

## 13. Project Assets (MODULE 04 boundary)

`ProjectAsset` = room-owned attachment record (roomId + mediaId + role/context); Media owns the binary/lifecycle/signed-access per ADR-008. Access = room-membership + media visibility.

## 14. Tasks

`ProjectTask`: title, description, status, creator, assignee (ProjectMember ref), dueDate*(optional, open)*, timestamps, completion. **Conceptual task capability only — no Kanban/sprints/Gantt/time-tracking** unless FRS requires. Workflow/status set = open.

## 15. Final Output

`FinalOutput`: roomId + mediaRef (MODULE 04) + finalization state + visibility. Multiple outputs allowed? — **OPEN** (FRS silent). Immutability/versioning per media rules.

## 16. Credits — cross-module contract

`ProjectCredit`: authoritative room-side record of actual contribution (roomId + memberId + contributionRole + verified/finalized state). **Portfolio consumes this read contract** — credits reflect *verified project contribution*, not unilateral user claims. Portfolio module designed later; this defines the source-of-truth it reads.

## 17. Visibility & Discovery (MODULE 05 boundary)

Room owns visibility state (public/private/members-restricted — options open); Discovery reads *eligible* rooms via read contract; `public=searchable` vs `private=invisible` **not assumed** — designed as explicit states; moderation-restricted excluded. **Discovery never overrides room authz.**

## 18. Moderation

Reports/restriction enforcement consumed; policy owned by Moderation. Events: project reported, member reported, asset restricted, room suspended/removed.

## 19. Notifications

Room emits signals (`InvitationSent`, `InvitationAccepted`, `MemberAdded/Removed`, `TaskAssigned`, `ProjectCompleted`, `FinalOutputReady`); Notifications owns delivery. No provider logic.

## 20. Data Model (conceptual)

| Entity | Purpose | Invariants |
|--------|---------|-----------|
| `CreativeRoom` | project container | id, title, description, type, visibility, status, ownerId |
| `ProjectMember` | membership | roomId+userId unique; memberId referenced by contributions/tasks |
| `ProjectContributionRole` | contextual contribution | roomId+memberId+role; taxonomy-config |
| `RequiredSkill` | declared need | roomId+skillId (→M03) |
| `ProjectInvitation` | invite lifecycle | unique active invite per room+invitee |
| `ProjectTask` | work item | assignee must be member |
| `ProjectAsset` | room-media link | roomId+mediaId (→M04) |
| `FinalOutput` | completed output | roomId+mediaRef |
| `ProjectCredit` | contribution record | links member→role→room |

Authoritative throughout; no derived entities at MVP.

## 21. API Surface (conceptual — ADR-007)

| Endpoint | Purpose |
|----------|---------|
| `POST/GET /api/v1/rooms` | create/list |
| `GET/PUT /api/v1/rooms/{id}` | detail/update |
| `POST /api/v1/rooms/{id}/skills` | declare required skill |
| `POST /api/v1/rooms/{id}/invitations` | invite |
| `POST /api/v1/invitations/{id}/accept|decline` | respond |
| `GET/POST/DELETE /api/v1/rooms/{id}/members` | membership |
| `POST /api/v1/rooms/{id}/members/{memberId}/roles` | assign contribution role |
| `GET/POST /api/v1/rooms/{id}/tasks` | tasks |
| `POST /api/v1/rooms/{id}/assets` | attach media ref |
| `POST /api/v1/rooms/{id}/finalize` | complete + final output |

## 22. Concurrency / Consistency

Invite-accept → membership write in one tx; duplicate-invite unique constraint; contribution-role assignment validated against membership; finalization = explicit state transition; asset-attach validates media+room; task updates optimistic-locked. PG-only; no locks/brokers/caches.

## 23. Security

Room enumeration (uniform errors + visibility gating); unauthorized membership/invite (authz per-action); asset leakage (membership+media authz); contribution-role tampering (admin-only assignment); IDOR (scoped lookups); moderation bypass (state filtering). **TalentSkill never authorizes.**

## 24. Open Decisions (Product Owner)

Lifecycle states, project-type taxonomy, visibility options, join-via-approval vs direct, max members, contribution-role cardinality + taxonomy + assignment workflow, invite expiry + permissions, task workflow/due-dates, final-output cardinality, completion semantics, credit approval/editing, archival/deletion, asset retention, ownership transfer.

## 25. Acceptance Validation

Room=project/collaboration owner ✓ · M03 skills referenced not owned ✓ · M04/M06/M09 delegated ✓ · contribution roles contextual + never global permission ✓ · credits = verified contribution ✓ · PG authoritative ✓ · no unauthorized infra ✓ · no implementation ✓

## 26. Traceability

- **FRS:** §13 Creative Rooms · §6 roles · §30 audit
- **ADRs:** ADR-001 boundary · ADR-003 PG · ADR-007 API · ADR-008 assets
- **Modules:** M01 identity · M03 skills (refs) · M04 media (refs) · M05 discovery · M06 messaging · Moderation · Notifications
