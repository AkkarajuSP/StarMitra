# StarMitra — Architecture Decision Register

**Version:** Draft v0.1 | **Parent:** [Architecture Baseline v1.0](../Architecture/STARMITRA-ARCHITECTURE-BASELINE-v1.0.md)
**Functional baseline:** StarMitra FRS v1.1 — Multi-Talent / Multi-Skill Model

## Purpose

Single working register for all major StarMitra architecture decisions (OD-01 … OD-13). Every entry presents genuine alternatives with real trade-offs — no decision is pre-decided.

**Numbering note:** OD-07 (API Architecture / API Contract Strategy) was inserted during review; the original media/realtime/cache/search/cloud/analytics sequence was renumbered to OD-08 … OD-13 to preserve review order. All cross-references updated.

## Status Convention

```text
PROPOSED — PENDING PRODUCT/TECHNICAL REVIEW   (initial state)
PROPOSED ACCEPTANCE — PENDING FINAL ADR APPROVAL  (review direction agreed; not yet accepted)
ACCEPTED   (only after review; then converted to a numbered ADR)
SUPERSEDED (later change replaces it)
```

## Review Flow

```text
Product Owner / Business Lead → User
Technical/Product Co-Pilot    → ChatGPT
Engineering                   → Devin
```

**ADR policy:** this register is the decision *candidate* list. When a decision is approved, an individual `ADR-NNN-<topic>.md` is created here with `Status: ACCEPTED`, and the baseline's Open Decisions table is updated. Nothing below is approved.

## Summary of Devin Recommendations

| ID | Topic | Recommendation | Status |
|----|-------|----------------|--------|
| OD-01 | Architecture style | Modular monolith | **PROPOSED ACCEPTANCE — PENDING FINAL ADR APPROVAL** |
| OD-02 | Backend technology | **Java 17+ / Spring Boot 3.x** | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| OD-03 | Primary database | **PostgreSQL** | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| OD-04 | Web frontend | **React + TypeScript + Vite SPA** (all four surfaces, route-group separation); SEO sub-decision open | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| OD-05 | Mobile technology | **React Native + TypeScript + Expo** — creator/audience scoped | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| OD-06 | AuthN/identity | **First-party Spring Security + OTP + JWT access / opaque refresh; unified across clients** | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| OD-07 | API architecture/contract | **REST + OpenAPI, `/api/v1` versioning, RFC 9457, cursor/offset pagination** | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| OD-08 | Media storage/processing | **Object storage + direct-to-storage upload + async processing + CDN — provider-neutral** | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| OD-09 | Real-time | **WebSocket inside Spring Boot monolith (isolated D6); protocol detail open; no broker** | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| OD-10 | Cache | **No distributed cache for MVP — in-process + HTTP/CDN only; Redis deferred w/ triggers** | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| OD-11 | Search | **PostgreSQL-native (FTS + trigram + relational filters); dedicated engine deferred w/ triggers** | **ACCEPTED IN PRINCIPLE — PENDING FINAL ADR FORMALIZATION** |
| OD-12 | Cloud/deployment | **Managed container platform + managed PG + object storage + CDN; no K8s for MVP; provider OPEN** | **ACCEPTED IN PRINCIPLE — PENDING FINAL ADR FORMALIZATION** |
| OD-13 | Analytics | **PostgreSQL-based operational reporting + read models; event model deferred; no warehouse/third-party** | PROPOSED — PENDING REVIEW |

---

## OD-01 — Architecture Style

| Field | Content |
|-------|---------|
| Decision ID | OD-01 |
| Decision | What application architecture style should StarMitra adopt for MVP and near-term? |
| Context | FRS defines ~15 interdependent domains; competition scoring chains submission→vote→evaluation→aggregation→ranking→qualification, demanding transactional consistency `[FRS §22][§30][§36]` |
| FRS References | §15–24 (competition pipeline), §30 (audit), §36 (consistency NFR) |
| Options | **A. Modular monolith** — one deployable, internal modules D1–D15 with strict boundaries. **B. Microservices** — per-domain deployables. **C. Hybrid** — monolith core + 1–2 satellite services (e.g., media worker, realtime messaging). |
| Advantages | **A:** single transaction boundary for scoring/voting/audit; fastest MVP; trivial local dev; no distributed-systems overhead. **B:** independent scaling/deployment; hard boundaries; team-parallel autonomy. **C:** monolith simplicity for core + independent scaling only where proven needed. |
| Disadvantages | **A:** boundary discipline relies on convention/review; one bad deploy affects all; can't scale domains independently. **B:** distributed transactions for scoring pipeline (sagas/outbox) — high complexity; network failure modes; more infra; slower MVP; debugging cost. **C:** still inherits most of A's constraints; requires discipline about what extracts. |
| StarMitra Fit | **A strong:** FRS's heavy transactional coupling and small initial team favor it. **B weak for MVP:** no demonstrated scale or team-parallel need yet; scoring pipeline resists distribution. **C credible:** media processing is the natural extraction candidate (CPU-bound, async, independent scaling). |
| Team Impact | A/C: one codebase, one deploy — minimal ops. B: requires distributed-systems expertise, service mesh/orchestration maturity, on-call per service. |
| Cost/Complexity | A lowest; C low-moderate; B highest (infra + ops + dev tooling). |
| Risks | A: boundary erosion → big-ball-of-mud (mitigate: module lint rules, no cross-module table access, review). B: premature distribution → missed MVP, integrity bugs across services. C: extraction never happens / happens messily. |
| Devin Recommendation | **A — Modular monolith** for MVP, designed for C: module boundaries drawn so media worker/messaging can extract to satellite services when load or team size justifies. Consistent with baseline §5. |
| Status | **PROPOSED ACCEPTANCE — PENDING FINAL ADR APPROVAL** |
| Decision Owner | Product + Technical Review |

**Review direction (first review, recorded verbatim):** StarMitra will use a modular monolith for MVP with explicit bounded domain/module boundaries so individual domains can be extracted into independently deployable services later if actual scale, reliability, organizational, or domain requirements justify it. The 15-domain structure from the Architecture Baseline is maintained.

This decision does NOT mean: one large unstructured codebase · shared unrestricted database access between modules · no domain boundaries · no asynchronous processing · no future microservices. Strong internal boundaries are mandatory. Final `ADR-001` will be created after the full decision review completes.

## OD-02 — Backend Technology

| Field | Content |
|-------|---------|
| Decision ID | OD-02 |
| Decision | Backend language/framework for the StarMitra API (assuming OD-01 modular monolith). |
| Context | 14+ interdependent domains, config-driven engines (rubrics, rounds, scoring weights), strict transactional integrity for the competition pipeline (submission → vote → evaluation → scoring → ranking → qualification → audit), real-time chat, media orchestration, 4 client surfaces. |
| FRS References | §6 roles, §10 media, §12 Connect, §15–24 competition pipeline, §30 audit, §36 NFRs, §38 guidance |
| Options | **A. Java 17+ / Spring Boot 3.x** · **B. Node.js + TypeScript + NestJS** · **C. Python + FastAPI** *(option set constrained by review; prior wider list superseded)* |
| Status | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| Decision Owner | Product + Technical Review |

**Review outcome (accepted):** Java 17+ / Spring Boot 3.x is the approved backend direction. Approved constraints and non-approvals below; `ADR-002` will formalize after the OD review sequence completes.

**Accepted Technology Decisions:**

```text
Java 17+
Spring Boot 3.x
PostgreSQL
Flyway
REST/OpenAPI
WebSocket capability
Docker
```

**NOT automatically approved — Future Infrastructure Decisions** (each requires its own justification/OD):

```text
Redis
Kafka
RabbitMQ
Elasticsearch/OpenSearch
Kubernetes
Service Mesh
```

**Architecture constraints binding OD-02:**

- Preserve OD-01 modular monolith with strong internal domain/module boundaries — evaluate/use **Spring Modulith** or an equivalent mechanism to enforce them. The 15 domains are NOT converted to 15 independently deployed services.
- **Realtime isolation:** the main backend remains Java + Spring Boot; StarMitra Connect realtime capability is architecturally isolated from core domain logic. MVP covers FRS messaging only (1:1, group/project conversations, text, media attachments, delivery/read status, project-linked threads, notification integration). Live video/streaming = future scope. No separate messaging microservice now — the boundary is designed for extraction if real scale requires it.

### Criteria Comparison

| Criteria | A. Java / Spring Boot | B. Node / NestJS | C. Python / FastAPI |
|----------|----------------------|------------------|---------------------|
| **Domain complexity** | Excellent. Spring Modulith enforces bounded contexts in-code — maps directly to D1–D15; mature patterns for config-driven engines (rubric builder, round configs) and deep entity graphs (User↔Skill↔Role↔ContributionRole). | Good. Nest modules map to D1–D15; DI + decorators are clean; deep invariants rely on convention/discipline more than enforcement. | Adequate. Router/service layering is manual; no module-boundary enforcement; dynamic typing raises drift risk in deep domain graphs (Pydantic mitigates boundaries). |
| **Transactions** (submissions, votes, scoring, ranking, tie-break, audit, concurrency) | Best-in-class. Declarative `@Transactional`, explicit isolation/locking (optimistic+pessimistic), JPA maturity for the scoring pipeline's invariants; virtual threads ease concurrent load. | Good. Prisma/TypeORM transactions cover typical flows; concurrency controls (row locks, versioning) are more manual; async/await obscures transaction scope. | Adequate–Good. SQLAlchemy has full tx control; async-session transaction semantics are error-prone for junior devs; sync path solid. |
| **Security** (authN, authZ, RBAC, JWT/OIDC, validation, rate limit, maturity) | Strongest. Spring Security is the reference implementation for JWT/OIDC/RBAC; method-level security cleanly enforces the `TalentSkill ≠ SystemRole` rule; Keycloak/Azure-AD integrations first-class; most enterprise-proven. | Good. Passport/JWT/guards; RBAC patterns hand-rolled but clear; mature but less standardized. | Adequate–Good. OAuth2 helpers + JWT libs; RBAC largely manual — discipline-dependent. |
| **REST/API** (validation, DTOs, versioning, OpenAPI, errors) | Strong. Bean Validation + DTOs, springdoc-openapi, `@ControllerAdvice` error model, API versioning straightforward, MockMvc for API tests. | Strong. class-validator DTOs, Swagger decorators, exception filters, built-in URI versioning. | Best ergonomics. Pydantic auto-validation + auto-OpenAPI from type hints; versioning is manual but trivial. |
| **Async processing** (media, notifications, indexing, analytics, score aggregation, jobs) | Strong. `@Async`, schedulers, **Spring Batch** for scoring/aggregation runs, first-class Kafka/RabbitMQ integration. | Good. BullMQ (Redis-backed queues), event emitters; capable but Redis-dependent for real job infra. | Adequate. Needs Celery/ARQ sidecar stack for real jobs; asyncio fine for I/O-bound only. |
| **Realtime** (Connect chat, WS, groups, delivery/read status) | Adequate. Spring WebSocket/STOMP handles rooms + receipts; scaling needs a broker relay; least ergonomic of the three. | Strongest. Socket.IO is reference-grade (rooms, acks → delivery/read receipts, auto-reconnect); natural fit for D6. | Adequate. FastAPI WS + python-socketio work; least polished at scale. |
| **PostgreSQL** (tx, relational, JSONB, pooling, migrations, ORM) | Strong. Hibernate/jOOQ + JSONB types, HikariCP (industry-best pooling), Flyway/Liquibase migrations, Testcontainers integration tests. | Good. Prisma (excellent DX, JSONB, migrations) or TypeORM; pooling less tunable. | Good. SQLAlchemy 2.0 + Alembic + asyncpg; JSONB supported; mature. |
| **Testing** (unit, integration, API, contract, E2E, CI) | Strong. JUnit5, Mockito, MockMvc, **Testcontainers** (real-PG tests), Spring Cloud Contract for consumer contracts. | Strong. Jest, supertest, Pact; easy DI mocking. | Good. pytest fixtures excellent, httpx TestClient, testcontainers-python. |
| **Maintainability** (boundaries, refactoring, onboarding) | Strong. Compiler-checked structure, best-in-class IDE refactoring, enforced layering — scales to large codebase; steeper onboarding curve. | Good. TS types + Nest conventions; churn risk in JS tooling; refactoring good. | Good early, degrades at scale — minimal enforced structure invites drift in a 15-domain codebase. |
| **Scalability** (horizontal, stateless, workers, WS, DB) | Strong. Stateless by design, virtual threads for concurrency, proven clustering/scale patterns. | Good. Stateless easy; WS scale-out needs Redis adapter; CPU-bound work needs worker processes (single-threaded runtime). | Good. Async I/O scales well for I/O; GIL caps CPU concurrency per process → multi-process pattern standard. |
| **Microservice extraction** (media, messaging, notifications, search, analytics) | Strongest. Spring Modulith boundaries → clean seams; Kafka/RabbitMQ/messaging maturity is the best of the three for event-driven extraction. | Good. Module boundaries help; BullMQ/NATS/Kafka clients exist; extraction viable. | Adequate. Boundaries are manual; extraction possible but contracts less typed/enforced. |
| **Team fit** (productivity, learning curve, debugging, local dev, hiring) | Slower initial velocity (ceremony, JVM warmup); top-tier docs/debugging; large global hiring pool; heavier local resource footprint. | Fastest for TS-capable devs; one-language stack with React/RN; quick onboarding; npm churn. | Fastest to write and read; simplest local setup; under-structuring risk as team grows. |
| **Ecosystem** | Deepest enterprise ecosystem (batch, security, integration, observability). | Widest web ecosystem; npm supply-chain diligence needed. | Broad (data/AI strength) though less deep for transactional enterprise apps. |
| **Operational complexity** | Moderate–high. JVM tuning/memory; slower cold starts (mitigable). | Moderate. Process management + worker tiers; light runtime. | Moderate. Multi-process workers standard; light runtime. |
| **Frontend/mobile alignment** (React/Next/RN/TypeScript) | Different language — but OpenAPI codegen produces identical TS client types regardless; practical parity with B on *code* sharing. | Same language — real benefit is **toolchain/skill consolidation**, not code reuse: generated types are identical either way. Quantified benefit ≈ hiring/onboarding simplicity + shared lint/test patterns, not shared implementation code. | Same as A — parity via codegen; plus Python splits the stack's skill profile. |

### Devin Recommendation

**Option A — Java 17+ / Spring Boot 3.x.**

*Revised from the initial register recommendation (Node.js/NestJS). The deeper criteria pass changed the weighting — the revision is recorded honestly rather than silently updated.*

**Why A, factually:**

- StarMitra's hardest engineering problem is the **transactional competition core** — concurrent voting, independent judge evaluations, weighted aggregation, ranking, tie-breaks, authorized overrides, all with append-only audit `[FRS §18–24][§30]`. Spring's declarative transactions, explicit locking, and Spring Batch are the strongest answer here.
- **Security maturity** matters for RBAC + judge/admin isolation + the `TalentSkill ≠ SystemRole` rule; Spring Security is the most battle-tested option.
- OD-01 mandates a **modular monolith designed for future extraction** — Spring Modulith *enforces* module boundaries in code, and the Spring messaging ecosystem (Kafka/RabbitMQ) is the strongest extraction runway for media/messaging/notifications.
- PostgreSQL story is the deepest: HikariCP, Flyway/Liquibase, JSONB support, Testcontainers.
- The previously-considered direction (Java/Spring Boot) is noted as context — this recommendation stands on the evaluation above, not on that context.

