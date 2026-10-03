# M03 — Talent Skills — Implementation

**Slice:** third business implementation. **Status: complete.**

## Ownership

M03 owns `talent_skills`, `user_talent_skills`, `skill_proficiencies` — nothing else. **Critical invariant:** TalentSkill is descriptive profile data, NEVER authorization. The only authz in this module is `ROLE_ADMIN` on taxonomy mutation; reads are authenticated-user.

## Contract surface (7 ops, unchanged)

| Operation | Path | Auth |
|---|---|---|
| listSkills | GET /skills (cursor+limit → SkillPage) | authenticated |
| createSkill | POST /skills | ADMIN |
| updateSkill | PUT /skills/{id} | ADMIN |
| deactivateSkill | POST /skills/{id}/deactivate → 204 | ADMIN |
| getMySkills | GET /users/me/skills → UserSkill[] | authenticated |
| addMySkill | PUT /users/me/skills/{skillId} → 200 | own (JWT sub) |
| removeMySkill | DELETE /users/me/skills/{skillId} → 204 | own (JWT sub) |

## Taxonomy

`talent_skills`: name (UQ), description, ACTIVE/INACTIVE, parent_skill_id (self-FK), display_order. Configurable via admin ops — not seeded by migration (V1.20 seeds only roles+proficiencies). FRS's 13-name list is the intended initial content; admin populates via `createSkill`. Duplicate name → `CONFLICT` (UQ first line). Parent must exist + be ACTIVE.

## UserTalentSkill

Composite PK `(user_id, skill_id)` = duplicate-proof by construction. `addMySkill` = PUT-upsert (create or update proficiency — idempotent; concurrent inserts converge on PK). `removeMySkill` = idempotent delete (missing → no-op 204). Skill must exist + ACTIVE else `NOT_FOUND`. Owner always JWT `sub` — no client-supplied owner → IDOR-impossible.

## Proficiency

Optional (`proficiency_id` nullable). 4 seeded codes (BEGINNER/INTERMEDIATE/ADVANCED/PROFESSIONAL, ordinals 1–4) — the baseline-proposed scale, configurable. Unknown code → `VALIDATION_FAILED`; null → cleared.

## Multi-talent

✅ many skills per user, no cap, no primary designation — verified by IT adding Singer/Actor/Director to one user.

## M02 integration

`UserSkillReadService implements UserSkillReadContract` (real impl — replaced `EmptyUserSkillReadContract`): joins `user_talent_skills → talent_skills → skill_proficiencies`, ACTIVE skills only. `GET /profiles/me` and `/profiles/{id}` now return real `skills[]`. M02 never touches M03 persistence (ArchUnit-verified).

## Future consumers (contracts only, nothing built)

M05 search/feed, M09 eligibility, M07 roles, M12 judge expertise, M08 portfolio — all consume via `UserSkillReadContract`; M03 provides identity/reads, never eligibility logic.

## Lifecycle

Deactivate ≠ delete: `INACTIVE` preserves historical refs (projects/competitions/judges may reference). Deactivated skills disappear from `listSkills` and association reads; re-add rejected with `NOT_FOUND`.

## Audit

`SKILL_CREATED`, `SKILL_DEACTIVATED`, `USER_SKILL_ADDED`, `USER_SKILL_REMOVED` via kernel audit.

## Testing

`SkillServiceTest` 10 unit (role gates incl. JUDGE≠admin-proof, inactive rejects, idempotent upsert, bad proficiency, no-op delete) · `SkillFlowIT` 7 real-PG (taxonomy lifecycle, name-UQ CONFLICT, multi-talent + M02 contract end-to-end, idempotent PUT/DELETE, inactive reject, FK+seed checks). Suite total: 76.

## Known follow-ups

- FRS 13-skill taxonomy is API-populated at deploy (admin ops exist; no migration seed — per "configurable, not seeded" baseline).
- `updateSkill` doesn't propagate name changes to cached/joined views — acceptable (single-source join reads).
