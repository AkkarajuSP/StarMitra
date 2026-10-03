# Module Design — 05: Discovery / Search / Feed

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 05)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–04 without modification.

## 1. Purpose

Provide the platform's **read/discovery surfaces**: user-initiated search, system-presented discovery, and the feed read-model — three distinct capabilities sharing PG-native backing and identical visibility/authz scoping.

## 2. Critical Domain Distinction (per your framing — adopted verbatim)

| Capability | Definition | Mechanism | MVP |
|-----------|------------|-----------|-----|
| **Search** | User explicitly asks to find something ("Find actors in Hyderabad") | Query + matching/filtering | ✅ PG-native (ADR-011) |
| **Discovery** | Platform presents content/entities for exploration | Read-model queries + deterministic ordering | ✅ FRS `[§11]` |
| **Feed** | Discovery's presentation/read-model experience | Cursor-paginated derived view | ✅ FRS `[§11]` |
| **Recommendation** | Personalized predictive ranking ("you'll probably like…") | ML/predictive | ❌ Deferred — future capability |

**Discovery is not a recommendation engine.** Three surfaces, one module, one authz model.

## 3. Responsibilities

- Search orchestration (query parsing, filter composition, result assembly)
- Discovery queries + **read-model projections** (derived display data)
- Feed composition + deterministic ordering
- Trending aggregation *(if FRS-supported — scoped, not invented)*
- Filters, pagination, discovery-visibility rules
- Search/discovery input validation + abuse handling

## 4. Non-Responsibilities

Does **not** own: User, UserProfile, TalentSkill, MediaAsset, Competition, Submission, Vote, JudgeEvaluation, ProjectContributionRole, moderation *decisions*, follow relationships (consumed). **Consumes published/readable data from those domains — never writes to them.**

## 5. ADR-011 Compliance (preserved verbatim)

PostgreSQL-native: `tsvector`/`tsquery` FTS + GIN, `pg_trgm` bounded trigram, relational filters, **deterministic documented relevance**. No ES/OS/Algolia/dedicated-service/Redis/Kafka/RabbitMQ. Transactional indexing on same DB — no sync pipeline.

## 6. FRS Discovery Scope `[FRS §11]` — traced

| Surface | FRS | Mechanism |
|---------|-----|-----------|
| Talent/creator search by name/skill | ✓ | FTS + trigram + skill filter |
| Discovery/browse by skill/category | ✓ | relational filters + read model |
| Content discovery | ✓ | media-backed read model |
| Competition discovery | ✓ | filters + status gating |
| Project/room discovery | *inference — open* | visibility-gated browse |
| Trending | ✓ `[§11]` | deterministic aggregate *(formula open — §10)* |
| Follows as feed signal | ✓ `[§11]` | consumed from Follow ownership (see §13) |
| Personalized recommendations | **not FRS** | deferred |

## 7. Search Model

- **Keyword:** `tsvector`/`tsquery` on documented fields
- **Filters:** skill, category, entity type, status, visibility — relational predicates
- **Matching:** exact (IDs/filters), FTS (text), `pg_trgm` (bounded fuzzy/typo on names)
- **Pagination:** cursor/keyset (ADR-007)
- No invented semantics (semantic search, fuzzy-beyond-trigram, learned ranking).

## 8. Search Relevance — deterministic + documented

Combined as: **`ts_rank` (text relevance) → recency → engagement** — fixed ordering, documented, reproducible. No ML/semantic/opaque-personalized ranking — each requires a future decision.

## 9. Deferred Advanced Capabilities

Autocomplete, advanced faceting, semantic/AI search, ML ranking — **all deferred** per ADR-011 guardrail 7. `pg_trgm` provides bounded typo tolerance only; true autocomplete is a product decision, not auto-included.

## 10. Discovery Feed — read model

```text
Operational domain data (profiles, media, competitions, follows)
        ↓ derivation (read-model projection — refreshable)
Derived feed read model
        ↓ deterministic ordering + visibility/moderation filtering
Feed queries (cursor-paginated)
```

- Feed candidates: published content from followed creators, relevant skills/categories, active competitions — scope = FRS discovery surface
- **Read model is derived + refreshable — never authoritative**; PG stays source of truth
- Consistency: transactional derivation preferred at MVP; refresh lag acceptable per product SLA *(open)*

## 11. Feed Ranking — deterministic proposal

Signals *(design proposal — exact weights are open product decisions)*: recency + engagement + follow-relationship + skill/category relevance + competition/project relevance. **No opaque scoring formula** — weights stay open until product approves.