**Honest costs of A:**

- Slower initial dev velocity and more ceremony than B or C — real MVP-speed cost for a small team.
- Heavier runtime/ops (JVM tuning, memory) vs Node/Python.
- **Weakest realtime ergonomics** of the three — chat delivery/read receipts are more natural in Socket.IO. Mitigations: Spring WebSocket/STOMP covers the FRS's actual requirements (delivery + read status, group rooms); or, under the OD-01 extraction path, messaging can later be a satellite service in whatever runtime fits.
- Language split vs the TypeScript frontend/mobile stack — mitigated because client types are generated from OpenAPI identically either way; the real sacrifice is one-language hiring/toolchain simplicity, not code reuse.

**When B (NestJS) would instead be right:** if MVP velocity and single-language staffing outweigh the transactional-depth argument, or the team is firmly TS-centered. **When C (FastAPI):** if the team's strength is Python — but it's the weakest fit for module enforcement and realtime.

### Open Questions (need Product/Technical input)

1. **Team skills today:** JVM experience available or acquirable? (Decisive between A and B.)
2. **MVP velocity pressure:** is speed-to-market or long-term platform robustness the higher priority for v1?
3. **Realtime expectations:** are Socket.IO-grade chat features (typing indicators, presence) needed at MVP, or is delivery/read status sufficient?
4. **Hiring market assumption:** local JVM vs Node talent availability/cost.
5. **Runtime profile:** expected competition-peak concurrency — does it push toward JVM's concurrency model?

## OD-03 — Primary Database

| Field | Content |
|-------|---------|
| Decision ID | OD-03 |
| Decision | Primary transactional database (system of record). |
| Context | FRS §31 logical model is inherently relational (M:N spine: User↔TalentSkill, User↔SystemRole, Project↔Member↔ContributionRole); strict consistency for votes/evaluations/scores `[FRS §18–24][§30][§36]`; config-driven entities (rubric versions, round configs) `[FRS §15][§20]`; admin reporting `[FRS §29]`; audit + backup/recovery `[FRS §30][§36]`. **Note:** PostgreSQL already sits inside the OD-02 accepted backend baseline — this OD formally evaluates/approves it as the primary datastore. |
| FRS References | §10 content lifecycle, §15–24 competition pipeline, §30 audit, §31 data model, §36 NFRs |
| Options | **A. PostgreSQL** · **B. MySQL 8** · **C. MariaDB** · **D. SQL Server** · **E. Distributed SQL (CockroachDB/Yugabyte)** · **F. Non-relational (MongoDB-class) — evaluated and rejected for the transactional core** |
| Status | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| Decision Owner | Product + Technical Review |

**Review outcome (accepted):** PostgreSQL is StarMitra's primary transactional relational datastore. `ADR-003` will formalize after the OD sequence. **Binding guardrails:**

1. Core business entities remain **relational and strongly typed**.
2. **JSONB selectively** — only for genuinely configuration-driven/flexible structures (rubric criteria, round configs, eligibility rules); never as a substitute for typed core entities.
3. Prefer **PostgreSQL constraints and transactions** for invariant enforcement (FK, unique, check, Σ weights = 100% `[BR-11]`, immutability rules).
4. **Row-level locking** where contention/concurrency requires it.
5. **Advisory locks are NOT a blanket requirement** — only where a specific concurrency design justifies them.
6. **Partitioning, replicas, CDC, multi-region** and other advanced capabilities remain **future decisions** unless separately approved.
7. This decision does **NOT** approve Redis, Kafka, RabbitMQ, Elasticsearch/OpenSearch, Kubernetes, or Service Mesh.

### 1. Domain Model Fit

All FRS §31 entities map directly to relational tables — no impedance mismatch:

| Domain | Relational fit |
|--------|----------------|
| Users & multi-talent skills | `User`—`UserTalentSkill`—`TalentSkill` M:N — natural |
| Portfolio/media metadata | `PortfolioItem`, `MediaAsset`, `MediaVariant` — FK-linked; binaries live in object storage (metadata only in DB) |
| Creative Rooms | `CreativeRoom`, `Project`, `ProjectMember`, `ProjectContributionRole` — M:N + contextual mapping |
| Competitions/rounds | `Competition`, `CompetitionRound`, `CompetitionCategory` — parent-child + per-round config (JSONB optional) |
| Submissions | Immutable association FKs (competition/round/category) `[FRS §16]` |
| Voting | `Vote` append-only + `VoteAggregate` |
| Judges/assignments | `Judge`, `JudgeAssignment` — scoped references |
| Rubrics | `EvaluationTemplate` → `EvaluationTemplateVersion` → `EvaluationCriterion` — versioned tree; JSONB viable for criterion config blobs |
| Scores/ranking/qualification | `Score`, `Ranking`, `QualificationDecision`, `OverrideRecord` — strictly transactional |
| Notifications | `Notification`, `NotificationPreference`, `DeliveryAttempt` — append-heavy inserts |
| Moderation | `Report`, `ModerationCase`, `ModerationAction` |
| Audit | `AuditLog` append-only `[FRS §30]` |

### 2. Transactional Requirements

| Requirement `[FRS]` | PostgreSQL mechanism |
|---------------------|----------------------|
| One vote per voter per configured limit `[BR-14][§18]` | Unique constraint + `INSERT … ON CONFLICT DO NOTHING` — dedup at the DB, not just app logic |
| Vote/evaluation/audit atomicity `[§18][§21][§30]` | Multi-statement transactions; audit row written **in the same transaction** as the business write |
| Evaluation submission → lock `[BR-12]` | Atomic insert + status transition; subsequent writes rejected at constraint level |
| Score aggregation `[§22]` | Aggregation reads under consistent snapshot; result writes transactional |
| Ranking/qualification `[§23]` | Batch compute → single commit; repeatable-read isolation for stable input |
| Round progression `[§23]` | Eligibility read (prior round) + advancement write in one transaction |
| Authorized overrides `[BR-13]` | Override row + `AuditLog` + recomputed standing in one transaction — override can never exist without its audit record |
| Auditability `[§30]` | Append-only table; insert-only privileges optional hardening |

### 3. Data Modeling Capability

- **Foreign keys** — full referential integrity across the M:N spine `[FRS §31.1]`.
- **Unique constraints** — `TalentSkill.name` uniqueness `[FRS §7]`; vote-dedup keys `[BR-14]`; one evaluation per judge×submission.
- **Check constraints** — enforce Σ weights = 100% at insert for rubric criteria `[BR-11][FRS §20]` and scoring mixes `[FRS §22]`; enum-like status fields.
- **Indexing** — B-tree, composite (e.g., `(competition_id, round_id, category_id)` for rankings), partial indexes (active submissions only), covering indexes for leaderboards.
- **JSON/JSONB** — suitable for **config-driven payloads**: rubric criterion definitions, round config, eligibility rules, scoring rules. *Inference (not FRS):* keep the relational spine typed; use JSONB only for truly variable configuration — not for core entities.
- **Temporal/versioned data** — natural fit: `EvaluationTemplateVersion` immutable rows `[BR-10]`, versioned consent records `[FRS §8]`, effective-dated configs.
- **Audit/event records** — append-only `AuditLog`; optional `OutboxEvent` table for reliable domain-event publishing *within* the monolith (no broker implied — see §8 note below).

### 4. Concurrency Requirements

| Scenario | PostgreSQL answer |
|----------|-------------------|
| Concurrent audience voting `[§18]` | MVCC — readers don't block writers; dedup via unique constraint + upsert; aggregate counter updates via row locks or deferred rollup |
| Concurrent judge evaluations `[BR-12]` | Judges touch disjoint rows (own assignments) — minimal contention; `SELECT … FOR UPDATE` where needed |
| Competition closing transitions | Single scheduler transaction; advisory lock prevents double-close |
| Ranking calculation | Consistent-snapshot read + bulk insert/upsert of `Ranking` |
| Round progression | Transactional read-then-write; row locks on affected `Submission`/standing rows |
| High-contention rows (aggregates, counters) | Row-level locking is adequate at MVP scale; counter contention can be smoothed via aggregate-table rollups — *inference: no queue/broker required for MVP* |

MVCC + row-level locks + advisory locks cover every FRS concurrency case without external coordination.

### 5. Scalability — MVP → Growth

- **MVP:** single primary + HikariCP pooling covers expected load comfortably — competition write bursts are modest by industry standards.
- **Growth path without premature scale:** read replicas for reporting/feed reads `[FRS §29]`; connection pooler (pgbouncer-class) under connection pressure; table partitioning for high-volume append tables (`Vote`, `AuditLog`, `Notification`) *if/when* volume warrants — additive, no redesign.
- **Not needed now:** sharding, distributed SQL, multi-region — mark future decisions.
- Media binaries never in DB — DB load is metadata-scale, not GB-scale.

### 6. Spring Boot Compatibility (OD-02 stack)

| Concern | PostgreSQL answer |
|---------|-------------------|
| Spring Data JPA / Hibernate | First-class dialect; JSONB via `@Type`/hypersistence or attribute converters |
| JDBC | Direct access where ORM is inappropriate (aggregations, batch ranking) |
| Transactions | `@Transactional` → PG isolation levels/locks; `@Lock` for pessimistic paths |
| Connection pooling | **HikariCP** (Spring Boot default) — best-in-class |
| Testcontainers | `PostgreSQLContainer` — real-DB integration tests for scoring/voting paths |
| Flyway | **Accepted in OD-02 stack** — versioned migrations, pristine baseline |
| Batch/jobs | Spring Batch job-repository tables — native support |

### 7. Reliability & Operations

- **Backup/recovery `[FRS §36]`:** pg_dump + WAL archiving → PITR; managed offerings automate this.
- **Replication/read replicas:** native streaming replication; read replicas for reporting/offload.
- **Monitoring:** `pg_stat_*` views, slow-query logging; every major cloud exposes dashboards.
- **Migration management:** Flyway (accepted); transactional DDL reduces half-migrated states.
- **Production operations:** widest managed-service availability of any OSS database (all three major clouds).

### 8. PostgreSQL-Specific Capabilities — Scoped Adoption

| Capability | Status for StarMitra |
|------------|----------------------|
| FK/unique/check constraints, MVCC, row locks, advisory locks, `INSERT…ON CONFLICT`, JSONB, partial/composite indexes, CTEs/window functions (ranking math!), transactional DDL | **Required-for-MVP scope** |
| FTS `tsvector` + `pg_trgm` (powers OD-11 DB-search recommendation), table partitioning, LISTEN/NOTIFY, materialized views (leaderboards/reporting), generated columns | **Optional — adopt when the dependent feature lands** |
| Logical replication/CDC feeds, multi-region setups, exotic extensions, row-level security for multi-tenant partitioning | **Not adopted without a separate decision** |

### 9. Alternatives — Explicit Criteria

| Criterion | PostgreSQL | MySQL 8 | MariaDB | SQL Server | Distributed SQL (CockroachDB-class) | MongoDB-class |
|-----------|-----------|---------|---------|------------|-------------------------------------|----------------|
| Constraint rigor (checks, FK strictness) | Excellent | Good (strict mode) | Good | Excellent | Good–Excellent | Weak (app-enforced) |
| JSON for config entities | **JSONB — best-in-class** | JSON (functional) | JSON (functional) | JSON (functional) | JSONB-compatible | Native — but wrong model shape |
| Concurrent voting/eval paths | MVCC + rich locking | InnoDB MVCC | InnoDB MVCC | MVCC | Global MVCC | Doc-level tx, weaker multi-doc |
| Tx guarantees for scoring pipeline | Excellent | Good | Good | Excellent | Excellent | Adequate |
| Versioned/immutable data fit | Excellent | Good | Good | Excellent | Good | Adequate |
| FTS/search baseline | Built-in FTS+trigram | Basic FTS | Basic FTS | Full-Text Search | Limited | Atlas Search (managed) |
| Spring Boot integration | First-class | First-class | First-class | First-class | Compatible | Via Spring Data Mongo |
| Ops/managed availability | Best-in-class | Excellent | Good | Excellent (licensed) | Limited/complex | Managed (Atlas) |
| Cost | Free/OSS | Free/OSS | Free/OSS | License $$$ | Costly | Free/managed $ |
| MVP fit | **Best** | Credible | Credible | Credible (licensing) | Premature | Wrong shape — rejected |

**MySQL/MariaDB** are credible substitutes — StarMitra's model would work — but weaker on JSONB-class config modeling, advanced indexing (partial), window-function ergonomics for ranking, and FTS for OD-11. **SQL Server** is technically capable; licensing makes it unjustified for a startup. **Distributed SQL** answers scale questions StarMitra doesn't have yet. **Document DBs** conflict with the inherently relational FRS §31 model and move integrity into application code.

### 10. Recommendation

**PostgreSQL — recommend ACCEPT.**

- **Advantages:** exact fit for the relational FRS model; strongest constraint/transaction toolkit for the competition pipeline; JSONB covers config-driven entities without a second store; FTS seeds OD-11; free/OSS with the widest managed-service availability; seamless in the accepted Spring Boot stack (HikariCP, Flyway, Testcontainers, Spring Batch).
- **Trade-offs:** JSONB ≠ schemaless document store (not needed); sharding is manual if ever needed (not needed at MVP); advanced features (partitioning, LISTEN/NOTIFY, CDC) arrive only via separate decisions.
- **Risks:** (i) over-reliance on a single DB for search/analytics later — mitigated by OD-10/11/13 staging triggers; (ii) heavy reporting queries contending with OLTP — mitigated by read replica + rollups before competition peaks; (iii) migration drift — mitigated by Flyway discipline (accepted) + transactional DDL.
- **Impact on architecture:** single system of record inside the modular monolith; module-per-schema or module-per-table conventions enforce OD-01 ownership boundaries *within* one database — modules must not reach across each other's tables.
- **Impact on future extraction:** module-owned tables/schemas make later extraction mechanical (export schema → new service DB); JSONB config entities keep rubric/round evolution inside module ownership; no shared-cache or broker implied — consistent with "no Redis/Kafka without separate decision."

## OD-04 — Web Frontend

| Field | Content |
|-------|---------|
| Decision ID | OD-04 |
| Decision | Web technology for Public Web + Creator + Audience + Admin Portal + Judge Portal `[FRS §5][§27][§28]`. |
| Context | Four web surfaces with different characters: public discovery (`[FRS §11]`, SEO-relevant — *inference, not FRS requirement*), authenticated creator/audience app, two internal portals (admin `[§27]`, judge `[§28]`). Brand baseline exists (verified palette, PROPOSED Poppins/Inter); accessibility + mobile-first are FRS NFRs `[§36]`. Backend is Java/Spring Boot REST/OpenAPI + WebSocket (OD-02). |
| FRS References | §5 channels, §9 profile, §10 media, §11 discovery, §12 Connect, §13 rooms, §15–24 competition flows, §26 moderation, §27–28 portals, §34 screens, §36 NFRs |
| Options | **A. React + TypeScript + Vite (SPA, all surfaces)** · **B. React + TypeScript + Next.js (SSR-capable, all surfaces)** · **C. Angular** · **D. Vue/Nuxt** · **E. SvelteKit** |
| Status | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| Decision Owner | Product + Technical Review |

