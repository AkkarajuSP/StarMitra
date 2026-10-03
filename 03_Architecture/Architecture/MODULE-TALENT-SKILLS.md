# Module Design — 03: Talent Skills

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 03)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01/02 without modification.

## 1. Purpose

Own StarMitra's **multi-talent capability model**: the configurable skill taxonomy and each user's associations to skills. A single `User` may hold multiple `TalentSkill` associations — this is a foundational FRS distinction.

## 2. Responsibilities

- Configurable `TalentSkill` taxonomy (admin-managed)
- `UserTalentSkill` associations (multi-skill per user)
- Optional `SkillProficiency` representation
- Skill activation/deactivation lifecycle
- Skill display metadata for Profile/Discovery read contracts
- Skill context for competition eligibility (read contract — competition owns the rule)

## 3. Non-Responsibilities

Does **not** own: authentication/sessions/system roles (MODULE 01); profile presentation (MODULE 02); **ProjectContributionRole** (room/project domain); competition scoring/judging/ranking; judge authorization; portfolio content (Media/Portfolio modules); any form of permission.

## 4. Core Domain Model (conceptual)

| Entity | Meaning | Fields |
|--------|---------|--------|
| `TalentSkill` | Configurable skill **definition** | id, name, description *(proposal)*, category *(proposal)*, status (active/inactive), displayOrder *(proposal)*, timestamps |
| `UserTalentSkill` | User ↔ skill **association** | userId, skillId, proficiency *(optional ref)*, status, timestamps |
| `SkillProficiency` | Optional proficiency **configuration** | id, level/label *(product decision — not invented)* |

`TalentSkill` = "what can this person do" · **not** `SystemRole` ("what may they do in the app") · **not** `ProjectContributionRole` ("what did they contribute to a specific project").

## 5. Skill Taxonomy (FRS baseline `[FRS §9]`)

```text
Singer / Vocalist          Actor / Performer         Dancer / Choreographer
Music Director / Composer  Director                  Story Writer
Screenplay Writer          Dialogue Writer           Lyric Writer
Artist / Visual Artist     Cinematographer           Editor
Other Creative Skill
```

**Configurable, not immutable** — FRS supports admin-configured taxonomy. Design supports: id, name, description, category, active/inactive, display ordering. **No** hierarchy/synonyms/tags unless justified — kept open.

## 6. Admin Configuration

Admin/SuperAdmin (system role) manages taxonomy: create, update, activate/deactivate, ordering. Guardrails:

