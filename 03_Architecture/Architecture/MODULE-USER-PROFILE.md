# Module Design — 02: User Profile

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 02)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01 (AuthN/Identity) without modification.

## 1. Purpose

Own the user's **public/platform-facing profile** — the identity others see and the user presents — kept strictly separate from authentication identity (MODULE 01) and creative capabilities (Talent Skills, MODULE 03).

## 2. Scope

User profile data, display identity, profile visibility/presentation, profile completion state, profile preferences, and references to profile-related media. Post-registration profile creation is triggered by an Auth event (MODULE 01 emits; this module consumes).

## 3. Responsibilities

- Profile information + display identity (display name, handle/username, bio/about, location if FRS-supported)
- **Profile visibility** + discovery visibility
- Profile **presentation** (what a public/visitor view looks like)
- Profile **completion** state
- Profile **preferences** where FRS/architecture justifies
- Profile-media *associations* (avatar/cover refs — not binaries)

## 4. Non-Responsibilities

Does **not** own: authentication identity/credentials/sessions/tokens/OTP/system-role assignment/auth-audit (→ MODULE 01); **TalentSkill taxonomy/associations/proficiency** (→ MODULE 03); portfolio media items as content (→ Media); ProjectContributionRole (→ Rooms/Collaboration); moderation decisions (→ Moderation); delivery of notifications.

## 5. Boundary — Authentication vs Profile

| Concern | Owner |
|---------|-------|
| Who you are (login identity, contact/verified identifiers) | **MODULE 01** |
| Sessions/tokens/credentials/roles/audit | **MODULE 01** |
| How you present to the platform (name, bio, visibility) | **this module** |
| What you can do (authz, role checks) | domain modules (authz) |

`User` (Auth identity) — `UserProfile` (presentation) — `TalentSkillAssignment` (capabilities) — `UserSystemRole` (permissions) are **separate concepts, separate owners**.

## 6. Talent Skill Boundary (critical)

**Profile displays skills; it does not own them.** Talent Skills module owns taxonomy + associations + proficiency + lifecycle. Profile renders whatever the user is authorized to display:

```text
User
 ├── UserProfile ──► displays skill references (read-model)
 └── TalentSkillAssignment ──► owned by Talent Skills module
         ├── Singer · Actor · Director
```

**Multi-talent is never collapsed** — a profile shows a skill set, not a single category.

## 7. FRS Profile Requirements — traceability

| Requirement | FRS | Design mapping |
|-------------|-----|----------------|
| Public talent/user profiles `[§9]` | ✓ | `UserProfile` + public view |
| Bio/about, display name, profile photo `[§9]` | ✓ | profile fields |
| Multiple talents displayed `[§9]` | ✓ | skill display (read-only refs) |
| Portfolio link/display `[§9]` | ✓ | profile → media/portfolio refs |
| Visibility/discoverability `[§9][§11]` | ✓ | profile visibility + discovery scope |
| Profile completion/prompting | *inference* | `ProfileCompletion` (design proposal — marked) |
| Social links | *inference — open question* | optional proposal |
| Cover/banner media | *inference — open question* | optional proposal |

*No mandatory field invented; inferred items marked.*

## 8. Profile Lifecycle

Owned by Profile (presentation-state, not access-state):

| State | Trigger | Owner |
|-------|---------|-------|
| `Created` | registration → profile auto-created | Profile |
| `Incomplete` | required-FRS fields unfilled | Profile (completion metric) |
| `Active` | complete + discoverable per visibility | Profile |
| `Restricted` | moderation visibility cap | **Moderation** decides; Profile enforces display |
| `Deactivated` | account deactivation | **Auth** decides; Profile hides |

Access-level states (Suspended/Blocked/Deactivated) remain MODULE 01-owned — Profile mirrors their presentation effect, doesn't own the state.

## 9. Profile Visibility

| Setting | Effect |
|---------|--------|
| Public | Discoverable + public view (audience-facing) |
| Private/limited *(if product wants)* | Hidden from public discovery; authorized viewers only *(open question)* |
| Moderation-restricted | View-limited/hidden pending review |
| Blocked-user | Content invisible to blockers `[FRS §12]` |

**Visibility ≠ authorization** — visibility is a display-scope rule layered on domain authorization; it never grants access beyond what authz allows.

## 10. Profile Data Model (conceptual — minimal)

| Entity | Required? | Fields (FRS-justified + *proposal*) |
|--------|-----------|-------------------------------------|
| `UserProfile` | **Yes** | userId, displayName, handle/username *(proposal)*, bio, location *(proposal)*, visibility, avatarMediaId *(ref)*, coverMediaId *(proposal)*, timestamps |
| `ProfileCompletion` | *proposal* | derived completion score — could be computed, not stored |
| `ProfileSocialLink` | *open question* | url, label, ordering |
| `ProfilePreferences` | *proposal* | visibility/discovery opts if FRS scope requires |

