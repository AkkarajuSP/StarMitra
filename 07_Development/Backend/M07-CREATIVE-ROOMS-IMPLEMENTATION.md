# M07 — Creative Rooms — Implementation

**Slice:** eighth business implementation. **Status: complete.** M07 owns project-contribution truth.

## Ownership

`creative_rooms`, `project_members`, `project_contribution_roles`, `required_skills`, `project_invitations`, `project_tasks`, `project_assets`, `final_outputs`, `project_credits` — the full V1.5 surface.

## Contract surface (16 ops, unchanged)

Rooms CRUD+list (skillId filter) · members list · required-skills PUT/DELETE · invitations (invite/myInvitations/respond) · contribution-role assign · tasks list/create · assets link · final-outputs add · credits list.

## Models

- **Room**: PUBLIC/PRIVATE, owner+`@Version` optimistic lock (real column) — `If-Match` = version.
- **Member**: owner auto-member; ACTIVE/LEFT; join via accepted invitation only.
- **Required skills**: owner-managed, M03-validated ACTIVE (`SkillTaxonomyContract`).
- **Invitations**: PENDING unique-per-(room,invitee) partial index → CONFLICT; invitee-only respond; ACCEPT → membership.
- **Contribution roles**: freeform contextual names ("Lead Actor") — **never TalentSkill, never permission**; owner assigns to ACTIVE member; UQ (room,member,role).
- **Credits** (authoritative): minted at `addFinalOutput` — every ACTIVE role → VERIFIED `project_credit` (once per role). This is the only credit source M08 may link.

## Cross-module

| Dir | Contract |
|---|---|
| M07 → M03 | `SkillTaxonomyContract` (required-skills validation) |
| M07 → M04 | `MediaReferenceContract` (asset/final-output validation) |
| M08 → M07 | `ProjectCreditContract.isLinkableCredit` — M08 now verifies (verified + caller-owned) — credit seam closed |
| M06/M21 → M07 | `ProjectMembershipContract.isActiveMember` (future seam) |

## Security

Owner-only mutations (non-owner → NOT_FOUND, no ownership leak) · private room invisible to non-members · members-only tasks · invitee-only respond · foreign credit link rejected.

## Testing

`RoomFlowIT` 7 real-PG17: room lifecycle+@Version/stale-ETag, private visibility, invite lifecycle (dup-CONFLICT, foreign-respond, accept→member, resolved→invalid), required-skill M03 validation, roles contextual+dup+non-member+non-owner, **finalize→verified-credit→M08-link→foreign-link-rejected** end-to-end, tasks members-only. Suite: **127**.

## Known follow-ups

- Invitation `expires_at` is stored but auto-expiry sweep not implemented (open product decision).
- Leave-room / member self-removal — no contract op.
- Task update/complete endpoints — not in contract.
- M06 project-conversation wiring (membership contract ready).