- Name uniqueness enforced by constraint
- **Referenced skills are deactivated, not deleted** — preserves historical integrity *(design recommendation — FRS doesn't mandate hard-delete protection, but referential integrity demands it)*
- Admin authorization comes from **system role** — never from possessing a skill
- All taxonomy changes audited

## 7. User–Skill Association

`User 1—* UserTalentSkill *—1 TalentSkill` — a user may hold multiple skills **simultaneously** `[FRS §9]`. Rules:

- Association creation/removal by owner
- Unique `(userId, skillId)` — duplicate prevention
- No forced "primary" skill unless product requires *(open question)*
- Optional proficiency per association
- Adding an **inactive** skill is rejected; already-associated skills that later deactivate remain historically valid but stop appearing as selectable

## 8. Proficiency

FRS marks proficiency **optional** — **no fixed scale invented**. Design options kept open:

- Configurable proficiency levels (if product defines them)
- Free-form descriptive level
- No proficiency (MVP baseline)

*Configurable model is a **design proposal pending product input** — Beginner/Intermediate/Advanced not assumed.*

## 9. Authorization Boundary (critical)

```text
TalentSkill ≠ SystemRole ≠ Permission ≠ ProjectContributionRole
```

- Possessing `Director` grants **no** admin/judge/competition-management/room-admin rights
- System roles remain the only authorization mechanism `[BR-2]`
- Skills are **data** consumed for eligibility/discovery — never consulted for access control

## 10. Project Contribution Role Boundary

| Concept | Scope | Owner |
|---------|-------|-------|
| `TalentSkill` | "What can this person do?" (stable capability) | **this module** |
| `ProjectContributionRole` | "What did they contribute to *this* project/room?" (contextual) | Rooms/Collaboration module |

A user may *have* `Director` but *contribute to a project as* `Writer`; the two never collapse. This module **does not duplicate** contribution roles.

## 11. Competition Interaction

Competition consumes skills via **read contract**: a competition may associate eligible skill(s); eligibility evaluation reads `UserTalentSkill` — the **rule is owned by Competition**, this module only provides skill data. Submissions may carry skill context (Competition/Submission owned). Skills never determine judging/scoring/ranking/admin.

## 12. Profile & Discovery Interaction

- **Profile:** displays user's skills via read contract — Profile can't write `UserTalentSkill`
- **Discovery:** skills used for filtering/search/discovery categories — read-only; Discovery never mutates

## 13. Portfolio Interaction

Portfolio items may *reference* skills (per-item skill tagging, if product confirms) — the association lives in Portfolio; this module owns only the skill definition. Not in Talent Skills.

## 14. API Surface (conceptual — ADR-007)

| Endpoint | Purpose | Auth | Authz |
|----------|---------|------|-------|
| `GET /api/v1/skills` | List active skills | auth'd or public | none — public taxonomy |
| `POST /api/v1/admin/skills` | Create skill | auth'd | Admin/SuperAdmin |
| `PUT /api/v1/admin/skills/{id}` | Update skill | auth'd | Admin/SuperAdmin |
| `POST /api/v1/admin/skills/{id}/deactivate` | Deactivate | auth'd | Admin/SuperAdmin |
| `GET /api/v1/users/me/skills` | My skills | auth'd | owner |
| `POST /api/v1/users/me/skills` | Add skill | auth'd | owner |
| `DELETE /api/v1/users/me/skills/{skillId}` | Remove | auth'd | owner |
| `PUT /api/v1/users/me/skills/{skillId}` | Update proficiency | auth'd | owner |
| `GET /api/v1/users/{id}/skills` | Public user's skills | auth'd-or-public | visibility-gated |

Errors: RFC 9457 + codes (`SKILL_NOT_FOUND`, `SKILL_INACTIVE`, `SKILL_ALREADY_ASSOCIATED`, `SKILL_NAME_CONFLICT`, `SKILL_REFERENCED_CANNOT_DELETE`, `SKILL_FORBIDDEN`).

## 15. Data Ownership

**Owns:** `TalentSkill`, `UserTalentSkill`, `SkillProficiency` (optional-config). **References:** User (MODULE 01 id only), Profile/Discovery/Competition (read consumers), Portfolio (per-item tagging elsewhere). No shared-write tables.

## 16. Validation / Business Rules (FRS-traced)

- Multi-skill required `[§9]` — no artificial single-skill cap unless product sets one
- Skill/permission separation `[BR-2]`
- Configurable taxonomy — admin-managed
- Duplicate association prevented (unique constraint)
- Inactive skill → not selectable, existing associations preserved
- Referenced skills → deactivate, never hard-delete *(design recommendation)*
- No invented max-skill limits *(open question)*

## 17. Lifecycle

| Concept | States | Rule |
|---------|--------|------|
| `TalentSkill` | active → inactive (deactivated, still referenced) | never hard-delete while referenced |
| `UserTalentSkill` | active → removed | removal allowed; historical data (submissions/portfolio refs) unaffected |

## 18. Audit

Audit: taxonomy create/update/deactivate; admin taxonomy changes. User add/remove skills — low-value audit (product decision whether to log); analytics separate.

## 19. Security

Unauthorized taxonomy modification (admin-only + audit); unauthorized skill assignment (owner-only); **privilege-escalation via skills impossible by design** — skills are never authorization; enumeration of private skill data (visibility-gated); inactive-skill misuse (rejected at validation).

## 20. Testing (not implemented)

Unit: taxonomy CRUD, deactivate-vs-delete, duplicate prevention, proficiency validation. Integration: multi-skill association, eligibility read-contract, Profile/Discovery read-only consumption, inactive-skill behavior. Boundary: skill-possession grants no permission; skills never writeable by Profile/Discovery.

## 21. Open Questions (Product Owner)

1. Proficiency representation (configurable vs free-form vs none)
2. Maximum user skills — none invented
3. Taxonomy hierarchy / synonyms / tags — needed?
4. Primary/preferred skill flag — needed?
5. Skill endorsement / verification — future?
6. Skill visibility — user-controlled?
7. Deactivation vs deletion policy (recommendation made; policy open)

## 22. Traceability

- **FRS:** §9 multi-talent model + portfolio · §11 discovery/search · §16 eligibility context · BR-2 separation
- **ADRs:** ADR-001 boundary · ADR-003 PG · ADR-007 API/errors
- **MODULE 01:** system-role authorization consumed (not skills)
- **MODULE 02:** profile displays skills via read contract
