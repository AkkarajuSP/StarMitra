# ADR-003 — Primary Database: PostgreSQL

| Field | Content |
|-------|---------|
| ADR | ADR-003 |
| Register entry | OD-03 — Primary Database |
| Status | **ACCEPTED** |
| Date | 2026-10-03 |
| Baseline refs | §11 Data Architecture, §20 Decisions, DATA-ARCHITECTURE.md |
| Related | ADR-002 (stack), ADR-007 (idempotency), ADR-010 (cache), ADR-011 (search), ADR-013 (analytics) |

## Decision

**PostgreSQL** is StarMitra's primary transactional relational datastore — the system of record for all domain data.

## Context

FRS §31 logical model is inherently relational (M:N spine); strict consistency for votes/evaluations/scores `[FRS §18–24][§30][§36]`; config-driven entities (rubrics, rounds); reporting `[§29]`; backup/recovery `[§36]`.

## Alternatives Considered

- **MySQL/MariaDB** — credible but weaker on JSONB config modeling, partial indexes, window functions for ranking, FTS.
- **SQL Server** — technically capable; licensing unjustified for a startup.
- **Distributed SQL** — premature (no scale need).
- **Document DB (MongoDB-class)** — rejected: wrong shape for the relational FRS model; integrity pushed into app code.

## Rationale

Exact relational fit; strongest constraint/transaction toolkit for the competition pipeline; JSONB covers config-driven entities without a second store; FTS seeds search (ADR-011); free/OSS with widest managed-service availability; seamless in the Spring Boot stack.

## Guardrails / Constraints (binding)

1. Core business entities remain **relational and strongly typed**.
2. **JSONB selectively** — only for genuinely configuration-driven/flexible structures; never as a substitute for typed core entities.
3. Prefer **constraints and transactions** for invariant enforcement.
4. **Row-level locking** where contention requires it.
5. **Advisory locks are NOT a blanket requirement** — only where a specific concurrency design justifies them.
6. **Partitioning, replicas, CDC, multi-region** remain future decisions unless separately approved.
7. This decision does **NOT** approve Redis, Kafka, RabbitMQ, Elasticsearch/OpenSearch, Kubernetes, or Service Mesh.

## Consequences

Single system of record inside the monolith; module-owned tables/schemas enforce ADR-001 boundaries; reporting contention managed via rollups/replica trigger.

## Deferred Items / Future Triggers

Read replicas (measured reporting contention), partitioning (table-volume growth), CDC/multi-region (future scale) — each a separate decision.

## Open Items

None — fully decided.
