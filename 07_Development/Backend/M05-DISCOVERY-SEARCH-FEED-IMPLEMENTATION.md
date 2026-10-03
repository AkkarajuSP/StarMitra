# M05 — Discovery / Search / Feed — Implementation

**Slice:** fifth business implementation. **Status: complete.** `SEARCH ≠ DISCOVERY ≠ FEED ≠ RECOMMENDATION`.

## Ownership

M05 owns query/read surfaces only — zero authoritative entities duplicated. Reads hit **authoritative tables** (`user_profiles`, `talent_skills`, `user_talent_skills`, `media_assets`) directly — the physical schema already carries the FTS indexes (`search_vector` GIN + `display_name` pg_trgm on profiles; `idx_ma_visibility` on media). The generic projection tables (`discovery_projections`, `feed_projections`) exist in the schema but are **unused for MVP**: direct queries keep visibility/moderation fresh by construction — no stale-projection authz leak. They're rebuildable/readable later if scale demands.

## Contract surface (3 ops, unchanged)

| Op | Path | Auth | Returns |
|---|---|---|---|
| search | GET /search?q=&type=&cursor&limit | authenticated | SearchPage |
| discover | GET /discovery?skillId=&category=&cursor&limit | authenticated | SearchPage |
| getFeed | GET /feed?cursor&limit | authenticated | SearchPage |

Items are `object`-typed per contract: `{type: PROFILE|SKILL|MEDIA, id, ...fields}`.

## Search (explicit query)

- **PROFILE**: `search_vector @@ plainto_tsquery` OR `display_name ILIKE` → `ts_rank DESC, created_at DESC, id ASC` (deterministic). PUBLIC only.
- **SKILL**: `talent_skills` ILIKE, ACTIVE only.
- **MEDIA**: `original_filename ILIKE` (only searchable metadata field present — documented gap: no description/title field on media_assets), gated by VERIFIED+processed+PUBLIC+unrestricted.
- `type=ALL` merges. Offset cursor (`o:N`) — deterministic under fixed ordering; bounded ≤100/page.

## Discovery (browse)

`GET /discovery?skillId` joins `user_talent_skills` → PUBLIC profiles, recency-ordered, `ProfileRestrictionContract` filter applied at read (seam for M18). `category` param is contract-declared but has no model field — ignored, documented.

## Feed

Deterministic recency surface: PUBLIC+VERIFIED+processed+unrestricted `media_assets`, `created_at DESC, id DESC`, **keyset** cursor `(created_at,id)`. Follow/engagement boosting is a labeled **PROVISIONAL slot** pending M21's follow contract — no fake counters, no ML.

## Visibility & moderation at query time

All filters in SQL: `visibility_state='PUBLIC'`, `upload_state='VERIFIED'`, `processing_state IN (...)`, `moderation_state NOT IN ('REJECTED','RESTRICTED')`. Private/nonexistent/restricted → same empty result — enumeration-safe (IT-verified indistinguishable). Blocked-relationship filtering awaits M06's contract — seam documented.

## Ranking (documented)

| Surface | Order | Status |
|---|---|---|
| search profiles | ts_rank → recency → id | ADR-011 baseline |
| search skills | name → id | deterministic |
| search media | recency → id | deterministic |
| discovery | recency → id | deterministic |
| feed | recency → id | PROVISIONAL — engagement/follow weights pending M21 |

## Boundaries

No foreign repositories — `JdbcTemplate` reads + `ProfileRestrictionContract` (M18 seam). M02/M03/M04/M21 authority untouched. TalentSkill used as filter data, never authorization.

## Testing

`DiscoveryFlowIT` 7 on real PG17: FTS name+bio hit, private-leak indistinguishability, skill search, skill-browse, public-only browse, deterministic feed + visibility filter, media type filter. Suite: **98**.

## Known follow-ups

- M21 `FollowContract` → feed personalization + FOLLOWERS visibility on M02/M04 surfaces.
- M06 block list → exclusion in search/discovery.
- `media_assets` lacks title/description — richer content search needs an owning-module field (gap, not invented).
- Trending/autocomplete/faceting/recommendation — open product decisions, not built.
- `discovery_projections`/`feed_projections` adoption if query volume demands.