## 12. Trending

`[FRS §11]` requires trending. **Conceptual definition only — no numerical formula invented.** Candidate signals (views/engagement over a window), window length, aggregation method, freshness, abuse handling = **open design/product questions** flagged for decision.

## 13. Visibility / Authorization — critical

Search/discovery **never exposes**: private profiles, private media, blocked-user content, moderation-restricted content, unpublished competitions, private Creative Rooms, unauthorized project content. Every query carries domain authz scoping. **Visibility ≠ authorization. TalentSkill is never an authorization mechanism.**

## 14. Moderation Boundary

Moderation *decides*; Discovery *consumes* the resulting published/visible state — restricted content excluded from search/feed/trending at query time. No policy duplication.

## 15. Following

Follow relationships are consumed as a feed signal but **not owned here** — Follow ownership lives in User Profile (relationship domain) per FRS; Discovery is a read consumer. No separate Follow module, no social-graph architecture invented.

## 16. Read Models

`Operational data → derived projection → feed/discovery queries`. Principles: PG-authoritative; projections refreshable/rebuildable; no analytics datastore; transactional consistency preferred at MVP; async refresh only if measured need (OD-12 trigger).

## 17. Pagination

Cursor/keyset for search/feed/discovery (unbounded); offset only for bounded/admin surfaces — per ADR-007.

## 18. API Surface (conceptual — ADR-007)

| Endpoint | Purpose | Auth | Notes |
|----------|---------|------|-------|
| `GET /api/v1/search` | Global keyword+filter search | auth'd-or-public | visibility-gated |
| `GET /api/v1/discovery/talents` | Creator discovery | auth'd | filters: skill, category, location* |
| `GET /api/v1/discovery/competitions` | Competition discovery | auth'd-or-public | status-gated |
| `GET /api/v1/discovery/projects` | Project/room discovery | auth'd | visibility-gated *(open scope)* |
| `GET /api/v1/discovery/content` | Content/media discovery | auth'd-or-public | published only |
| `GET /api/v1/feed` | Personalized-ish feed (deterministic) | auth'd | cursor-paginated |
| `GET /api/v1/discovery/trending` | Trending surface | auth'd-or-public | deterministic aggregate |

Errors: RFC 9457 + codes (`SEARCH_QUERY_INVALID`, `SEARCH_RATE_LIMITED`, `DISCOVERY_FORBIDDEN`, `FEED_EMPTY_OK`) + correlation ID.

## 19. Performance

Indexed filters + GIN-FTS adequate at MVP volume; read-model projections for feed; cursor pagination everywhere; no cache authority (ADR-010). **Migration triggers:** measured p95 latency, index-maintenance cost, autocomplete/facet product need → dedicated-engine decision (ADR-011 §4).

## 20. Security / Abuse

Enumeration (uniform errors + visibility gating), private-content leakage (authz filters mandatory), blocked-user invisibility, scraping/rate-limiting *(thresholds = open config)*, excessive pagination (cursor + page caps), moderation bypass (state filtering enforced).

## 21. Cache (ADR-010)

No distributed cache; in-process only for hot reference data; CDN for public content where appropriate. Cache never authoritative for discovery/search state.

## 22. Analytics Boundary (ADR-013)

Discovery may contribute operational metrics but analytics doesn't determine search results or competition outcomes; AuditLog + observability separate; no event pipeline.

## 23. Testing (not implemented)

Search accuracy, filter correctness, authz/visibility matrix (incl. private/blocked/moderated exclusion), pagination, deterministic relevance/ordering, feed composition, trending aggregate, performance, abuse.

## 24. Open Questions (Product Owner)

1. Exact searchable fields; relevance weights; trending formula + window
2. Feed ranking rules + freshness SLA
3. Autocomplete; advanced faceting; typo-tolerance extent
4. Personalized recommendations — product appetite (deferred capability)
5. Search rate limits; read-model refresh strategy
6. Project/room discovery scope (FRS inference)

## 25. Traceability

- **FRS:** §11 discovery/feed/search · §9 visibility · §12 blocking · §26 moderation · §36 NFRs
- **ADRs:** ADR-007 API/pagination · ADR-010 no-distributed-cache · ADR-011 PG-search (primary) · ADR-013 analytics boundary
- **MODULE 01:** authz principal · **MODULE 02:** profile visibility · **MODULE 03:** skills as filters · **MODULE 04:** media visibility
