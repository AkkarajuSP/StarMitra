# StarMitra — Cache Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Draft for review | **Decision status:** under review as **OD-10** in the [Decision Register](../ADR/ARCHITECTURE-DECISION-REGISTER.md)

## 1. Principle

PostgreSQL is the sole source of truth. **No distributed cache is required for MVP.** Caching exists at three layers, each with its own invalidation model — none authoritative:

| Layer | Serves | Mechanism |
|-------|--------|-----------|
| HTTP/CDN | Public discovery/feed responses, media delivery | Cache-Control, ETag, conditional GET |
| In-process (JVM) | Hot reference/config data | Spring cache abstraction (Caffeine-class — implementation choice) |
| Distributed | *Not in MVP* — future decision | Redis/Valkey-class, adoption triggers defined |

## 2. Authoritative vs Cacheable

**Never authoritative from cache:** votes, submissions, judge evaluations, scores, rankings, competition state, messages, audit records, security/session state `[BR-14][FRS §22][§30]`.

**Cacheable (in-process):** skill taxonomy, competition categories, published rubric versions, active competition metadata, configuration — all immutable or short-TTL + explicit invalidation.

**Never shared-cache:** private/user-specific responses (profiles, media metadata, submissions, judge data, moderation, authz decisions).

## 3. Competition Integrity

Live voting counts, score/ranking freshness, and state transitions always read authoritative state. Cached display snapshots only for published results `[FRS §22]`. Competition close/round transitions bypass cache.

## 4. Failure Model

Cache is always optional — every read degrades to PostgreSQL. In-process: TTL + explicit eviction; restart = lazy rebuild. No cache-owned writes at MVP.

## 5. Redis Adoption Triggers (future decisions)

1. Multi-instance deployment needing shared rate-limiting / WS fan-out (OD-09 deferred)
2. Measured leaderboard/hot-read load exceeding PG + read replicas
3. A concrete feature demanding shared state (sessions-at-scale, distributed locks)

None approved by this decision.
