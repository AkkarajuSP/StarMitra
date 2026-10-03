# M02 — User Profile — Implementation

**Slice:** second business implementation. **Status: complete.**

## Ownership

M02 owns `user_profiles` only — displayName, bio, location, avatar/banner media **references**, visibility_state, completion_score. It does NOT own identity (M01), skills (M03), media assets (M04), restriction decisions (M18), followers (M21), portfolio (M08).

## Contract surface (3 ops, unchanged)

| Operation | Path | Auth |
|---|---|---|
| getMyProfile | GET /profiles/me | authenticated |
| updateMyProfile | PUT /profiles/me (+If-Match) | authenticated, own |
| getPublicProfile | GET /profiles/{userId} | authenticated, visibility-filtered |

## Lifecycle

No POST exists in the contract → **lazy initialization**: first `GET/PUT /profiles/me` materializes a profile (default `PRIVATE`, displayName = email local-part); `uq_user_profiles_user` converges concurrent first access. States: Created→Incomplete→Active are implicit (completion_score) — no status enum exists in the schema; `RESTRICTED` comes from M18, not this table.

## Visibility model (visibility ≠ authorization)

`getPublicProfile` is enumeration-safe — the same `NOT_FOUND` covers: nonexistent user, PRIVATE, FOLLOWERS*, COLLABORATION_ONLY*, M18-restricted. *FOLLOWERS/COLLABORATION_ONLY require M21/M06 reads that don't exist yet — treated as non-public until those modules land (documented in Known Follow-ups, not a deviation). Owner always sees own profile.

## Concurrency

Schema has no `version` column → **ETag = `"epoch-second.nano"` derived from `updated_at`**; `If-Match` mismatch → `CONFLICT_VERSION`; missing If-Match → last-writer-wins (contract makes header optional).

## Cross-module boundaries

| Needs | Via |
|---|---|
| identity (M01) | `AuthService.identityOf` application contract |
| skills in profile response (M03) | `UserSkillReadContract` — `EmptyUserSkillReadContract` returns `[]` until M03 |
| moderation restriction (M18) | `ProfileRestrictionContract` — `NoOpProfileRestrictionContract` until M18 |
| avatar/banner (M04) | UUID reference only, no FK, no binary handling |

No foreign repository is touched anywhere — ArchUnit still green.

## Security

Own-profile authz from JWT `sub` (never client-supplied owner) · IDOR impossible (no mutable-by-id endpoint; `/profiles/{id}` is read-only and visibility-gated) · displayName/bio validated · skills can't be mutated via M02 · profile update can't touch M01 state · audit events `PROFILE_CREATED`/`PROFILE_UPDATED`.

## Testing

`ProfileServiceTest` 9 (lazy-init, private→404, restricted→404, own-private visible, stale/matching/absent ETag, bad visibility) · `ProfileFlowIT` 6 on real PG17 (persistence, UQ-one-profile, update+etag rotation, stale→CONFLICT_VERSION, visibility lifecycle, audit). Total suite 59.

## Known follow-ups

- M03 slice: replace `EmptyUserSkillReadContract` (skills array populates).
- M18 slice: real `ProfileRestrictionContract` impl.
- M21/M06: FOLLOWERS/COLLABORATION_ONLY visibility semantics.
- completion_score computation policy (schema column exists; calculation rules are product-open — left unset, documented).
