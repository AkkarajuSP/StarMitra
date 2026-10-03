# ADR-011 — Search: PostgreSQL-Native Search for MVP

| Field | Content |
|-------|---------|
| ADR | ADR-011 |
| Register entry | OD-11 — Search |
| Status | **ACCEPTED** |
| Date | 2026-10-03 |
| Baseline refs | §20 Decisions, SEARCH-ARCHITECTURE.md |
| Related | ADR-003 (PG capabilities), ADR-007 (filter conventions), ADR-001 (D5 module) |

## Decision

**PostgreSQL-native search** for MVP: `tsvector`/`tsquery` FTS + GIN indexes, `pg_trgm` fuzzy matching, relational filtering, deterministic relevance — transactional indexing inside the same database. Dedicated search infrastructure deferred.

## Context

`[FRS §11]` requires talent search by name/skill, feed/discovery, browse/filter; advanced search is **P1** `[FRS §35]`. Elasticsearch/OpenSearch/Algolia were not pre-approved.

## Alternatives Considered

- **Dedicated engine (ES/OS-class)** — rejected at MVP: new infra + index-sync pipeline + authz-in-index complexity for capabilities not yet required.
- **Managed search (Algolia-class)** — rejected: vendor cost for unneeded capability.

## Rationale

Covers all FRS-supported needs (filter-browse + name/skill lookups); zero new infra; immediate consistency (same transaction/DB); authz/moderation scoping is trivially enforced in-query; clean migration path behind a query abstraction.

## Guardrails / Constraints (binding)

1. PostgreSQL remains authoritative.
2. PostgreSQL FTS (`tsvector`/`tsquery` + GIN) and `pg_trgm` are used only where justified.
3. Search must remain distinct from discovery/feed/recommendation.
4. Search must enforce authorization/privacy/moderation/visibility rules.
5. TalentSkill and ProjectContributionRole must never be treated as authorization mechanisms.
6. Search relevance must be **deterministic and documented** — no undocumented ranking formula (`ts_rank` → recency → engagement).
7. Advanced autocomplete, faceting, semantic/AI search and ML ranking remain deferred unless explicitly required by product scope.
8. Elasticsearch, OpenSearch, Algolia, Redis, Kafka, RabbitMQ and a dedicated search service remain **unapproved** for MVP.
9. Explicit migration triggers for a dedicated engine are documented.
10. Conceptual searchable entities/fields are documented — no implementation.

## Migration Triggers (future decisions)

1. P1 "advanced search" requirements land `[FRS §35]` exceeding PG capability.
2. Measured search latency/index-maintenance cost exceeds PG comfort.
3. Autocomplete/faceted discovery become product priorities.

## Consequences

Basic relevance/limited typo tolerance accepted at MVP; dedicated engine becomes a disposable projection if adopted later (PG stays authoritative).

## Open Items

- P1 advanced-search scope — product input
- Autocomplete at launch — product input
