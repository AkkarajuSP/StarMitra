# StarMitra — Search & Discovery Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Accepted in principle | **Decision status:** **OD-11 ACCEPTED IN PRINCIPLE** — ADR-011 formalization pending ([Decision Register](../ADR/ARCHITECTURE-DECISION-REGISTER.md))

## 1. Scope — FRS vs Inference

`[FRS §11]` requires: talent search by name/skill, feed/discovery, browse/filter by skill/category; **advanced search is P1** `[FRS §35]`. Not FRS: autocomplete, typo tolerance, faceting, ML ranking, personalization — all inference/deferred.

## 2. MVP Approach

`[Proposed]` **PostgreSQL-native search** inside the D5 Discovery read-model:

- **Text:** `tsvector`/`tsquery` FTS + GIN indexes; `pg_trgm` for fuzzy/partial name matching
- **Filtering/sorting:** relational predicates + composite B-tree indexes (competition × category × skill × status)
- **Relevance:** `ts_rank` + engagement signals (likes/follows) — no ML
- **Indexing:** transactional — generated columns/indexes on the same tables; no sync pipeline, no broker

## 3. Search ≠ Discovery Feed

Feed is a browse surface driven by domain read-model queries/projections in D5 — **not** a recommendation engine. Future ranking/personalization = separate product decision.

## 4. Consistency & Privacy

Immediate consistency (same transaction/DB). Every search carries authz scoping: private profiles/media excluded, moderated/removed content filtered, room-internal content invisible to non-members, blocked users' content suppressed. **Search never widens visibility.**

## 4a. Conceptual Searchable Entities/Fields — documented, not implemented

| Entity | Searchable fields | Filters |
|--------|-------------------|---------|
| Talent/User | name, bio | skills, status, visibility |
| TalentSkill | name | — |
| Competition | title, description | category/skill, status, dates |
| Submission/Media | title, description, content type | competition, category, visibility, moderation status |
| Project/Creative Room | title, description | type, member scope |
| Feed content | title/caption | skill, content type, recency |

**Deterministic relevance (documented):** `ts_rank` → recency → engagement counts. Fixed ordering — no undocumented ranking formula. `[BR-2]` preserved: TalentSkill/ProjectContributionRole are searchable *data*, never authorization.

## 5. Limitations (stated honestly)

Basic relevance; no native faceting; limited typo tolerance; no managed synonyms. Acceptable at MVP scale.

## 6. Migration Triggers → Dedicated Engine

1. P1 "advanced search" requirements land `[FRS §35]`
2. Measured search latency/index-maintenance cost exceeds PG comfort
3. Autocomplete/faceted discovery become product priorities

Any adoption = separate decision; engine is a disposable projection rebuilt from PostgreSQL (system of record stays authoritative). Query abstraction in D5 keeps the swap localized.