**Review outcome (accepted):** React + TypeScript + Vite SPA is the approved web frontend baseline. `ADR-004` will formalize after the OD sequence. **Binding guardrails:**

1. React + TypeScript + Vite is the approved web frontend baseline.
2. **One** web application/codebase serves public, authenticated app, judge and admin experiences.
3. Route groups are organizational/access boundaries only — **backend authorization remains authoritative**.
4. **Mobile-first responsive design is mandatory.**
5. WebSocket integration remains isolated from core frontend domain/state logic.
6. Specific frontend libraries remain separate decisions/recommendations.
7. Authentication/session strategy remains deferred to OD-06.
8. SEO/SSR remains an explicit open sub-decision.
9. Next.js, Redux, Zustand, Tailwind, or other libraries/frameworks are NOT automatically approved.

### 1–2. Core Technology Evaluation — React + TypeScript + Vite

| Aspect | Assessment |
|--------|------------|
| React + TypeScript | Largest ecosystem/hiring pool; component model fits design-system cards/forms; TS safety across the codebase; aligns with future React Native evaluation (OD-05) without implying it |
| Vite | Fast dev server + build; standard tooling for SPA React; no server runtime required — deploy as static assets behind CDN or Spring-hosted statics, consistent with monolith deployment |
| SSR capability | **Not provided by Vite** — the material trade-off vs Next.js; see SEO analysis below |

### 3. Responsive / Mobile-First `[FRS §36]`

FRS requires responsive, mobile-first UX. A React SPA delivers this via responsive CSS + component design; mobile web is also the fallback for users without the app. No framework-level constraint — satisfied equally by A/B.

### 4–5. Surface Coverage

| Surface | FRS scope | SPA fit |
|---------|-----------|---------|
| Public (landing, discovery, talent profiles, portfolio/media, competitions, public submissions/results) `[§5][§34]` | Crawlability desirable — *inference* | Works, but SEO needs mitigation (below) |
| Creator (home, profile, skills, portfolio, upload, competitions, submissions, results) `[§34]` | Auth'd app | Excellent |
| Audience (browse, vote, engage) `[§6]` | Auth'd app | Excellent |
| Judge (dashboard, assigned entries, evaluation form, history) `[§28]` | Auth'd app | Excellent — deterministic SPA |
| Admin (users, skills, competitions, rounds, votes, judges, rubrics, moderation, reports, audit, config) `[§27]` | Auth'd app | Excellent — forms/tables heavy |

**Packaging recommendation:** one SPA codebase with route-group surfaces — `/` public, `/app` creator+audience, `/judge`, `/admin` — role-gated routing over the shared design system. Splitting into separate apps is a build-time detail, not an OD.

### 6. Admin & Judge Portals `[§27][§28]`

Both are authenticated, data-dense, form/table-heavy — the ideal SPA use case. No SEO need; deterministic rendering; role-scoped API surfaces enforced backend-side `[FRS §19]`.

### 7. State Management

- **Server state:** recommend **TanStack Query**-class server-state library — caching, retries, invalidation for feeds/votes/leaderboards. `RECOMMENDATION, not approval.`
- **Client/global state:** keep minimal — React state + context; **Redux/Zustand NOT required at MVP** — introduce only if a concrete cross-surface state need emerges (separate decision).
- Forms state: form library local to forms (§10).

### 8. API Integration (Spring Boot REST/OpenAPI)

Backend ships OpenAPI contracts (OD-02). Recommend **codegen'd typed client** (openapi-typescript / Orval-class) — types generated identically regardless of frontend choice; auth interceptor attaches token; consistent error model per API-ARCHITECTURE.

### 9. WebSocket Integration (Connect)

