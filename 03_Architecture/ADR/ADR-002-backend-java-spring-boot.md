# ADR-002 — Backend Technology: Java 17+ / Spring Boot 3.x

| Field | Content |
|-------|---------|
| ADR | ADR-002 |
| Register entry | OD-02 — Backend Technology |
| Status | **ACCEPTED** |
| Date | 2026-10-03 |
| Baseline refs | §5, §7 Application Architecture, §20 Decisions |
| Related | ADR-001 (monolith), ADR-003 (PostgreSQL), ADR-009 (realtime isolation) |

## Decision

The backend is **Java 17+ / Spring Boot 3.x**. Accepted platform stack:

```text
Java 17+
Spring Boot 3.x
PostgreSQL
Flyway
REST/OpenAPI
WebSocket capability
Docker
```

## Context

14+ interdependent domains; strict transactional integrity for the competition pipeline; config-driven engines (rubrics, rounds); real-time chat; media orchestration; 4 client surfaces.

## Alternatives Considered

- **Node.js + TypeScript + NestJS** — strong runner-up (realtime ergonomics, one-language stack); weaker on transaction/concurrency tooling and boundary enforcement.
- **Python + FastAPI** — fastest to write; weakest domain-boundary enforcement and realtime.

## Rationale

Best-in-class declarative transactions and locking for the voting→scoring→ranking→audit pipeline; most mature RBAC/security (supports `TalentSkill ≠ SystemRole` cleanly); **Spring Modulith** enforces module boundaries → strongest future-extraction story; deepest PostgreSQL tooling (HikariCP, Flyway, JSONB, Testcontainers).

## Guardrails / Constraints (binding)

1. Preserve ADR-001 modular monolith — strong internal boundaries via **Spring Modulith** or equivalent; the 15 domains are **not** 15 deployed services.
2. StarMitra Connect realtime is architecturally **isolated** from core domain logic; extraction deferred (see ADR-009).
3. **NOT automatically approved** — each requires its own decision: Redis, Kafka, RabbitMQ, Elasticsearch/OpenSearch, Kubernetes, Service Mesh.

## Consequences

Slower initial dev velocity and heavier runtime vs Node/Python — accepted for integrity/robustness; weakest realtime ergonomics mitigated by module isolation.

## Deferred Items / Future Triggers

Job-queue library choice (implementation); messaging extraction (triggered by scale).

## Open Items

Job-queue library selection; OTP/realtime implementation spikes.
