# ADR-010 — Cache: No Distributed Cache for MVP

| Field | Content |
|-------|---------|
| ADR | ADR-010 |
| Register entry | OD-10 — Cache |
| Status | **ACCEPTED** |
| Date | 2026-10-03 |
| Baseline refs | §20 Decisions, CACHE-ARCHITECTURE.md |
| Related | ADR-003 (PG authoritative), ADR-009 (WS fan-out trigger), ADR-012 (scaling triggers) |

## Decision

**No distributed cache for MVP.** Caching exists only as: in-process caching for hot reference/config data, HTTP caching, and CDN caching — never as an authoritative store. **Redis/Valkey is deferred and not approved.**

## Context

FRS demands correctness for votes/evaluations/scores `[BR-14][FRS §22]` — caching must never corrupt authoritative state. No FRS requirement mandates sub-ms reads or shared caching; ADR-009 did not approve Redis.

## Alternatives Considered

- **Redis/Valkey from day one** — rejected: overkill at MVP scale; new failure mode; premature.
- **Managed cache** — same + vendor cost.

## Rationale

PostgreSQL + indexes + JVM in-process cache + HTTP/CDN layers cover every FRS read path; single-instance monolith has no shared-coordination need; zero added infra.

## Guardrails / Constraints (binding)

1. No distributed cache is required for MVP.
2. PostgreSQL remains the authoritative source of truth.
3. In-process caching may be used selectively for hot, relatively stable reference/configuration data.
4. HTTP caching may be used where appropriate.
5. CDN caching may be used for appropriate public media/content.
6. Cache is an optimization and never an authoritative data store.
7. Votes, submissions, evaluations, scores, rankings, competition state, messages and audit records remain authoritative in PostgreSQL.
8. Authorization decisions must not rely on a shared cache as the authority.
9. Cached display data must never determine official competition results.
10. Cache failure must degrade safely to PostgreSQL wherever possible.
11. Cache invalidation remains simple for MVP.
12. Redis/Valkey is deferred and is **NOT approved**.
13. Multi-instance WebSocket fan-out may become a future distributed-cache/coordination trigger.
14. Measured hot-read performance may become a future cache trigger.
15. Any future distributed-cache adoption requires a separate architecture decision.
16. Cache technology/library selection is not part of this decision's implementation.

## Consequences

Zero extra infra; DB carries all read load (manageable at MVP); clean invalidation surface; clear adoption triggers.

## Deferred Items / Future Triggers

Redis/distributed cache adoption triggers: (1) multi-instance rate-limiting / WS fan-out coordination, (2) measured hot-read/leaderboard load, (3) concrete shared-state feature — each a separate decision.

## Open Items

None — fully decided.