*Lean model: start with `UserProfile`; add entities only when a requirement demands.*

## 11. Media Relationship (ADR-008 preserved)

- `avatarMediaId` / `coverMediaId` are **references to Media-owned objects** — Profile stores the association, Media owns the binary + lifecycle.
- Upload/processing/delivery go through Media's direct-upload + signed-URL flow; Profile just holds the resulting media ID.
- Media replacement → update the reference (transaction: validate media exists + belongs to user → update ref).

## 12. Talent Display

`GET /profiles/{id}` returns profile + the user's **authorized** skill display list (read via Talent Skills module's contract — cross-module read, never a table join into another module's data at the boundary level). Ordering/primary-skill display = Talent Skills data; Profile presents it.

## 13. API Surface (conceptual — ADR-007)

| Endpoint | Purpose | Auth | Authz | Notes |
|----------|---------|------|-------|-------|
| `GET /api/v1/profiles/me` | Own profile | auth'd | owner | full view incl. private fields |
| `PUT /api/v1/profiles/me` | Update own profile | auth'd | owner | validation; audit |
| `GET /api/v1/profiles/{id}` | Public/visitor profile | auth'd-or-public | visibility-gated | filtered view per visibility + moderation state |
| `PUT /api/v1/profiles/me/visibility` | Change visibility | auth'd | owner | audit |
| `PUT /api/v1/profiles/me/avatar` | Set avatar (media ref) | auth'd | owner + media-ownership check | validates media belongs to user |
| `GET /api/v1/profiles/me/completion` | Completion state | auth'd | owner | computed |

Errors: RFC 9457 + stable codes (`PROFILE_NOT_FOUND`, `PROFILE_FORBIDDEN`, `PROFILE_RESTRICTED`, `MEDIA_NOT_OWNED`, validation errors) + correlation ID.

## 14. Profile Authorization

- **View:** owner (full), authenticated users (per visibility), public (per visibility + moderation), admin/moderation (scoped)
- **Edit:** owner only; admin-edit only via moderation workflow
- **Blocked users:** requester blocked → profile hidden `[FRS §12]`
- **Never:** TalentSkill/ProjectContributionRole as authorization.

## 15. Profile + Moderation / Discovery / Notifications

- **Moderation:** Moderation *decides* restriction; Profile *enforces* display state; Auth *enforces* access consequences. No policy duplication.
- **Discovery:** reads **published** profile-visible data via Profile's contract — never writes; never reads restricted/private fields.
- **Notifications:** profile *change events* may trigger notifications (e.g., "profile published") — Profile emits the event; Notifications owns delivery.

## 16. Data Privacy

Public vs private field separation; visibility gating at read-path; data minimization in public view; deactivation → profile hidden (not deleted — deletion = product-retention decision, flagged); audit on visibility/ownership-sensitive changes.

## 17. Transactions / Consistency

- Profile create (on registration event) — single tx
- Profile update — single tx + validation
- Visibility change — tx + audit
- Avatar/cover change — validate media ownership + update ref, one tx
- PostgreSQL authoritative; unique handle/username enforced by constraint.

## 18. Audit

Audit-worthy: profile creation, visibility changes, moderation restriction changes, avatar changes. **Not** audited: ordinary reads/updates (high-noise) — unless product says otherwise.

## 19. Error Model

RFC 9457 + stable codes + correlation ID + validation errors; restricted/not-found responses indistinguishable where enumeration is a concern (`PROFILE_NOT_FOUND` vs `PROFILE_RESTRICTED` → same outward response for non-authorized viewers).

## 20. Testing (not implemented)

Unit: visibility-rule matrix, completion computation, media-ref validation. Integration: create→edit→visibility→public-view flows; blocked-user invisibility; moderation-restriction effect. Boundary: skills display read-only (no cross-module write); authz never delegated to skills/roles.

## 21. Open Questions (Product Owner)

1. Exact profile fields (handle, location, social links)
2. Private/limited visibility — in scope at MVP?
3. Completion rules + required-field list
4. Deletion vs deactivation retention
5. Social links — allowed at MVP?
6. Cover/banner media — in scope?

## 22. Traceability

- **FRS:** §9 profile/portfolio · §11 discovery · §12 blocking · §26 moderation · §30 audit
- **ADRs:** ADR-001 boundaries · ADR-003 PG · ADR-007 API/errors · ADR-008 media refs
- **MODULE 01:** owns identity/session/roles — consumed, never duplicated
