# M08 — Portfolio — Implementation

**Slice:** seventh business implementation. **Status: complete.**

## Ownership

`portfolios` (one per user — DB-04, lazy-init like profiles), `portfolio_items`, `portfolio_item_media`, `portfolio_item_contributions`. Cross-module refs are UUID-only by design: skill → M03, media → M04, project_credit → M07.

## Contract surface (9 ops, unchanged)

| Op | Path | Auth |
|---|---|---|
| getMyPortfolio | GET /portfolios/me | own (lazy-init) |
| updateMyPortfolio | PUT /portfolios/me +If-Match | own |
| listPortfolioItems | GET /portfolios/me/items | own |
| createPortfolioItem | POST /portfolios/me/items | own |
| updatePortfolioItem | PUT /portfolios/me/items/{id} +If-Match | own |
| deletePortfolioItem | DELETE /portfolios/me/items/{id} | own |
| linkPortfolioMedia | POST …/items/{id}/media | own |
| linkProjectCredit | POST …/items/{id}/contributions | own |
| getPublicPortfolio | GET /portfolios/{userId} | any auth |

## Credit model (the important rule)

Portfolio `skill_id` is a **display tag** (validated ACTIVE via `SkillTaxonomyContract`); actual contribution is `portfolio_item_contributions` — links to M07 `project_credits` only. **Credits are never manufactured**: linking stores a reference; M07 verification is the documented seam. Multi-talent preserved — item's skill is per-item, no primary-skill assumption.

## Cross-module contracts

| Direction | Contract |
|---|---|
| M08 → M03 | `SkillTaxonomyContract.isActiveSkill` (new) |
| M08 → M04 | `MediaReferenceContract.isUsableBy` (media must be owned+VERIFIED) |
| M21 → M08 | `PortfolioTargetContract.isEngageableItem` (new — fills M21's PORTFOLIO validation slot: PUBLIC+ACTIVE or owner) |

## Behavior

- ETag/`If-Match` on portfolio + item updates (updated_at-derived, `CONFLICT_VERSION` on stale).
- Foreign item ops → NOT_FOUND (IDOR-safe).
- Public portfolio: PUBLIC items only; missing → NOT_FOUND.
- Media link: usable-media check; dedup via composite PK.
- Credit link: `uq_pic_item_credit` → CONFLICT on dup.
- Audit: `PORTFOLIO_ITEM_CREATED/DELETED`, `PROJECT_CREDIT_LINKED`.

## Testing

`PortfolioFlowIT` 7 on real PG17: lazy-init+single-portfolio, ETag lifecycle+stale-reject, item CRUD+foreign-IDOR, bad-skill reject + real skill + unusable-media reject + real media link, public-filter, credit-link store+dup-CONFLICT, M21 PORTFOLIO target validation end-to-end. Suite: **120**.

## Known follow-ups

- M07 `ProjectCredit` — verification of linked credits (contract seam ready).
- Item `status` values beyond ACTIVE (archive semantics — open product decision).
- `FOLLOWERS`/`COLLABORATION_ONLY` item visibility → M21 `isFollowing` wiring.