Client wraps WS in a dedicated `realtime` module exposing typed events to D6 surfaces — preserving the OD-02 isolation decision (messaging isolated, extractable later). Transport detail depends on backend WS mechanism (STOMP-over-WS is Spring's idiom vs native WS) — **open sub-detail pending backend realtime design**, not a blocker for the framework decision.

### 10. Form-Heavy Workflows

Rubric builder (dynamic criteria, weights, ordering `[FRS §20]`), competition creation (round configs `[§15]`), profile/skills, submissions, judge evaluation forms — recommend **React Hook Form + schema validation (Zod-class)** `RECOMMENDATION`: dynamic field arrays for criteria, per-criterion scoring UIs, validation shared with backend error model. This is the heaviest form workload in the product — library choice matters but is an implementation decision, flagged not approved.

### 11. Media Upload & Playback `[§10][§16]`

- **Upload:** backend issues pre-signed URL (MEDIA-ARCHITECTURE) → browser PUTs directly to object storage → progress UI; backend never proxies binaries `[FRS §36]`.
- **Playback:** `<video>`/`<audio>` + HLS where transcoded renditions exist; responsive images via variant URLs; document preview strategy open (OD-08 dependent).
- **Secure access:** signed URLs for non-public media; visibility enforced server-side.

### 12. Accessibility & Responsive Design

WCAG AA target (brand baseline §8): semantic HTML, focus management for modals/routes, `eslint-plugin-jsx-a11y`, keyboard navigation, touch targets ≥44px, no color-only meaning — enforced by component library conventions + lint + a11y audits in CI.

### 13. Performance & Code Splitting

Route-level code splitting per surface (admin bundle never ships to audience); lazy media components; bundle budgets in CI; CDN-cached static assets; image/video lazy-loading. React 18 concurrent features optional.

### 14. Security

| Concern | Approach |
|---------|----------|
| Auth tokens | Session strategy tied to OD-06: recommend **httpOnly secure cookies** (XSS-resistant) OR memory-held access token + refresh rotation — *sub-decision, not resolved here* |
| Authorization | Server-side enforcement only `[BR-02]`; UI hides/disables but never grants |
| XSS | React escaping + sanitization for any rich text; strict CSP |
| CSRF | If cookie auth → SameSite=Strict/Lax + CSRF token for mutations |
| Secure media | Signed URLs, no client-side trust of visibility |

### 15. Testing Strategy

Vitest + Testing Library (unit/component), MSW (API mocks aligned to OpenAPI), Playwright (E2E critical flows — voting, evaluation submit, upload), contract tests against OpenAPI, visual/a11y checks in CI.

### 16. Maintainability & Mobile Alignment

Component-driven design system implementing the brand tokens; domain-mirrored feature folders matching D1–D15 modules; shared TS types from OpenAPI — the same types a future React Native app (OD-05) would consume (types share, not components — honest limit).

### 17. Alternatives — Explicit Comparison

| Option | For | Against | StarMitra verdict |
|--------|-----|---------|-------------------|
| **A. React+TS+Vite SPA** | Simplest ops (static deploy); all four surfaces fit; huge ecosystem; matches Spring static/CDN serving | No SSR/SEO out of box; needs mitigation for public discovery | **Recommended** |
| **B. React+TS+Next.js** | SSR/SSG for public discovery; same React knowledge | Server runtime required for portals that don't need it; complexity (RSC, caching semantics); overkill for 3 of 4 surfaces | Strong alternative **if public SEO is confirmed critical at MVP** — see open question |
| **C. Angular** | Batteries-included for forms/admin | Heavier for consumer surfaces; smaller pool vs React; splits from RN-aligned ecosystem | Rejected — overkill |
| **D. Vue/Nuxt** | Capable, good DX | Smaller hiring/ecosystem than React | Credible but no advantage |
| **E. SvelteKit** | Lean bundles | Smallest ecosystem/enterprise track | Rejected for team risk |

**SEO sub-decision — the real fork:** `[FRS §5]` lists "discovery and selected public content" on public web but does **not** state an SEO requirement — *architecture inference*. Options: (i) accept SPA client-rendering for MVP, (ii) prerender public routes (vite-plugin-prerender-class — cheap, flaky at scale), (iii) split public site into a small SSR/SSG app (Next.js/Astro) later. **Recommendation: proceed with SPA; hold the SEO carve-out as an open sub-decision for the product owner.**

### Devin Recommendation

**Option A — React + TypeScript + Vite**, one SPA serving all four surfaces via role-gated route groups. Recommendations (not approvals): TanStack Query server-state, React Hook Form + Zod, openapi-typescript codegen, Vitest/Playwright testing, Tailwind-vs-CSS-solution is a separate styling decision (Tailwind was in previously-considered context — **not auto-approved**).

**Trade-offs:** loses built-in SSR (accepted — mitigable later if SEO is confirmed); gains the simplest possible deployment, fastest dev loop, and full alignment with the monolith's static-asset serving.

### Open Questions

1. Is organic SEO for public discovery required at MVP? (drives SPA vs SSR fork)
2. Cookie-based session vs token-in-memory (ties to OD-06)?
3. Admin+Judge as route groups of one SPA vs separate thin apps (packaging detail — confirm)?
4. Styling system choice (Tailwind vs CSS-in-JS vs component library) — separate review needed.

## OD-05 — Mobile Technology

| Field | Content |
|-------|---------|
| Decision ID | OD-05 |
| Decision | Mobile strategy for the mobile-first Audience+Creator app `[FRS §5]`. |
| Context | `[FRS §5]` names the **Mobile App** as the primary channel for Audience and Creators: "create, upload, discover, engage, communicate, participate." FRS does **not** specify mobile implementation details — all technology evaluation below is architecture recommendation/inference. |
| FRS References | §5 channels, §9–10 profile/portfolio/media, §11 discovery, §12 Connect, §13 rooms, §15–18 competitions/submissions/voting, §25 notifications, §26 moderation/reporting, §35 priorities |
| Options | **A. React Native + TypeScript (+Expo)** · **B. Flutter/Dart** · **C. Native (Kotlin + Swift)** · **D. Responsive web only / PWA** |
| Status | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| Decision Owner | Product + Technical Review |

**Review outcome (accepted):** React Native + TypeScript + Expo is the approved mobile baseline. `ADR-005` will formalize after the OD sequence. **Binding guardrails:**

1. React Native + TypeScript + Expo is the approved mobile baseline.
2. **MVP mobile scope = Creator + Audience.**
3. Admin and Judge experiences remain **web-based** for MVP.
4. Mobile does **not** require full feature parity with web.
5. React web UI reuse must **not** be assumed; shared TypeScript contracts/utilities may be evaluated selectively.
6. Android and iOS are the intended platforms; **release sequencing is a separate decision**.
7. Push notification provider is a **separate decision**.
8. OTA/update tooling is a **separate decision**.
9. Native modules/device-specific capabilities require **separate evaluation**.
10. Expo approved as development platform — **individual Expo services/packages are not automatically approved**.
11. Future Admin/Judge mobile experiences are not prohibited but require a future product/architecture decision.

### 1–2. Approach & Alternatives

| Option | Fit for StarMitra | Verdict |
|--------|-------------------|---------|
| **A. React Native + TS** | One language with OD-04 web + OD-02 codegen types; mature media/push/notifications ecosystem (Expo); one team can cover web+mobile | **Recommended** |
| **B. Flutter** | Best UI consistency/performance (own renderer); strong animation for a "premium" brand feel; but separate Dart stack — no shared code or devs with web | Strong challenger — see §8 |
| **C. Native (Kotlin+Swift)** | Peak performance, zero framework impedance for camera/media | Rejected for MVP — ~2× cost, two codebases |
| **D. Responsive web / PWA only** | Zero new stack | Rejected — fails `[FRS §5]` "Mobile App" channel intent; iOS PWA limits (push, media UX); no store presence |

### 3. FRS Requirement Coverage (mobile-relevant `[FRS §5]`)

| FRS capability | RN coverage | Flutter coverage |
|----------------|-------------|------------------|
| Auth (OTP) `[§8]` | Secure-store + OTP UX — mature | Equally capable |
| Profile + multi-skills `[§7][§9]` | Standard | Standard |
| Portfolio/media `[§9][§10]` | Camera/gallery pickers, direct-to-storage upload | Equal |
| Discovery/feed `[§11]` | FlatList patterns; FlashList for perf | Excellent scroll perf |
| Likes/comments/follows `[§4][§11]` | Standard | Standard |
| StarMitra Connect `[§12]` | WS client + chat UI; delivery/read receipts | Equal |
| Creative Rooms `[§13]` | Standard screens | Standard |
| Competitions/submissions `[§15–17]` | Media capture + upload + status tracking | Equal |
| Audience voting `[§18]` | Standard | Standard |
| Notifications `[§25]` | FCM/APNs via Expo Notifications-class | firebase_messaging-class |
| Moderation/reporting `[§26]` | Standard | Standard |

### 4. Explicitly Out of Mobile Scope (web/admin/judge surfaces)

`[FRS §5]` — **Admin Web** and **Judge Web** are separate channels. Mobile scope excludes: admin configuration, rubric builder, competition ops, moderation console, judge evaluation workflows, analytics dashboards. *(Any future "admin-on-mobile" is a product decision, not assumed.)*

### 5. Cross-Cutting Evaluation

| Concern | React Native | Flutter |
|---------|--------------|---------|
| Shared code with React web | **Types + domain logic** (API client, models, validation schemas) shared via TS — real but partial; no UI sharing | None |
| UI consistency | Good; platform-idiomatic | Best-in-class pixel consistency |
| Native capabilities | Mature bridge + Expo modules (camera, mic, secure store, notifications) | Strong plugin ecosystem |
| Media capture/upload | Camera/gallery → pre-signed direct upload (backend never proxies) | Equal |
| Push notifications | FCM/APNs — Expo-class services; **provider choice = future decision, not approved** | Equal |
| Deep linking | Universal/app links standard | Standard |
| Performance | Good for feed/media; heavy editing needs native modules | Slightly better animation/render perf |
| Offline | Cache images + queue actions; FRS doesn't mandate offline — *inference: nice-to-have* | Equal |
| App-store deployment | Store review cycles; OTA updates (EAS/CodePush-class — **tooling decision pending**) | Same, CodePush-class needed for OTA |
| Testing | Jest + RNTL + Detox/Maestro E2E + emulator CI | flutter_test + integration_test + Patrol-class |
| Maintainability | TS + React patterns — team carries web skills | Dart learning; separate lint/test/docs toolchain |
| Skill requirements | React/TS (same as OD-04) | Dart/Flutter (new) |
| Future scalability | Bridge/perf edge cases at heavy media scale; module extraction unaffected | Scales well; lock-in to Dart ecosystem |

### 6. Initial Scope — A/B/C Analysis

| Option | Verdict |
|--------|---------|
| **A. Full feature parity** | Rejected — FRS assigns admin/judge to web `[§5]`; parity would duplicate portal surface area for no user need |
| **B. Creator/audience focused** | **Recommended** — exactly `[FRS §5]`'s mobile scope: create, upload, discover, engage, communicate, participate |
| **C. Phased subset + web for admin/judge** | Same as B in practice — admin/judge stay web permanently (not phased); creator features could phase if timeline demands: P0 = auth/profile/feed/upload/competition/vote, P1 = rooms/messaging depth |

### 7. Backend Communication

REST/OpenAPI → codegen'd TS client (same generator as web); WebSocket via dedicated realtime client module (isolation per OD-02/OD-04); auth tokens per OD-06 in secure enclave storage; media upload via pre-signed URLs direct to storage; notifications via platform push (FCM/APNs) → backend notification service.

### 8. Does RN Gain Real Benefit from React Web Baseline?

**Yes, but precisely:** shared TypeScript types/domain logic (API client, models, validation), one skill pool, shared design tokens/patterns ported to RN styles, unified lint/test/toolchain. **Not** UI components or screens — those are rewritten. Quantified: ~language+logic sharing, not code reuse of UI. If UI-perfection is judged more valuable than stack unity, **Flutter's explicit cost = a second language (Dart), separate toolchain, separate hiring** — but arguably better visual polish.

### 9. Recommendation

**React Native + TypeScript + Expo (managed workflow, ejectable if needed)** — creator/audience scope per `[FRS §5]`. Strongest alternative: **Flutter** if premium-UI consistency outweighs stack consolidation (explicitly trading a separate Dart stack). Reject native (cost) and PWA-only (fails FRS channel intent). Expo/EAS vs bare RN and push-provider choice are **implementation-level decisions, not approved here**.

### 10. Security Considerations

- Tokens in platform secure storage (Keychain/Keystore via expo-secure-store-class); never AsyncStorage/plain prefs
- Biometric unlock = optional convenience layer, not a credential store change — *inference*
- Secure media via short-lived signed URLs
- Deep links validated server-side; auth-gated screens never trust link params alone
- Authorization always server-enforced `[BR-02]`; client hides, never grants
- Certificate pinning — optional hardening, future decision

### 11. Testing Strategy

Unit (Jest), component (React Native Testing Library), API-mock (MSW against OpenAPI), E2E on emulators/simulators + farmed-device runs for critical flows (upload, vote, chat), store-release smoke tests.

### 12. Deployment/Release

Google Play + Apple App Store; semantic app versioning + build numbers; OTA JS updates for hotfixes (tooling pending); CI = build per platform + store upload automation (EAS/Fastlane-class — pending); staged rollouts.

### Open Questions

1. Android+iOS both at MVP, or Android-first (market/demographic input)? — **Q4 in register**
2. Push notification provider/tooling choice — future infra decision
3. OTA update tooling (EAS vs alternatives)
4. Team RN vs Flutter skill availability
5. Media-edge cases (trimming/editing) — does MVP need native-module-level capture UX? `[FRS §4.2]` says advanced editing is out-of-MVP

## OD-06 — Authentication / Identity

| Field | Content |
|-------|---------|
| Decision ID | OD-06 |
| Decision | Authentication & session architecture for all clients: mechanism, token/session strategy, provider choice. |
| Context | `[FRS §8]` mobile/email + OTP "or configured authentication mechanism"; secure login/logout; password reset where password auth enabled; optional social login *in future*; consent capture; account states Active/Suspended/Blocked/Deactivated. **Critical:** `TalentSkill ≠ SystemRole` — authN and authZ are separate `[BR-2][FRS §6]`. Judge/Admin surfaces warrant stricter policy (inference). |
| FRS References | §6 system roles, §8 registration/auth, §30 audit, §36 security NFRs |
| Options | **A. First-party Spring Security + OTP + JWT/refresh** · **B. Managed identity provider (Cognito/Firebase Auth/Auth0-class)** · **C. Hybrid** |
| Status | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| Decision Owner | Product + Technical Review |

**Review outcome (accepted):** First-party Spring Security is the MVP authentication architecture. `ADR-006` will formalize after the OD sequence. **Binding guardrails:**

1. First-party Spring Security is the MVP authentication architecture.
2. OTP is supported as the primary authentication mechanism.
3. Optional password support remains an **open product decision**.
4. Short-lived JWT access tokens are used.
5. Refresh tokens are opaque, server-controlled and persisted.
6. Refresh token rotation is required.
7. Refresh-token reuse detection is required.
8. Server-side session/token revocation must be supported.
9. Web uses secure HTTP-only cookie transport with appropriate CSRF protection.
10. React Native uses bearer authentication with platform-secure credential storage.
11. Admin/Judge use the same authentication architecture.
12. MFA for Admin/Judge remains a **separate security/product decision**.
13. WebSocket connections must authenticate and respect session/token revocation.
14. Social login is **deferred**.
15. External identity providers are **not required** for MVP.
16. Authentication, authorization, talent skills and project contribution roles remain separate concepts.
17. OTP security controls must be explicitly designed before implementation.
18. No Redis or other infrastructure is implied by this decision.

### 1. Authentication Requirements `[FRS §8]`

| Requirement | Approach |
|-------------|----------|
| Registration (mobile/email + OTP or configured mechanism) | OTP challenge → verify → account create; `OtpChallenge` records |
| Sign-in / sign-out | OTP or password (configured) → issue token pair; sign-out revokes refresh |
| Password handling | Optional credential — where enabled: bcrypt/argon2 hashing, recovery via verified OTP/mail link |
| Account recovery | OTP to verified channel → reset/re-issue |
| Email/phone verification | OTP is the verification mechanism itself |
| Session lifecycle | Short-lived access + rotatable, revocable refresh; logout/inactivity expiry; account-status enforcement per request |

### 2. Authorization Model `[FRS §6][BR-02]` — binding

```text
authZ input = UserSystemRole → SystemRole  (ONLY)
Audience/User · Creator/Talent capability · Judge · Admin · Super Admin
```

System roles map to permission sets; resource-scoping adds judge-assignment, room-membership, content-visibility rules. **TalentSkill and ProjectContributionRole are never authz inputs.**

### 3. Approach Comparison

| Approach | For | Against | Verdict |
|----------|-----|---------|---------|
| **A. First-party: Spring Security, OTP + password(optional), JWT access + opaque DB-persisted refresh** | Full control of OTP UX/cost; zero new infra (PostgreSQL only — no Redis needed); aligns with OD-02/03; revocable sessions support account suspension immediately; FRS's "configured mechanism" fits | Team owns OTP/throttling/token security — real responsibility, mitigated by proven patterns + review | **Recommended** |
| **B. Managed IdP (Cognito/Firebase Auth/Auth0-class)** | Battle-tested flows, MFA built-in, social login nearly free | Per-MAU cost at audience scale; another infra/service dependency requiring its own approval; OTP UX customization limits | Viable alternative — triggers a provider decision + cost review |
| **C. Hybrid (IdP users + in-house admin/judge, or vice versa)** | Tailored per-surface | Two systems to secure — unjustified at MVP | Rejected |

Session-based alternative (Spring session + cookie, no JWT): simpler revocation but awkward for React Native and WebSocket — rejected as the unified mechanism; cookies still used as the *transport* for web.

### 4. Per-Client Strategy

| Client | Transport | Storage | Notes |
|--------|-----------|---------|-------|
| Web SPA (public/app) | Access+refresh via **Secure httpOnly SameSite cookie** | Never in JS-accessible storage | CSRF token required for mutations |
| React Native | `Authorization: Bearer` header | Secure enclave (Keychain/Keystore) | Cookies impractical; standard mobile pattern |
| Admin web | Same cookie model + **mandatory MFA policy** *(inference recommendation — FRS doesn't mandate MFA)* | — | Stricter session TTL recommended |
| Judge web | Same cookie model + MFA policy | — | Access only assigned evaluations `[FRS §19]` |
| WebSocket | Token presented at handshake (header/subprotocol, not query-param-in-logs) | — | Server validates + scopes connection; reconnect re-auths |

**Unified mechanism?** Yes — one credential + token system for all clients; only the **transport** differs (cookie vs bearer header). Separate auth systems per surface rejected.

### 5. Security Requirements

| Concern | Approach |
|---------|----------|
| Password hashing | bcrypt/argon2id where password enabled |
| Credential protection | TLS-only; no password/OTP in logs |
| Token storage | Web: httpOnly cookies; Mobile: secure enclave; Server: refresh hashed in DB |
| Token rotation | Refresh rotation per use; reuse-detection → revoke family |
| Revocation | DB-persisted refresh tokens → instant revoke (suspend/block `[FRS §8]`); short access TTL bounds risk |
| CSRF | SameSite + CSRF token for cookie mutations |
| XSS | React escaping + CSP; token never in JS-readable storage |
| CORS | Explicit origin allowlist; credentials-scoped |
| Brute-force/rate limiting | OTP attempt caps, per-IP+per-account throttling, progressive delays — **server-side app logic (no Redis required at MVP; Redis optional later — not approved)** |
| Account lockout/recovery | Lockout on repeated failures; OTP-based recovery; state machine enforced at auth layer |
| WebSocket auth | Handshake token validation; per-connection authz; close on revoke |
| Audit `[FRS §30]` | Login attempts, OTP issuance, token refresh, role changes, status transitions → AuditLog |

### 6. Stack Interaction

- **Spring Security:** resource-server JWT validation + custom auth endpoints; method-level `@PreAuthorize` enforces system-role permissions.
- **REST/OpenAPI:** `/auth/*` endpoints in contract; 401/403 semantics documented.
- **WebSocket:** handshake interceptor validates token; connection principal = user.
- **PostgreSQL + Flyway:** `AuthCredential`, `OtpChallenge`, `RefreshToken` tables — no additional infra.
- **React web / RN:** codegen'd auth client; token refresh interceptor; secure storage adapters per platform.

### 7–8. Identity Scope `[FRS §8]`

- **First-party auth only for MVP** — FRS requires OTP/password-capable registration; social login is explicitly "optional in future" → design provider-abstraction now, implement later.
- External IdP is **not** assumed required — nothing in FRS mandates it; Option B remains the fallback if cost/ops review favors outsourcing.

### 9. Unified vs Per-Surface Auth

One authentication mechanism, one identity (`User`), one authorization model — four transports only differ in token carriage. Judge/Admin differ only by **policy** (MFA, TTL) — not by separate systems.

### 10. Security/Operational Trade-offs

| | First-party (A) | Managed IdP (B) |
|---|---|---|
| Build cost | OTP + token lifecycle owned | Minimal |
| Security risk | Team owns throttling/enumeration/replay defenses | Vendor-hardened |
| Cost | SMS/email provider fees only | Per-MAU pricing at audience scale |
| Lock-in | None | Migration pain later |
| Revocation | Native (DB) | Provider-dependent |
| Social login later | Manual add | Nearly free |

### 11. Boundary Definitions — binding restatement

```text
Authentication  → "who are you" (OTP/password/token)
Authorization   → "what may you do" (UserSystemRole → permissions) — NEVER talent data
TalentSkill     → "what can you do creatively" — profile data, zero permissions
ProjectContributionRole → "in what capacity on THIS project" — context, zero permissions
```

### Devin Recommendation

**Option A — first-party Spring Security:** OTP per FRS (+ optional password), JWT access (short-lived) + opaque DB-persisted refresh tokens with rotation/revocation; web via httpOnly cookies + CSRF token, mobile via bearer + secure enclave; one unified mechanism; admin/judge same system + MFA policy recommendation; social login deferred behind provider abstraction; **no Redis/IdP infra implied.**

### Open Questions

1. SMS/OTP provider + volume/cost (register Q5)
2. Password required in addition to OTP at MVP, or OTP-only?
3. MFA for admin/judge — required at MVP or P1? *(inference — not FRS)*
4. Session/TTL policies per surface
5. Managed IdP revisit trigger (scale/cost threshold)

## OD-07 — API Architecture / API Contract Strategy

| Field | Content |
|-------|---------|
| Decision ID | OD-07 |
| Decision | API architecture style and contract strategy for the modular monolith's external API. |
| Context | 4 client surfaces (web SPA, mobile, judge, admin) consume one backend; API boundaries must mirror OD-01 module boundaries and support future extraction. OD-02 accepted REST/OpenAPI capability at stack level — this OD defines the architecture/conventions. |
| FRS References | §5 surfaces, §9–13 user/portfolio/media/Connect/rooms, §15–24 competition pipeline, §25 notifications, §30 audit, §36 NFRs |
| Options | **A. REST + OpenAPI** · **B. GraphQL** · **C. RPC/gRPC** · **D. Hybrid** |
| Status | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| Decision Owner | Product + Technical Review |

**Review outcome (accepted):** REST + OpenAPI is the approved external API architecture. `ADR-007` will formalize after the OD sequence. **Binding guardrails:**

1. REST is the primary external API style.
2. OpenAPI is the canonical version-controlled API contract.
3. API namespaces use `/api/v1/{domain}`.
4. APIs are owned by their respective modular-monolith domains.
5. DTOs form the API boundary; persistence entities are never exposed directly.
6. RFC 9457 Problem Details is the standard error model.
7. Correlation IDs are supported across API operations.
8. Cursor/keyset pagination is used where appropriate for large/unbounded collections.
9. Offset pagination is available for appropriate bounded/admin collections.
10. Database constraints are preferred for idempotency where possible.
11. Idempotency-Key may be used for retry-sensitive operations where justified.
12. PostgreSQL may persist idempotency records; Redis is not implied.
13. Concurrency uses appropriate combinations of transactions, constraints, optimistic locking/versioning and explicit state transitions.
14. Media APIs remain storage-provider agnostic.
15. REST handles commands/history; WebSocket handles realtime events.
16. WebSocket protocol selection is deferred to OD-09.
17. API authorization is based on system roles and domain authorization rules; talent skills do not grant permissions.
18. Module boundaries must prevent direct cross-module persistence access.
19. GraphQL and gRPC are not selected as the primary external API.
20. API design must preserve future extraction capability without implementing microservices now.

### 1. API Architecture Style

**REST + OpenAPI, resource-oriented, module-owned.** Each backend module D1–D15 owns its API surface under a domain namespace (`/api/v1/{domain}/...`). Layering per module: controller → service → repository, with **DTO/entity separation** — entities never serialize to clients; API↔domain mapping is explicit. Cross-module interaction goes through internal contracts (queries/events), never another module's tables — preserving OD-01 extraction seams without becoming services.

### 2. Contract Source of Truth

**Recommendation: contract-in-code → generated canonical spec.** Controllers+DTOs (Bean Validation, springdoc) generate the OpenAPI spec at build; the generated spec is version-controlled and diff-checked in CI (breaking-change detection). This is *contract-first discipline with code-first ergonomics* — honest trade: pure contract-first (spec edited first) is stricter but adds a sync step a small team may not sustain. **Codegen clients:** both web (OD-04) and mobile (OD-05) consume generated TS types — generator tool (openapi-typescript/Orval-class) is an implementation choice, not approved here. Version-controlled spec lives in-repo.

### 3. API Versioning

**Recommendation: `/api/v1/…` URI versioning.** Rationale: mobile upgrade lag (OD-05 store cycles) needs explicit compat windows; URI versioning is visible, debuggable, CDN/proxy-friendly. Rejected for MVP: header/media-type versioning (complexity, less discoverable) and no-versioning (fragile). Deprecation policy: N/N−1 client support window *(inference — product should confirm)*; new major version only for breaking changes.

### 4. Resource & Endpoint Conventions

- Plural nouns, lowercase, hyphen-free (`/users`, `/talent-skills`, `/competition-rounds`)
- One-level nesting only for true containment: `/competitions/{id}/rounds`, `/submissions/{id}/evaluations` — deeper paths via parent-scoped top-level resources + filters
- Actions where nouns are insufficient: `POST /competitions/{id}/publish`, `POST /rounds/{id}/close`, `POST /submissions/{id}/submit`, `POST /projects/{id}/members`
- Identifiers: opaque UUIDs in URLs; no sequence leakage
- Status codes: 200/201/204, 400 validation, 401 unauthenticated, 403 unauthorized, 404, 409 conflict/state, 422 business-rule, 429 rate-limit, 500
- Example mapping (not a spec): `/users/{id}/talent-skills`, `/portfolios/{id}/media`, `/competitions/{id}/submissions`, `/projects/{id}/rooms`, `/competitions/{id}/votes`, `/evaluations`, `/rankings?competition=&round=`, `/notifications`

### 5. Error-Response Standard

**Recommendation: RFC 9457 Problem Details** (`application/problem+json`) — `type`, `title`, `status`, `detail`, `instance` + extensions: `code` (stable machine string), `errors[]` (field-level validation), `correlationId`. Standard for all surfaces; documented error catalog per endpoint.

### 6. Pagination

| Collection | Strategy | Justification |
|------------|----------|---------------|
| Discovery/feed, talent search, comments, messages, notifications | **Cursor/keyset** | Large, mobile infinite-scroll, stable ordering under inserts; keyset performs well on PostgreSQL |
| Competitions, submissions, rankings, admin lists | **Offset (page/size)** | Bounded, admin-facing, sortable tables need page-jumps |
| Rankings | Cursor on rank or offset — either; deterministic ordering + total count | Per-product UX |

Envelope: `{data[], pageInfo{cursor/nextPage}}` vs `{data[], page,size,totalElements}` — per strategy. No assumption of cursor everywhere.

### 7. Filtering / Sorting / Searching

Allowlisted query params per resource (`?skill=&status=&sort=`), validated server-side, **never widening authorization scope** — filters intersect with visibility rules `[FRS §9]`. Search = PostgreSQL FTS/trigram per OD-11 baseline; no dedicated engine implied by this decision. Field selection (`?fields=`): optional, defer unless payload sizes justify.

### 8. Idempotency

| Operation | Mechanism |
|-----------|-----------|
| Vote submission `[BR-14]` | **DB unique constraint** + upsert — idempotent by construction |
| Evaluation submit `[BR-12]` | Unique (judge×submission×round) + state transition |
| Submission create / media finalize / retried POSTs | **Idempotency-Key header** → key stored in PostgreSQL (`idempotency_keys` table, response replay) |
| Notification-triggering actions | Same-DB dedup on (event, recipient, window) |

No Redis: keys persist in PostgreSQL — consistent with "no Redis" constraint.

### 9. Concurrency / Optimistic Locking

- **Optimistic locking** (`version` column → `ETag`/`If-Match`) for user-visible updates (profile, submission draft, room settings)
- **Explicit state transitions** via action endpoints — no direct status-field writes (competition open→vote→evaluate→score→publish `[FRS §15]`)
- **Transactions + constraints** for single-entity invariants; row-level locks where contention is real (OD-03 guardrail)
- Admin overrides `[BR-13]`: same-transaction write + audit

### 10. AuthN/AuthZ Integration (OD-06)

Token → principal → permissions from `UserSystemRole` only; resource-scoping rules (judge-assigned-only `[FRS §19]`, room-membership, content visibility) enforced in service layer. **Boundaries preserved:** authN ≠ authZ ≠ TalentSkill ≠ ProjectContributionRole — skills/roles in payloads are data, never permissions.

### 11. Media API Contracts

Contract-level sequence (pipeline unimplemented): `POST /media` (metadata+intent) → `POST /media/{id}/upload-url` (pre-signed) → direct-to-storage PUT → `POST /media/{id}/complete` → `GET /media/{id}` (processing status) → visibility/moderation flags → `DELETE`. Provider-agnostic terms ("storage", "renditions") — no cloud vendor in contract. Consistent with MEDIA-ARCHITECTURE.

### 12. WebSocket Boundary

| Channel | Responsibility |
|---------|----------------|
| REST | All CRUD/commands/history: conversations list, send message (also acceptable), attachments metadata, read-marking fallback |
| WebSocket | Realtime events: `message.new`, `message.delivered`, `message.read`, optional `typing`, notification pushes, live counters (where enabled `[FRS §18]`) |

Handshake authenticated per OD-06 (token at connect); connection principal scoped by authz; reconnect → re-auth + resume via event-cursor (last-event-id); acks for send/delivery/read. Messaging stays an isolated module (OD-01 extraction seam). **No STOMP/Socket.IO/native-WS selection here** — transport choice is a separate decision (OD-09 realtime).

### 13. API Security

Bean Validation on every DTO; DTO-only binding (no mass assignment); CORS allowlist per environment; CSRF on cookie-authenticated mutations (OD-06); server-side rate limiting on auth/vote/upload endpoints (app-level counters — no Redis implied); request/media size limits; no sensitive data in URLs/logs; correlation ID required on responses; audit events for state-changing admin/scoring ops `[FRS §30]`.

### 14. API Observability

Correlation/request ID (accept-or-generate, propagated via MDC), structured JSON logs, latency + status + error metrics per endpoint, business-operation audit events kept **separate** from technical telemetry per OBSERVABILITY-ARCHITECTURE.

### 15. API Documentation

OpenAPI spec + rendered docs (per-surface views), authentication guide, error catalog, example payloads, changelog tied to versioning. Spec is the published contract.

### 16. Testing Strategy

Unit (services/mappers), controller-slice tests (validation/error mapping), integration (Testcontainers vs real PG), **contract tests** (spec conformance + generated-client compatibility), **authZ tests** (role matrix including `TalentSkill ≠ SystemRole` negatives), **concurrency tests** for vote/evaluation/score paths, negative/security tests (mass-assignment, injection, over-fetch).

### 17. Future Extraction Principles

- Module-owned `/api/v1/{domain}` namespaces — no cross-domain path mixing
- No shared persistence models across module APIs; DTO boundaries everywhere
- Cross-module reads via internal contracts/events — never direct table access
- Extraction candidate (e.g., D6 messaging) lifts out with its namespace intact — no consumer-facing change

### 18. Alternatives

| Approach | For | Against | Verdict |
|----------|-----|---------|---------|
| **REST + OpenAPI** | Universal clients, cacheable, codegen, mature tooling; matches resource-shaped FRS model | Chatty for composite views (mitigate: aggregated read endpoints) | **Recommended** |
| **GraphQL** | Flexible queries, one endpoint | AuthZ/caching complexity, N+1 risk, heavier stack, FRS resources are naturally REST-shaped — overkill | Rejected for MVP |
| **gRPC/RPC** | Efficient internal calls | Browser friction (needs gateway), less discoverable, wrong tool for public API | Rejected — possible internal option only if extraction happens |

### 19. Recommendation Summary

- **API architecture:** REST + OpenAPI, module-owned namespaces under `/api/v1`
- **Contract:** generated OpenAPI is canonical, version-controlled, CI-diffed; codegen TS clients for web + mobile (tool = implementation choice)
- **Versioning:** `/api/v1/` URI
- **Errors:** RFC 9457 Problem Details + `code`/`errors[]`/`correlationId`
- **Pagination:** cursor/keyset for high-volume feeds; offset for bounded/admin lists
- **Idempotency:** DB unique constraints first; `Idempotency-Key` header + PG key-store for retried creates
- **Concurrency:** optimistic `version`/ETag + explicit state-transition endpoints + transactions
- **Security:** DTO-only binding, validation, CORS/CSRF, rate limits, size limits, audit + correlation IDs
- **WS boundary:** REST = commands/history; WS = realtime events; protocol choice deferred to OD-09
- **Key trade-offs:** URI versioning adds maintenance vs invisible versioning; code-first spec risks drift (mitigated by CI diff); split pagination adds convention overhead vs uniformity
- **Risks:** spec drift, authz leaks via filters (mitigated by authZ tests), versioning discipline decay (mitigated by deprecation policy)
- **Extraction:** namespace-per-module + DTO boundaries + no shared tables = mechanical later extraction

### Open Questions

1. Client support window (N/N−1?) for mobile API versions — product input
2. OpenAPI generation: build-time (springdoc) vs spec-first authoring — confirm at implementation
3. `?fields=` sparse fieldsets needed, or fixed response shapes sufficient?
4. WS protocol selection deferred to OD-09 (STOMP vs Socket.IO vs native)

## OD-08 — Media Storage & Processing

| Field | Content |
|-------|---------|
| Decision ID | OD-08 |
| Decision | Media architecture: storage, upload, processing, delivery for video/audio/image/document `[FRS §10]` — provider-neutral. |
| Context | Media powers profile/portfolio `[§9]`, discovery `[§11]`, rooms `[§13]`, submissions `[§16]` and Connect `[§12]`; upload/publish lifecycle `[FRS §10]`; moderation `[§26]`; scalable storage required `[FRS §36]`. Largest infrastructure surface of the product. **FRS names no provider — all storage/delivery choices below are architecture recommendations.** |
| FRS References | §9, §10, §12, §13, §16, §26, §36 |
| Options | **A. Object storage + async processing + CDN delivery (provider-neutral interfaces)** · **B. All-in-one media platform (Cloudinary/Mux-class)** · **C. Self-managed pipeline (object store + ffmpeg workers)** · **D. DB BLOB storage** |
| Status | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| Decision Owner | Product + Technical Review |

**Review outcome (accepted):** Option A is the approved media architecture. `ADR-008` will formalize after the OD sequence. **Binding guardrails:**

1. Object storage is the primary binary media storage architecture.
2. PostgreSQL stores durable media metadata and relationships — **not** large media binaries.
3. Media uploads use authorized direct-to-object-storage transfers.
4. Spring Boot does **not** proxy media binaries.
5. Multipart/resumable uploads supported where media size/network conditions justify them.
6. Upload completion is verified by the backend.
7. Media processing is asynchronous.
8. MVP processing supports video, audio, image and document requirements identified in the review.
9. Media metadata and binary objects remain separate concerns.
10. Media access requires StarMitra authorization before signed delivery access is issued.
11. Signed URLs are appropriately short-lived.
12. CDN delivery is part of the target architecture — **no CDN provider selected**.
13. Storage and CDN providers remain provider-neutral until OD-12.
14. Competition submission media respects submission finalization and audit requirements.
15. Media integrates with moderation.
16. Media deletion/retention policies remain product decisions where not established by the FRS.
17. Storage object keys are backend-controlled — never derived from untrusted client input.
18. Orphaned-object reconciliation is required.
19. No Redis, Kafka, RabbitMQ or other infrastructure is implied.
20. Media processing tooling/provider selection remains separate from this decision.

### 1. Storage Architecture

| Approach | Verdict |
|----------|---------|
| **Object storage** | **Recommended** — purpose-built for binaries: durability, lifecycle rules, signed URLs, CDN-pairable; DB stores metadata only |
| DB BLOB | Rejected — bloats primary store, breaks backup cadence, no streaming/CDN, worst for video |
| Hybrid (BLOB for small files) | Rejected — added complexity for no gain |

### 2. Upload Architecture

`Client → POST /media (metadata+intent) → server issues pre-signed direct-upload URL → client PUTs to object storage → POST /media/{id}/complete → async processing → status via GET /media/{id}` (WS ready-event optional, OD-09 dependent)

- **Large files:** multipart upload support; resumable uploads via multipart/session resumption — *recommendation, FRS silent*
- **Retry/interruption:** client retries part-level failures; incomplete uploads expire via lifecycle policy
- **Verification:** server checks object existence/size/ETag at `/complete`; checksum (SHA-256/ETag) recorded
- **Duplicates:** content-hash dedup optional *(inference)*; same-file re-upload is harmless (new Media ID)

### 3. Media Processing — MVP vs Future

| Type | MVP scope | Deferred/future |
|------|-----------|-----------------|
| Video | Transcode to standard H.264/HLS renditions *(recommendation)*, thumbnail/poster, duration/resolution metadata | Per-scene detection, advanced quality ladders |
| Audio | Transcode to common bitrate, duration; waveform metadata **optional — not justified for MVP** | Waveforms, loudness |
| Images | Resize variants, thumbnails, format normalization, **EXIF strip** | AI tagging |
| Documents | Type validation + malware scan; preview **optional** | Full-text extraction |
| All | Async via internal job queue (monolith worker) — **no broker required at MVP** | GPU/ML pipelines |

*FRS requires upload/storage/playback `[§10]` — rendition/transcode specifics are architecture recommendations.*

### 4. Media Metadata — Principles (no schema yet)

`MediaAsset`: media ID, owner ID, media type + MIME, file size, storage reference (provider-neutral object key), duration, dimensions, **processing status**, **visibility**, **moderation status**, timestamps; `MediaVariant`: per-rendition info. Explicit attach links (`SubmissionMedia`, `PortfolioItemMedia`, `MessageAttachment`) per DATA-ARCHITECTURE. Metadata is the durable record — binaries are regenerable artifacts.

### 5. Access Control

| Media class | Access rule |
|-------------|-------------|
| Public (landing/discovery) | Public objects/CDN URL acceptable |
| Private/profile/portfolio | Visibility-gated → short-lived signed URLs, server-issued after authz check |
| Competition submissions | Per competition state + submission rules `[§16]`; judges see assigned entries `[§19]` |
| Creative Room assets | Members only `[§13]` |
| Message attachments | Conversation members only |
| Moderation/admin | System-role-gated; access audited |

**Principle:** media access never bypasses domain authz — server authorizes *before* issuing any URL; storage ACLs are a second layer, not the control plane.

### 6. Delivery

**Recommendation:** CDN-fronted signed URLs for non-public media; public-CDN for public assets; HLS for video *(recommendation)*. **Never proxy media through the API.** CDN/provider selection deferred to OD-12 — interfaces stay provider-neutral.

### 7. Security Controls

MIME+extension allowlists per type `[FRS §10]`; magic-byte sniffing (not extension-trust); size limits; malware scanning on upload-complete (async); `Content-Disposition: attachment` for non-renderable types; short signed-URL expiry; server-generated object keys (no user path control); EXIF/metadata stripping on publish; executable rejection; rate-limited upload initiation.

### 8. Moderation Integration

Recommended status model *(architecture recommendation — FRS doesn't enumerate states)*:

`Uploaded → Processing → Ready → {UnderReview → Approved|Rejected} → Removed`

Transitions driven by `[FRS §10][§26]` lifecycle + moderation events; `Rejected/Removed` revokes delivery; domain-owned states, not pipeline-owned.

### 9. Competition Media

- Attached via `SubmissionMedia`; association locked when submission finalizes `[§16]`
- Deadline enforced at submission level — media referenced after deadline can't attach
- Immutable evidence: submitted media links frozen; replacement blocked post-submission *(recommendation per `[FRS §16]` immutability)*
- Judge sees assigned submissions only; audience sees per-competition visibility config
- Retention per audit `[§30]` — *duration is a product policy, not architecture*

### 10. Messaging Attachments `[FRS §12]`

Members-only; per-conversation size limits; scanned like other media; thumbnails for images/video; deletion follows message deletion + retention *(product decision pending)*; delivered via signed URLs in message payload.

### 11. Performance & Scalability

Async worker tier decouples transcode bursts; object storage scales natively; CDN absorbs delivery load; lifecycle rules control storage cost; concurrent uploads are storage-native — no backend bottleneck. Single worker pool suffices for MVP — no premature infra.

### 12. Disaster Recovery

Object-store durability (11-9s class standard); metadata backed via PostgreSQL `[FRS §36]`; **reconciliation job** for orphaned objects/records; recovery = metadata restore + re-derive missing renditions on demand.

### 13. Privacy & Lifecycle

User-deleted → object delete + metadata tombstone; moderation-removed → revoke + retain per audit policy; competition retention per product policy; room dissolution per room policy; attachment lifecycle tied to conversation. **Product policies pending** — architecture supports any.

### 14–15. Alternatives & Provider Neutrality

Rejected: DB BLOB (wrong store), self-managed ffmpeg fleet (ops burden), media platform lock-in (egress-pricing risk for video-heavy platform). **Recommended: A** — all behind provider-neutral interfaces; concrete provider = OD-12's job.

### 16. API Alignment (OD-07)

Contract sequence matches OD-07 §11 exactly; statuses feed `GET /media/{id}`; WS ready-event optional (OD-09 transport pending).

### 17. Future Extraction

Media module isolated behind storage/processing adapters — extraction = lift module + storage config; object storage is already infra-independent. No redesign needed.

### Devin Recommendation

**Option A — object storage + direct-to-storage upload + async processing + CDN delivery, behind provider-neutral interfaces.** Processing via internal job queue (no broker at MVP); provider selection deferred to OD-12.

### Open Questions

1. CDN vs direct object delivery cost posture — tied to OD-12 provider choice
2. Video rendition ladder (resolutions/bitrates) — implementation detail
3. Document preview strategy — deferred to implementation
4. Attachment/room/media retention policies — product decisions pending
5. Content-hash dedup for duplicate uploads — evaluate at implementation

## OD-09 — Real-Time Communication

| Field | Content |
|-------|---------|
| Decision ID | OD-09 |
| Decision | Realtime transport/architecture for StarMitra Connect `[FRS §12]` and realtime surfaces — inside the OD-01 monolith, isolated for extraction. |
| Context | FRS §12 requires: 1:1 + group/project conversations, text + media attachments, timestamps, delivery + read status, project-linked conversations, notifications integration, report/block. **Future (out of MVP):** live video/audio, calls, live streaming, realtime collaborative editing `[FRS §4.2][§35]`. |
| FRS References | §12 Connect, §18 live vote counts (optional), §25 notifications, §35 priorities |
| Options | **A. WebSocket in the Spring Boot backend (isolated module)** · **B. Managed realtime platform (Pusher/Ably/Stream Chat-class)** · **C. SSE/long-polling only** |
| Status | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| Decision Owner | Product + Technical Review |

**Review outcome (accepted):** WebSocket in the Spring Boot monolith is the approved realtime architecture. `ADR-009` will formalize after the OD sequence. **Binding guardrails:**

1. WebSocket is the approved realtime transport capability.
2. Realtime messaging remains inside the Spring Boot modular monolith for MVP.
3. StarMitra Connect remains an isolated messaging module/domain.
4. PostgreSQL is the durable source of message state.
5. Message persistence occurs before realtime fan-out.
6. At-least-once delivery semantics with idempotent processing.
7. Exactly-once delivery is not claimed.
8. Server message IDs + clientMessageId support deduplication.
9. Per-conversation ordering where required.
10. REST handles commands/history/recovery; WebSocket handles realtime events.
11. Offline/reconnection recovery uses durable message state and REST recovery.
12. WebSocket authentication follows OD-06.
13. Per-event authorization and conversation membership checks are required.
14. Token/session revocation must be honored.
15. Talent skills never grant messaging permissions.
16. Media attachments use OD-08; binaries do not travel through WebSocket.
17. Typing/presence are not mandatory MVP capabilities.
18. Single-instance realtime is sufficient for MVP.
19. Multi-instance fan-out requires a separate architecture decision.
20. Redis/Kafka/RabbitMQ are not approved by OD-09.
21. STOMP/native WebSocket/Socket.IO protocol selection remains open pending an implementation spike.
22. Live video/audio/calls/streaming remain outside MVP.
23. Connect remains isolated for potential future extraction.

### 1. Realtime Scope

| MVP `[FRS §12]` | Future — explicitly out of scope |
|-----------------|--------------------------------|
| Messaging: 1:1, group/project, text, attachments, timestamps, delivery + read status, conversation updates, notification integration | Live video/audio, calls, live streaming, realtime collaborative editing, presence/typing *(not FRS-mandated at MVP — recommendations only if product wants them)* |

### 2. Approach Comparison

| Approach | For | Against | Verdict |
|----------|-----|---------|---------|
| **A. WebSocket in Spring Boot** | Bidirectional events; full control of auth/semantics; zero vendor cost; isolated module → extraction path (OD-01); Spring WebSocket native | You own connection lifecycle/scaling | **Recommended** |
| **B. Managed realtime platform** | Zero ops, SDKs solve receipts/presence | Per-connection/message pricing at chat scale; vendor lock-in on a core capability; new infra dependency requiring separate approval | Rejected for MVP |
| **C. SSE / polling only** | Simplest; server-push only | Weak for chat (client→server needs REST anyway); awkward delivery/read receipt semantics; poor mobile battery | Rejected — insufficient for chat UX |

**Transport/library sub-decision — kept open:** *Spring WebSocket (native WS)* vs *STOMP-over-WebSocket* vs *Socket.IO*. Trade-offs: STOMP gives built-in pub/sub + destination addressing (maps cleanly to conversations/topics) but heavier client libs; native WS is leanest (custom event protocol, more code); Socket.IO adds reconnect/fallback conveniences but brings a Node-style abstraction into Spring (Netty-socketio-class server exists but adds dependency). **Recommendation: evaluate STOMP-over-WebSocket first (natural fit for conversation-topic routing); finalize at implementation.** Not auto-approved.

### 3. Protocol Recommendation

**WebSocket (transport) — recommended.** Protocol detail (STOMP vs native frames) stays open pending a small implementation spike. Compatibility: browsers + React Native support WS natively; auth via handshake token per OD-06; reconnect = client-resume via REST history + event cursor.

### 4. Spring Boot Integration

Spring WebSocket/STOMP handlers inside the messaging module (D6); Spring Security at handshake (OD-06); REST APIs remain command surface; PostgreSQL is message store; module boundary enforced per OD-01 (messaging owns its tables; other modules read via contracts).

### 5. REST vs WebSocket Responsibilities

| REST (commands/history) | WebSocket (realtime events) |
|-------------------------|------------------------------|
| Conversation create/list, message history + pagination, attachment initiation, send-message (acceptable), block/report, admin ops | `message.new`, `message.delivered`, `message.read`, conversation updates, in-app notification events, live counters where enabled `[FRS §18]` |

*Typing indicators/presence:* **not FRS-mandated** — optional enhancement, separate product decision.

### 6. Delivery Semantics

**At-least-once transport + idempotent processing.** Message ID server-assigned (UUID); client carries a client-generated `clientMessageId` for dedup on retry (idempotent-send via unique constraint — OD-07 idempotency rules apply). Server ACK confirms persistence → delivered/read events are state transitions. **No exactly-once claims** — dedup makes it effectively-once.

### 7. Persistence vs Transport

**WebSocket is transport, not storage.** Messages persist to PostgreSQL first (`Conversation`, `ConversationParticipant`, `Message`, `MessageAttachment`, `MessageReceipt`), then fan-out to connected members. Offline clients get history via REST on resume. Durable state independent of connection state.

### 8. Ordering

**Minimum necessary: per-conversation ordering** via conversation-scoped sequence number (or timestamp+ID tiebreak). No global ordering; per-sender subsumed by conversation order. *Recommendation — FRS doesn't specify ordering.*

### 9. Reconnection

Client holds `lastSeenEventId`/message cursor; on reconnect → REST fetch `?after=<cursor>` → resume WS. Covers: network loss, mobile backgrounding, tab suspension, server restart. Missed-message recovery is a **read-path**, not replay — durable store is source of truth.

### 10. Delivery/Read Status

FRS requires sent/delivered/read states `[FRS §12]`. Implementation model *(recommendation)*: `MessageReceipt` per recipient (`deliveredAt`, `readAt`); single-user view shows aggregate. States are FRS requirements; the per-recipient receipt table is the architecture mechanism.

### 11. Authentication & Authorization

Handshake validates access token/session (OD-06); per-connection principal; **conversation-participant check** gates every event (join/send/receipt); suspended/blocked users → connection refused/closed on revocation `[FRS §8]`; project/room membership derived from domain authz — **talent skills grant no messaging permissions** `[BR-2]`.

### 12. Attachments (aligns with OD-08)

Message → `MessageAttachment` metadata → media upload via pre-signed URL (OD-08 flow) → attachment references media ID; WS message carries attachment *metadata*, never binaries. Access = conversation-membership + media visibility.

### 13. Notifications Interaction

In-app realtime notification = WS event (same channel). Push notification (when user offline) = notification service trigger (D13) — **push provider is a separate decision, not selected here.** Email/SMS deferred per NOTIFICATION-ARCHITECTURE.

### 14. Scaling

Single-instance WS is sufficient for MVP (chat volumes modest); multi-instance later needs **fan-out coordination** (sticky sessions or a pub/sub adapter — e.g., Redis/Kafka) — **explicitly a future dependency requiring a separate decision, not approved now.** Module isolation makes swapping in a distributed adapter a config-level change, not a rewrite.

### 15. Failure Handling

Disconnected client → messages queue in DB, delivered on resume; server restart → clients reconnect + REST catch-up; duplicates → idempotent dedup via clientMessageId; persistence failure → send fails with error (never "ghost-sent"); invalid authz → send rejected; expired creds → connection challenged/closed, re-auth.

### 16. Security

Handshake auth + per-event authz; connection limits per user; message size caps; rate limits on sends; block/report `[FRS §12]` enforced at authz layer; attachment validation per OD-08; idle-connection timeouts; resource-exhaustion guards; audit events for moderation-relevant actions `[FRS §30]`.

### 17. Future Extraction

Messaging module is OD-01's named extraction candidate: isolated tables, internal contracts for cross-domain reads, WS endpoint namespaced under `/ws/messaging`, REST under `/api/v1/conversations` — extraction = lift module + endpoint; domain code unchanged.

### 18. Alternatives Recap

Rejected: managed realtime platform (cost/lock-in), SSE/polling (insufficient), broker-backed distribution (premature), separate messaging service (violates OD-01). **Recommended: A.**

### Devin Recommendation

**Option A — WebSocket inside the Spring Boot monolith, isolated messaging module (D6), PostgreSQL persistence, at-least-once + idempotent dedup.** Protocol detail (STOMP vs native) deferred to a small implementation spike — not auto-selected.

### Open Questions

1. STOMP-over-WS vs native WS frames vs Socket.IO — implementation spike decides
2. Presence/typing indicators — product decision (not FRS)
3. Multi-instance fan-out mechanism — future infra decision when scale demands (ties to OD-10 cache / OD-12 deployment)
4. Push provider — separate decision (OD-12-adjacent)

## OD-10 — Cache

| Field | Content |
|-------|---------|
| Decision ID | OD-10 |
| Decision | Does StarMitra need caching — and at which layer — for MVP? Redis is explicitly NOT pre-approved. |
| Context | FRS demands correctness for votes/evaluations/scores `[BR-14][FRS §22]` — caching must never corrupt authoritative state. No FRS requirement mandates sub-ms reads or shared caching. OD-09 did not approve Redis; this OD decides whether any cache is needed at all. |
| FRS References | §18 voting, §20 rubrics, §22 scoring, §24 leaderboards, §36 NFRs |
| Options | **A. No distributed cache — in-process cache for hot reference data only** · **B. Redis/Valkey** · **C. Managed cache (ElastiCache-class)** |
| Status | **ACCEPTED — PENDING FINAL ADR FORMALIZATION** |
| Decision Owner | Product + Technical Review |

**Review outcome (accepted):** No distributed cache for MVP. `ADR-010` will formalize after the OD sequence. **Binding guardrails:**

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
16. Cache technology/library selection is not part of OD-10 implementation.

### 1. Is Caching Actually Required?

Layers that already satisfy most "cache" needs without distributed infrastructure:

| Layer | Covers |
|-------|--------|
| PostgreSQL + indexes + query optimization | Nearly all MVP read paths — competition metadata, profiles, rubrics are small, hot, indexed |
| JVM in-process cache (Caffeine-class via Spring `@Cacheable`) | Reference data (skill taxonomy, competition config, rubric versions — immutable anyway) |
| HTTP/CDN caching (Cache-Control, ETag) | Public discovery/feed responses, static media delivery |
| Browser/mobile caching | Client-side asset/feed caching |

**MVP verdict:** single-instance monolith + indexed PostgreSQL handles every required path. **No distributed cache is required for MVP.**

### 2. Cache Candidates — Classification

| Candidate | Classification |
|-----------|----------------|
| Skill taxonomy, competition categories, published rubric versions | **Useful** — in-process (immutable/config-scale) |
| Active competition metadata, round state | **Useful** — in-process, short TTL + explicit invalidation on transitions |
| Public talent profiles, discovery/feed | Useful — HTTP/CDN first; in-process if needed |
| Leaderboards/read models | **Future optimization** — PG materialized views/rollup tables first; Redis ZSET only if load demands |
| Notification counts | Unnecessary — cheap indexed count queries |
| Configuration | Useful — in-process (boot-time load + refresh) |
| User-specific responses | **Never shared-cache** — see §8 |

### 3. Technology Comparison

| Option | MVP scale | Multi-instance | Invalidation | Ops cost | Failure | Verdict |
|--------|-----------|----------------|--------------|----------|---------|---------|
| **None + PG** | Sufficient | N/A | None | Zero | Simple | Baseline |
| **In-process (Caffeine-class)** | Sufficient | Per-instance inconsistency for mutable data — acceptable only for immutable/short-TTL data | TTL + explicit evict | Zero | Falls back to DB | **Recommended** |
| **Redis** | Overkill | Real shared cache | Complex | Moderate | New failure mode | **Deferred — triggers defined** |
| **Managed cache** | Same as Redis + vendor | Same | Same | $$$ | Same | Deferred |

### 4. Cache vs Database — Authoritative Boundaries

PostgreSQL is sole source of truth. **Never cached as authoritative:** votes, submissions, judge evaluations, scores, rankings, competition state, messages, audit records, security/session state *(separate approval required for any session caching)*.

### 5. Consistency Strategy (when in-process cache is introduced)

Cache-aside + short TTL for config/reference data; explicit eviction on admin mutation events; no write-through/write-behind needed at MVP (no cache-owned writes, ever). Stale-while-revalidate acceptable for public discovery only.

### 6. Competition-Specific Concerns

| Data | Rule |
|------|------|
| Active competition metadata/round state | Cacheable in-process, short TTL, **invalidated on state transition** |
| Live voting counts | **Never authoritative-cache** — aggregates computed on read or via rollup job; display may lag, decisions must not |
| Score/ranking freshness | Computed transactional; cached *display* snapshots only after publication `[FRS §22]` |
| Competition closing/round transitions | Always read live state — never serve from cache during transitions |

### 7. Realtime Interaction

WS connection state stays in-process (single-instance MVP per OD-09); message fan-out is in-memory within the module; presence/notification counts need no shared cache. **Multi-instance fan-out = future decision** (explicitly deferred in OD-09).

### 8. Security

Private profiles/media/submissions/judge data/moderation/authz decisions: **never shared-cache** — private responses are per-user and authz-scoped; any caching must be keyed per-user + invalidated on authz change. Shared caches serve only public/reference data.

### 9. HTTP/CDN vs Application vs Distributed

Distinct layers: HTTP/CDN handles edge caching of public content (`Cache-Control`, ETag, conditional GET); in-process handles hot reference data; distributed cache would only serve multi-instance scale. Don't conflate — each has its own invalidation model.

### 10. Failure Behavior

In-process cache unavailable → direct DB reads (correct, slower); stale entries bounded by TTL + explicit eviction; restart = cold cache rebuilt lazily; poisoning prevented by validation at write; invalidation failure degrades to TTL expiry. **Cache is always optional — DB is the fallback.**

### 11. Recommendation

**Option A — no distributed cache for MVP.** In-process caching (Spring `@Cacheable`/Caffeine-class — *library choice at implementation*) for hot reference data only; HTTP/CDN for public content; PG authoritative for everything else. **Redis deferred with explicit adoption triggers:**

1. Multi-instance deployment needs shared rate-limiting / WS fan-out coordination
2. Measured hot-read/leaderboard load exceeds what PG + replicas handle
3. A concrete feature (sessions-at-scale, distributed locks) demands it

Each trigger is a separate decision — Redis is **not** approved by this OD.

### Open Questions

1. Which job-queue library OD-02 adopts (if it brings Redis-class primitives, cache may arrive as byproduct — still requires the trigger review)
2. Leaderboard freshness SLA — product input
3. Notification-count pattern (badge polling vs push) — product input

## OD-11 — Search

| Field | Content |
|-------|---------|
| Decision ID | OD-11 |
| Decision | Search architecture for MVP + triggers for future dedicated search infrastructure. |
| Context | `[FRS §11]` requires talent search by name/skill, feed/discovery, "advanced search by skills/categories" as P1 `[FRS §35]`; genre/category popularity in reports `[§29]`. Elasticsearch/OpenSearch NOT pre-approved. |
| FRS References | §11 discovery/feed/search, §35 P1 advanced search, §29 analytics |
| Options | **A. PostgreSQL-native search** (FTS + trigram + relational filters) · **B. Dedicated engine** (Elasticsearch/OpenSearch/Meilisearch-class) · **C. Managed search service** (Algolia-class) |
| Status | **ACCEPTED IN PRINCIPLE — PENDING FINAL ADR FORMALIZATION** |
| Decision Owner | Product + Technical Review |

**Review outcome (accepted in principle):** PostgreSQL-native search for MVP. `ADR-011` will formalize after the OD sequence. **Binding guardrails:**

1. PostgreSQL remains authoritative.
2. PostgreSQL FTS (`tsvector`/`tsquery` + GIN) and `pg_trgm` are used only where justified.
3. Search must remain distinct from discovery/feed/recommendation.
4. Search must enforce authorization/privacy/moderation/visibility rules.
5. TalentSkill and ProjectContributionRole must never be treated as authorization mechanisms.
6. Search relevance must be **deterministic and documented** — no undocumented ranking formula.
7. Advanced autocomplete, faceting, semantic/AI search and ML ranking remain deferred unless explicitly required by product scope.
8. Elasticsearch, OpenSearch, Algolia, Redis, Kafka, RabbitMQ and a dedicated search service remain **unapproved** for MVP.
9. Explicit future migration triggers for a dedicated search engine are documented (§4).
10. Conceptual searchable entities/fields are documented (§4a) — no implementation.

### 1. Search Requirements — FRS vs Inference

| Requirement | FRS | Inference |
|-------------|-----|-----------|
| Search talents by name/skill `[§11]` | ✓ FRS | |
| Feed/discovery by skill/category `[§11]` | ✓ FRS | |
| Content/competition discovery `[§11]` | ✓ FRS (browse/filter) | |
| Advanced search (skills/categories) | ✓ FRS — but **P1** `[§35]` | |
| Keyword text search across content | | *inference — not explicitly required* |
| Typo tolerance, autocomplete, facets | | *inference — not FRS* |
| Relevance ranking | | *inference* |

FRS-supported core = **filter-browse + name/skill lookups**. Everything beyond that is a product enhancement.

### 2. PostgreSQL Capabilities for MVP

| Capability | Covers |
|------------|--------|
| B-tree + composite indexes | Skill/category/status filters, competition browse, ordered lists |
| `tsvector`/`tsquery` FTS + GIN | Text search on titles/descriptions/bios |
| `pg_trgm` similarity + `ILIKE` | Fuzzy name matching, partial/substring, near-typo tolerance |
| Relational filtering + joins | All scoped searches (competition×category×skill) |
| `ts_rank` + weights | Simple relevance ordering |

### 3. Alternatives Comparison

| Criterion | PostgreSQL-native | Dedicated engine (ES/OS-class) | Managed search (Algolia-class) |
|-----------|-------------------|--------------------------------|--------------------------------|
| MVP capability fit | Covers all FRS-supported needs | Exceeds | Exceeds |
| Relevance quality | Basic ts_rank | Strong | Strongest |
| Typo/autocomplete | Trigram approximates | Native | Native |
| Faceting | Manual SQL | Native | Native |
| Infra/ops | Zero (same DB) | New cluster + sync pipeline | Vendor + sync pipeline |
| Consistency | Immediate (same tx) | Eventual (index lag) | Eventual |
| Consistency with authz/moderation | Trivial (same query scope) | Index must carry authz/visibility flags — drift risk | Same risk |
| Cost | None added | Ops/cluster or vendor bill | Per-record pricing |
| Failure modes | None new | Stale index, sync lag, divergence | Same + vendor outage |

### 4. MVP Recommendation

**PostgreSQL is the initial search engine.** Scope:

- **Supported:** name/skill/title lookups, all filters/sorts, FTS on text fields, trigram fuzzy names
- **Indexing:** B-tree/composite/GIN on queryable columns; `tsvector` generated columns where needed
- **Limitations (stated honestly):** weaker relevance ranking, no native faceting, limited typo-tolerance, no managed synonyms — acceptable at MVP scale
- **Migration triggers → dedicated engine:** (i) P1 "advanced search" requirements land `[FRS §35]`, (ii) measured query latency/index-maintenance cost exceeds PG comfort, (iii) autocomplete/faceted discovery become product priorities

**"PostgreSQL first" ≠ "PostgreSQL forever"** — search is behind a query abstraction (§12).

### 4a. Conceptual Searchable Entities/Fields — documented, not implemented

| Entity | Searchable fields (conceptual) | Indexed/filters |
|--------|-------------------------------|-----------------|
| Talent/User | name, bio | skills, status, visibility |
| TalentSkill | name | — |
| Competition | title, description | category/skill, status, dates |
| Submission/Media | title, description, content type | competition, category, visibility, moderation status |
| Project/Creative Room | title, description | type, member scope |
| Feed content | title/caption text | skill, content type, recency |

Relevance is **deterministic and documented**: `ts_rank` (FTS) → recency → engagement counts; fixed ordering, no hidden formula.

### 5. Search vs Discovery Feed

Distinct concerns. **Discovery/feed** `[FRS §11]` is not keyword search — it's a ranked/filtered browse surface driven by domain read models (D5). **Recommendation:** feed = dedicated **read model/projection queries on PG** (denormalized display views, materialized where heavy) — *not* a recommendation engine; FRS names no personalization/recommendation requirement. Any future ranking algorithm is a separate product decision.

### 6. Search Indexing

**Simplest correct approach for MVP:** search fields are columns on the same tables (generated `tsvector` columns or views); indexes updated **transactionally** — no async pipeline, no sync drift, no broker. Async indexing is only relevant when a dedicated engine arrives (future decision).

### 7. Consistency

**Immediate consistency** — same transaction, same DB: new/updated/hidden/removed content reflects instantly; moderation/visibility changes take effect with the write. Eventual-consistency risk only appears *if* a dedicated engine is adopted later.

### 8. Authorization & Privacy

Search queries carry the same authz scoping as domain reads: private profiles excluded unless authorized `[§9]`; moderated/removed content filtered (`status` predicates); blocked users' content excluded; room-internal content never searchable by non-members `[§13]`. **Search never widens visibility** — enforced at query layer + tested (OD-07 authZ test matrix).

### 9. Relevance/Ranking

MVP = `ts_rank` + recency/popularity signals (likes/follows counts — FRS-supported engagement `[§4]`). No ML ranking — not FRS-supported, unjustified. Limitations acknowledged: ranking is basic; refinement = future work if product signals need.

### 10. Autocomplete & Typo Tolerance

**Not FRS requirements.** `pg_trgm` delivers acceptable partial-match for MVP. True autocomplete/typo-tolerance = future enhancement, and alone does **not** justify a search engine.

### 11. Performance

MVP search load is modest (browse+lookup, not high-QPS query). Indexed filters + GIN-FTS perform well at MVP volume. Triggers for concern: measurable p95 latency on search paths, or FTS index-maintenance cost under write load. Caching: none needed (OD-10); results computed live.

### 12. Future Extraction

Search sits behind a **query abstraction in D5** (read-model contracts: `SearchQuery` → `SearchResult`). Swapping PG → Elasticsearch later = implement the same contract against the engine; **domain DB stays authoritative** — engine is a disposable projection rebuilt from source-of-truth.

### 13. Failure Handling

Search unavailable → browse paths still work; heavy query timeout → circuit-breaker + simplified filter mode; PG is source of truth so there's no "stale index" at MVP. Future-engine divergence handled by rebuild-from-source when/if adopted.

### 14. Security

Validated/allowlisted filter params (OD-07); query length/wildcard caps; rate-limited search endpoints; authz filters mandatory in every search query; no sensitive data in indexed/searchable fields.

### 15. Devin Recommendation

**Option A — PostgreSQL-native search** (FTS + trigram + relational filters + generated tsvector columns) for MVP; discovery feed = PG read-model projections, not a recommendation engine; dedicated engine deferred with explicit triggers (P1 advanced-search landing, measured latency, facet/autocomplete product need).

### Open Questions

1. P1 "advanced search" scope — what does it require that PG can't provide? (shapes migration trigger)
2. Search-result UX expectations — does product want autocomplete at launch?
3. Discovery feed personalization expectations — is basic recency/popularity ordering sufficient, or does product want a ranking model later?

## OD-12 — Cloud / Deployment

| Field | Content |
|-------|---------|
| Decision ID | OD-12 |
| Decision | Cloud platform + deployment architecture for all environments (dev/test/UAT/staging/production). |
| Context | All prior decisions shape this: OD-01 monolith, OD-02 Spring Boot, OD-03 PostgreSQL, OD-04 Vite SPA, OD-05 RN+Expo, OD-06 auth, OD-07 REST API, OD-08 media (provider-neutral), OD-09 WS (single-instance MVP), OD-10 no distributed cache, OD-11 PG-native search. FRS requires backup/recovery + security `[§30][§36]`. **FRS names no cloud provider — provider choice is deferred pending Product Owner input.** |
| FRS References | §5 channels, §36 NFRs (scale, availability, backup, observability), §30 audit |
| Options | **A. Major cloud, managed services** (AWS/GCP/Azure — container platform + managed PG + object storage + CDN) · **B. PaaS** (Render/Fly.io/Railway-class) · **C. VPS/self-managed** (own Docker+PG) |
| Status | **ACCEPTED IN PRINCIPLE — PENDING FINAL ADR FORMALIZATION** |
| Decision Owner | Product + Technical Review |

**Review outcome (accepted in principle, post-refinement):** Option A is the approved deployment *architecture* — managed container platform + Spring Boot modular monolith + managed PostgreSQL + object storage + CDN, single-region MVP, no Kubernetes. `ADR-012` will formalize after the OD sequence. **Binding guardrails:**

1. Deployment architecture is provider-independent — **cloud provider selection remains OPEN** (no AWS/Azure/GCP lock-in in this decision).
2. **Kubernetes is not approved** — deferred to documented triggers (§18). No Redis/Kafka/RabbitMQ/ES/OS/service mesh implied.
3. Environment model is explicitly the 5-logical-environment model (§3) — no ambiguity.
4. Background jobs: application-managed mechanism inside the monolith; **in-memory-only queues must not be authoritative for business-critical work**; conceptual job requirements defined (§10); no broker.
5. Single-instance WebSocket preserved (OD-09); **HTTP replicas do NOT imply WS fan-out** — multi-instance WS fan-out is a separate future decision.
6. **Version-controlled Infrastructure as Code** is the principle — Terraform is the current *recommended implementation option*, not an immutable requirement.
7. **RTO/RPO remain open** — FRS specifies no numerical targets; none invented.
8. **MVP infrastructure budget remains an open Product Owner decision.**
9. All prior decisions (OD-01…OD-11) preserved unchanged.
10. All deferred technologies retain explicit future triggers (§18).

### 1. Deployment Model (conceptual topology)

| Component | MVP deployment |
|-----------|----------------|
| Backend (modular monolith) | Container image → managed container platform (single service, horizontal replicas possible) |
| Web frontend (Vite SPA) | Static build → CDN/object-hosted statics — no server runtime needed |
| PostgreSQL | **Managed service** (automated backups, patching, replica option) |
| Media | Provider-neutral object storage (OD-08) + CDN |
| WebSocket | Same monolith process — **single instance suffices at MVP** (OD-09) |
| Async/background | Internal job queue (Spring `@Async`/scheduler) + worker threads **inside the same deployable** — no broker |
| Scheduled/watchdog | Scheduled jobs in monolith (media orphan reconciliation, competition close, score compute triggers) |

### 2. Cloud Platform Options

| Option | For | Against | Verdict |
|--------|-----|---------|---------|
| **A. Major cloud managed services** | Every needed primitive (container runtime, managed PG, object storage, CDN, secrets, CI/CD, monitoring); media-scale capable; startup credits; scales with product | Complexity + pricing nuance (egress) | **Recommended** — provider pick = separate sub-decision (credits/pricing) |
| **B. PaaS (Render/Fly/Railway-class)** | Fastest deploys, minimal ops | Media-bandwidth unit economics; ceilings at competition scale; re-platforming later | Rejected beyond alpha/testing |
| **C. VPS/self-managed** | Cheapest raw compute | Own DB HA/backups/patching/media pipeline — ops burden a small team can't afford | Rejected for MVP |
| VM-based within A | Control | More ops than containers; unnecessary | Rejected in favor of managed containers |
| **Kubernetes** | Orchestration power | Massive overkill for a monolith — no demonstrated multi-service scale | **Not approved — deferred to scale trigger** (§18) |

### 3. Environment Model — explicitly 5 logical environments

The model is **5 logical environments** (not ambiguous). **Test/QA is intentionally combined with CI-ephemeral testing** — at MVP, ephemeral test environments satisfy both CI-test and QA-validation needs; a dedicated always-on Test/QA environment is not justified until a dedicated QA process demands it. Rationale documented explicitly:

| # | Logical environment | Form | Purpose |
|---|--------------------|------|---------|
| 1 | **Local Development** | Local Docker; developer machines | Day-to-day dev |
| 2 | **Ephemeral Test** (combines CI-ephemeral + Test/QA) | CI-spun disposable envs | Automated + QA validation |
| 3 | **UAT** | Small persistent managed env | Product validation `[FRS §37]` |
| 4 | **Staging** | Prod-shaped minimal (same topology, small sizes) | Pre-release verification |
| 5 | **Production** | Managed services, single region | Live |

If a dedicated always-on Test/QA environment is later needed (dedicated QA team, long-running env), it becomes a sixth environment — product/ops decision.

### 4. Availability & Scalability — MVP

- Backend: stateless container, 2 replicas for availability (not capacity); horizontal scale via platform autoscaling if needed
- Frontend: static CDN — inherently scalable
- PostgreSQL: managed single-primary + automated backups; **read replica only if reporting load demands** (trigger, not default)
- Media: storage/CDN scale natively
- WS: single backend instance at MVP per OD-09 — **HTTP replicas do NOT imply WS fan-out is solved; multi-instance WS fan-out requires a separate architecture decision** *(future trigger)*
- Failure: container auto-restart; health checks; PG managed failover *(managed-service dependent)*

### 5. Database Deployment

**Managed PostgreSQL** — automated backups (PITR via WAL), patching, monitoring, optional replica. Self-managed rejected (ops burden, no need). Preserves OD-03.

### 6. Media (preserves OD-08)

Provider-neutral object storage + pre-signed upload + async processing + CDN delivery — all already architecture-approved; provider selection stays with OD-12's open provider question.

### 7. Realtime (preserves OD-09)

Single-instance WS at MVP confirmed. **Multi-instance fan-out trigger** = when concurrent-connection count or availability needs force a second app instance → needs coordination adapter (separate decision, no broker approved).

### 8. Cache (preserves OD-10)

No distributed cache; in-process only; Redis deferred.

### 9. Search (preserves OD-11)

PG-native FTS; no ES/OS/Algolia; dedicated-service trigger documented.

### 10. Messaging/Event Infrastructure + Background Jobs

**No broker at MVP.** Async work = application-managed job mechanism inside the monolith. Broker (Kafka/RabbitMQ) enters only if a future scale scenario demands durable distributed eventing — **documented trigger, not approved.**

**Conceptual requirements for the application-managed job mechanism** *(architecture requirements — no implementation/framework selected)*:

- **Durable job state where business-critical** — job records persisted (PostgreSQL); **in-memory-only queues must not be authoritative for business-critical work**
- Retry with bounded attempts + backoff; **idempotency** on job handlers (dedup via keys/constraints per OD-07)
- Duplicate-execution protection; explicit job status tracking
- Failure handling: dead-letter/failed-job records, alerting
- Graceful shutdown (drain in-flight jobs) + restart recovery (resume persisted jobs)
- Transaction boundaries: job work within transactions; business write + audit atomic where required `[FRS §30]`
- Scheduled job execution (competition close, scoring triggers, reconciliation) + watchdog processing
- PostgreSQL remains the authoritative store for job state; no Redis/broker implied

### 11. Kubernetes / Orchestration

**Not justified for MVP** — one monolith container doesn't need an orchestrator. Managed container platforms (ECS Fargate/Cloud Run/App Service-class) give autoscaling + health checks + rolling deploys without K8s ops. **K8s trigger:** multiple independently-deployable services or cluster-level orchestration requirements — future decision.

### 12. CI/CD Principles (not implemented)

Source control (Git, feature branches → PR); build (container image + Vite static bundle); automated tests in CI; artifact registry; deploy via managed-container rollout; rollback = redeploy prior image/tag; config per environment; **Flyway migrations run at deploy time** (OD-02/03); environment promotion = immutable artifacts dev→staging→prod; **infrastructure via version-controlled IaC** *(principle — Terraform is the current recommended implementation option, not an immutable requirement; tooling choice is a separate decision)*.

### 13. Secrets & Configuration

Environment-specific config via env vars/parameter store; secrets in managed secrets store (never in repo/code); rotation supported; per-environment secret isolation; no shared prod/non-prod credentials.

### 14. Observability (preserves baseline §18)

Structured JSON logs; metrics (JVM/HTTP/DB); correlation IDs across API ops (OD-07); request tracing where the platform supports it; health endpoints + deployment health checks; audit events stay **separate** from technical telemetry. **No monitoring vendor selected.**

### 15. Backup & DR

PG automated backups + PITR; object-store durability (11-9s class); media metadata backed via PG; restore testing as a scheduled practice; retention per product policy; **RTO/RPO targets not invented — proposed/open for product input** (single-region MVP; multi-region deferred).

### 16. Security/Networking

TLS everywhere (terminated at LB/CDN); private subnet for DB (no public exposure); backend behind load balancer only; storage access via signed URLs; least-privilege service roles; per-env isolation; admin access via audited, restricted paths.

### 17. Cost & Operational Complexity

| Option | MVP suitability | Ops complexity | Scale | Reliability | Cost |
|--------|-----------------|----------------|-------|-------------|------|
| A. Major cloud managed | Strong | Medium | High | High | Medium (optimize via credits) |
| B. PaaS | Good early | Low | Medium | Medium | Low→rising |
| C. VPS | Weak (ops burden) | High | Low | Medium | Low $, high effort |

### 18. Future Scaling Triggers (each = separate decision)

| Technology | Trigger condition |
|------------|-------------------|
| Kubernetes | Multiple independently-deployable services; orchestration complexity justifies |
| Redis/Valkey | OD-10 triggers: multi-instance rate-limiting/WS fan-out, measured hot-read load, shared-state feature |
| Kafka/RabbitMQ | Durable distributed eventing requirement at scale |
| Elasticsearch/OpenSearch | OD-11 triggers: P1 advanced search, measured latency, facet/autocomplete need |
| Multi-instance WS fan-out | Concurrent connections / availability force second instance |
| Read replicas | Measured reporting/read contention on primary |
| DB partitioning | Table-volume growth (`Vote`/`AuditLog`/`Notification`) |
| Multi-region | Latency/DR requirement beyond single-region tolerance |
| CDN expansion | Delivery latency/coverage gaps |
| Dedicated worker infra | Media-processing load exceeds in-app worker capacity |

### Devin Recommendation

**Option A — one major cloud, managed services:** containerized monolith on a managed container platform + managed PostgreSQL + object storage + CDN + managed secrets/monitoring; **version-controlled IaC** (Terraform = current recommended implementation option — not immutable); CI/CD per §12; **5-logical-environment model** (Local Dev, Ephemeral Test, UAT, Staging, Production); single-region MVP. **Provider (AWS/GCP/Azure) remains OPEN** — this OD establishes the deployment architecture independent of vendor; provider selection requires Product Owner input on credits/pricing.

### Open Questions (Product Owner)

1. **Cloud provider choice** — existing commitments/credits/pricing preference (Q1)
2. RTO/RPO expectations — FRS silent; product input needed
3. Budget ceiling / cost posture for MVP infra
4. Single vs dual-region appetite (deferred anyway — but posture matters for provider choice)
5. UAT/staging environment persistence cadence

## OD-13 — Analytics

| Field | Content |
|-------|---------|
| Decision ID | OD-13 |
| Decision | Analytics architecture for MVP: operational reporting vs product analytics vs future data platform. |
| Context | `[FRS §29]` requires admin analytics + reporting dashboards; `[FRS §30]` audit is separate from analytics. No FRS requirement mandates a warehouse, event streaming, or third-party analytics platform. |
| FRS References | §27 admin dashboards, §29 reporting/analytics, §30 audit, §36 observability |
| Options | **A. PostgreSQL operational reporting** (queries + read models/materialized views) · **B. + first-party event capture (DB-persisted)** · **C. Third-party analytics platform (PostHog/Mixpanel/GA-class)** · **D. Warehouse/data platform** |
| Status | PROPOSED — PENDING PRODUCT/TECHNICAL REVIEW |
| Decision Owner | Product + Technical Review |

### 1. FRS Analytics Requirements

`[FRS §29]` explicitly requires: **registered users, active creators, skill/category popularity, uploads/engagement, competition participation, voting activity, judge completion, per-criterion score averages, round progression, collaboration statistics, top-talent metrics.** All are *operational aggregates of system-of-record data* — none require a warehouse. *Inference:* product-level funnels/retention/feature-usage analytics are not FRS requirements — flagged separately.

### 2. Analytics vs Operational Data — Boundaries

| Layer | Store | Rule |
|-------|-------|------|
| Transactional business data | PostgreSQL | **Authoritative** |
| Reporting/read models | PostgreSQL (views/materialized/rollup tables) | Derived, refreshable — never authoritative |
| Analytics events | PG table (if first-party capture is adopted) | Telemetry, not facts |
| Audit `[FRS §30]` | PostgreSQL `AuditLog` | **Separate** — compliance trail, never mixed with analytics |
| **Competition results** | PostgreSQL | Analytics can NEVER override authoritative records |

### 3. MVP Analytics Architecture

| Approach | Verdict |
|----------|---------|
| PG queries/read models/materialized views | **Recommended** — all §29 metrics are derivable aggregates |
| Async aggregation jobs (rollups) | Recommended where heavy (leaderboards, per-criterion averages) — in-app job mechanism (OD-12 §10) |
| Separate analytics datastore | Rejected at MVP — no justification |
| Warehouse/lake | Rejected — premature |
| Managed analytics platform | Deferred — product decision |

### 4. Event Model — do we need one?

FRS doesn't require product-event tracking. *Recommendation:* **deferred** — if first-party product analytics is wanted later, a minimal DB-persisted event record (name, actor, entity, properties JSONB, timestamp) with naming/versioning conventions; dedup via event ID; ordering by timestamp+ID; retention per product policy. **Not implemented/approved now** — architecture notes only.

### 5. Privacy & Data Governance

- Analytics aggregates **non-content, non-private** operational metrics; private user content is never an analytics input
- Reporting surfaces enforce admin-role authz; judge-identity data handled per `[FRS §19]` scoping
- Data minimization: aggregate-first; row-level personal data excluded from analytics surfaces
- Deletion/anonymization follows product data-retention policy *(product decision — FRS silent)*; retention/deletion of analytics aggregates inherits source-data lifecycle
- Legal/compliance review (consent, residency) flagged as **separate review — not invented here**

### 6. Competition Analytics

All §29 competition metrics (submissions, participation, voting, judge evaluations, scoring, ranking, qualification, round progression) read from authoritative records — aggregates are *derived views*; scoring/ranking outputs remain computed by the scoring engine, not by analytics. Analytics reports on outcomes; it never produces them.

### 7. Performance — where queries run

| Load | Approach |
|------|----------|
| Admin dashboards, light aggregates | Direct PG queries (indexed) |
| Heavy/recurring aggregates | Read-model or materialized-view rollups (refresh via scheduled job) |
| Competition-peak protection | Report on replica **only if measured contention demands** (OD-12 trigger) |

### 8. Realtime/Dashboards

Admin/judge dashboards: **synchronous queries + periodic aggregation** — sufficient for §29. Realtime dashboards are not FRS-required; live counters (where admin-enabled `[§18]`) come via OD-09 WS events, not an analytics pipeline.

### 9. Product vs Operational Analytics

Distinct layers: **operational observability** (logs/metrics/traces — baseline §18, *not analytics*); **business/competition analytics** (§29 aggregates — this OD's MVP scope); **product usage analytics** (funnels/retention — deferred to a product decision + possible future tool).

### 10. Third-Party Analytics

**None approved.** GA/Mixpanel/Amplitude/PostHog/Segment/Snowflake/BigQuery/Redshift/ClickHouse all deferred — each requires product justification + privacy review. If adopted, they operate *alongside* PG, never as authoritative.

### 11. Future Scale Triggers (each = separate decision)

| Technology | Trigger |
|------------|---------|
| Separate analytics datastore | §29 query load degrades OLTP despite rollups |
| Data warehouse | Cross-source/historical BI questions emerge |
| Event streaming (Kafka-class) | High-volume first-party event capture required |
| CDC | Analytics needs change-data-capture feeds |
| Dedicated analytics pipeline | Multi-source ingestion becomes necessary |
| BI platform | Non-engineers need self-serve reporting |
| Real-time analytics infra | Live operational dashboards become product requirement |

### 12. Security

Analytics never bypasses authz/privacy/moderation/competition-access controls — derived surfaces enforce the same role scoping; aggregate-only where privacy demands.

### 13. Relationship to Existing Decisions

Preserves OD-01 (analytics lives in D15 module), OD-03 (PG only), OD-07 (reporting APIs in `/api/v1/admin`), OD-10 (no cache-authoritative analytics), OD-11 (search ≠ analytics), OD-12 (no new infra). **No Redis/Kafka/RabbitMQ for analytics.**

### 14. Devin Recommendation

**Option A — PostgreSQL operational reporting** (direct queries + read-model/materialized rollups in D15, scheduled aggregation jobs) covering all FRS §29 metrics. First-party event capture deferred pending product need; third-party analytics + warehouse deferred with documented triggers. Status proposal: **ACCEPTED IN PRINCIPLE** upon review.

### Open Questions (Product Owner)

1. Is product-usage analytics (funnels/retention) wanted at MVP — and if so, first-party capture or a tool?
2. Reporting freshness SLA — live vs scheduled rollups acceptable?
3. Analytics data-retention/anonymization policy (FRS silent)
4. Legal/consent review for any future event tracking

---

## Open Questions Requiring Product Owner Input

| # | Question | Blocks |
|---|----------|--------|
| Q1 | Existing cloud commitments/credits (AWS/GCP/Azure/other)? | OD-12 provider selection, OD-08 provider pick |
| Q2 | Expected MVP scale: users, concurrent voters during competition windows, media upload volume? | OD-01, OD-09, OD-10, OD-12 sizing |
| Q3 | Team skills today: TypeScript? Python? Java? | OD-02, OD-04, OD-05 |
| Q4 | iOS and Android both required at MVP, or Android-first? | OD-05 scope |
| Q5 | SMS/OTP volume expectations + budget (India-primary SMS aggregators vs IdP bundled OTP)? | OD-06 |
| Q6 | Media budget posture: managed platform premium acceptable vs engineering time? | OD-08 |
| Q7 | Any compliance/residency constraints on user data or media? | OD-06, OD-08, OD-12 |
| Q8 | Product analytics appetite — is funnel/retention insight wanted at MVP or later? | OD-13 |

## Next Step

Review each OD in this register. On approval, the decision moves to `Status: ACCEPTED`, an `ADR-NNN` document is created, and the baseline's Open Decisions table is updated. No implementation begins until decisions are accepted.
