# Module Design — 08: Portfolio

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 08)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–07 without modification.

## 1. Purpose

Own the user's **creative-work presentation** — a showcase of work items and verified project contributions across the multi-talent model `[FRS §9]`. Portfolio is presentation + verified references; it never owns skills, media, or contribution records.

## 2. Core Product Model — separation preserved

| Concept | Owner | Meaning |
|---------|-------|---------|
| `TalentSkill` | MODULE 03 | capability — what the user *can do* |
| `UserTalentSkill` | MODULE 03 | user↔skill association |
| `ProjectContributionRole` | MODULE 07 | what the user *did* in a specific room |
| `PortfolioItem` | **this module** | a showcase entry |
| `MediaAsset` | MODULE 04 | the actual media |

A portfolio item may *reference* a skill, a contribution, or media — it owns none of them. **Skill possession never fabricates a project contribution.**

## 3. FRS Scope `[FRS §9]` — traced

Portfolio per user · media/work showcase · talent-specific work · **verified project contribution credits** · presentation for discovery. No invented features.

## 4. Module Boundary

| Owns | Does NOT own |
|------|--------------|
| `Portfolio`, `PortfolioItem`, `PortfolioItemMedia` (ref), `PortfolioItemContribution` (verified ref), presentation metadata, item ordering | User (M01), skills (M03), MediaAsset (M04), project contributions (M07), Competition/Submission (future), moderation policy, Discovery ranking |

## 5. Portfolio Ownership

**One portfolio per user** *(design proposal — FRS describes portfolio per user; multiplicity open)*. Auto-created at registration alongside profile. Lifecycle: active → restricted (moderation) → deactivated-with-account. Visibility + metadata owned here.

## 6. Portfolio Items

`PortfolioItem`: id, portfolioId, title, description, **item-type** *(configurable/open taxonomy — not fixed)*, visibility, skill refs (multi), contribution ref *(optional)*, media refs, ordering, timestamps. Cardinality of skills-per-item **OPEN** — design supports multi-skill tagging.

## 7. Multi-Talent Support — critical

Portfolio represents work across skills: Item A→Singer, B→Actor, C→Director, D→Writer — all in one portfolio. **No primary-talent dependency; no single-skill limit.** Skills are display/filter refs, never authorization.

## 8. Media Integration (MODULE 04 / ADR-008)

`PortfolioItemMedia`: itemId + mediaId + ordering + optional featured flag *(open/proposal)*. Binary/upload/processing/signed-access stay in MODULE 04. Item visibility never widens media access — a private `MediaAsset` can't be exposed by a public item.

## 9. Project Credits — cross-module contract

Portfolio **consumes** `ProjectCredit`/`ProjectContributionRole` from MODULE 07 via read contract:

```text
CreativeRoom (authoritative) → ProjectCredit {userId, roomId, contributionRole}
        ↓ read-only reference
PortfolioItem { type=project-credit, contributionRef, displayMetadata }
```

- Portfolio displays the *contribution as recorded by the project* — never independently asserts it
- If the project record changes (role removed/finalized), the displayed ref tracks the authoritative source
- No `ProjectContributionRole` duplication inside Portfolio

## 10. Competition Integration — future only

Competition/Submission not yet designed. **Future integration point flagged:** portfolio may later surface verified achievements/participation — but "ranking becomes portfolio item", "every submission shows", "results auto-display" are **all open product decisions, not assumed.**

## 11. Visibility

`Public / Private / Restricted` *(options — taxonomy open)*. **Visibility ≠ authorization:** never overrides account restriction, moderation, media access, or private-project rules. Public item + private media = still private.

## 12. Discovery Integration (MODULE 05)

Portfolio emits signals for discovery refresh; Discovery reads *eligible* items via read contract + projects/ranks them. Discovery never authoritative for portfolio data; no separate search engine.

## 13. Profile Integration (MODULE 02)

Profile shows portfolio *summary* via read contract; Portfolio owns content. No data duplication.

## 14. Moderation

Report/restriction state consumed + enforced on display; policy owned by Moderation. Covered: reported item, restricted item, removed media, user restriction, discovery suppression — no invented workflow.

## 15. Verification Model

Three item kinds *(conceptual — states open)*:

1. **User-created showcase** — self-authored, no verification needed
2. **Project-backed** — references verified `ProjectCredit` from MODULE 07
3. **Future competition-backed** — deferred

An unverified item can never *pose as* authoritative project contribution — the reference type distinguishes them.

## 16. Data Model

| Entity | Purpose | Notes |
|--------|---------|-------|
| `Portfolio` | container | userId unique (proposal); visibility, status |
| `PortfolioItem` | showcase entry | type, visibility, ordering, skill refs |
| `PortfolioItemMedia` | media link | itemId+mediaId+ordering |
| `PortfolioItemContribution` | verified ref | itemId+contributionRef (→M07), display fields |

Authoritative: items + links. Referenced: skills, media, contributions. Derived: discovery projections.

## 17. API Surface (conceptual — ADR-007)

| Endpoint | Purpose | Authz |
|----------|---------|-------|
| `GET /api/v1/portfolios/me` | own portfolio | owner |
| `PUT /api/v1/portfolios/me` | update/visibility | owner |
| `GET /api/v1/portfolios/{id}` | public view | visibility-gated |
| `POST/PUT/DELETE /api/v1/portfolios/me/items` | item CRUD | owner |
| `PUT /api/v1/portfolios/me/items/{id}/media` | attach media ref | owner + media-owned |
| `POST /api/v1/portfolios/me/items/{id}/contributions` | link verified credit | owner + must exist in M07 |

## 18. Concurrency / Consistency

Portfolio/item edits — optimistic-lock + tx; duplicate-item prevention; ordering updates transactional; stale contribution-ref handled via M07 source-of-truth reads (never cached-authoritative); visibility changes atomic. PG only.

## 19. Security

Cross-user modification impossible (owner-scoped writes); IDOR; contribution spoofing (must exist + belong to user in M07 — can't manufacture); media URL leakage (signed-URL + visibility); private-item leakage; moderation bypass. **A user cannot create an authoritative project contribution through Portfolio APIs.**

## 20. Performance

Single-item load + cursor pagination for lists; media refs resolved via Media access API; discovery refresh on item events. No ES/Redis/Kafka/separate-service.

## 21. Open Decisions

Portfolio cardinality (1/user proposed), visibility taxonomy, item-type taxonomy, skills-per-item cardinality, featured/ordering model, verification states, user-vs-project item display, stale/deleted project behavior, competition integration, deletion/archival, media retention, discovery freshness.

## 22. Acceptance Validation

Portfolio owns items only ✓ · M02 doesn't own portfolio ✓ · M03 doesn't own items ✓ · M04 owns media ✓ · M07 owns contributions ✓ · multi-talent supported ✓ · Discovery derived ✓ · visibility ≠ authz ✓ · no competition impl ✓ · PG authoritative ✓ · no infra ✓ · no implementation ✓

## 23. Traceability

- **FRS:** §9 portfolio · §11 discovery · §26 moderation
- **ADRs:** ADR-001 boundary · ADR-003 PG · ADR-007 API · ADR-008 media
- **Modules:** M01 authz · M02 profile-display · M03 skills · M04 media · M05 discovery · M07 credits
