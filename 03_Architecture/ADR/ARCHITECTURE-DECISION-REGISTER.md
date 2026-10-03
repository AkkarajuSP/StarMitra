# StarMitra — Architecture Decision Register

**Version:** Draft v0.1 | **Parent:** [Architecture Baseline v1.0](../Architecture/STARMITRA-ARCHITECTURE-BASELINE-v1.0.md)
**Functional baseline:** StarMitra FRS v1.1 — Multi-Talent / Multi-Skill Model

## Purpose

Single working register for all major StarMitra architecture decisions (OD-01 … OD-12). Every entry presents genuine alternatives with real trade-offs — no decision is pre-decided.

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
| OD-05 | Mobile technology | **React Native (Expo)** — creator/audience scoped app; Flutter strongest alternative | PROPOSED — PENDING REVIEW |
| OD-06 | AuthN/identity | Managed identity provider w/ phone OTP + JWT/refresh; RBAC internal | PROPOSED — PENDING REVIEW |
| OD-07 | Media storage/processing | Object storage + CDN + managed transcoding behind adapter | PROPOSED — PENDING REVIEW |
| OD-08 | Real-time | WebSocket (Socket.IO-class) inside backend for MVP | PROPOSED — PENDING REVIEW |
| OD-09 | Cache | Not required for MVP — defer Redis until a concrete trigger | PROPOSED — PENDING REVIEW |
| OD-10 | Search | PostgreSQL FTS + trigram for MVP; dedicated engine later | PROPOSED — PENDING REVIEW |
| OD-11 | Cloud/deployment | Containerized on one major cloud; provider chosen on cost/credits | PROPOSED — PENDING REVIEW |
| OD-12 | Analytics | Operational reporting from transactional DB + lightweight product analytics; defer warehouse | PROPOSED — PENDING REVIEW |

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
| FTS `tsvector` + `pg_trgm` (powers OD-10 DB-search recommendation), table partitioning, LISTEN/NOTIFY, materialized views (leaderboards/reporting), generated columns | **Optional — adopt when the dependent feature lands** |
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

**MySQL/MariaDB** are credible substitutes — StarMitra's model would work — but weaker on JSONB-class config modeling, advanced indexing (partial), window-function ergonomics for ranking, and FTS for OD-10. **SQL Server** is technically capable; licensing makes it unjustified for a startup. **Distributed SQL** answers scale questions StarMitra doesn't have yet. **Document DBs** conflict with the inherently relational FRS §31 model and move integrity into application code.

### 10. Recommendation

**PostgreSQL — recommend ACCEPT.**

- **Advantages:** exact fit for the relational FRS model; strongest constraint/transaction toolkit for the competition pipeline; JSONB covers config-driven entities without a second store; FTS seeds OD-10; free/OSS with the widest managed-service availability; seamless in the accepted Spring Boot stack (HikariCP, Flyway, Testcontainers, Spring Batch).
- **Trade-offs:** JSONB ≠ schemaless document store (not needed); sharding is manual if ever needed (not needed at MVP); advanced features (partitioning, LISTEN/NOTIFY, CDC) arrive only via separate decisions.
- **Risks:** (i) over-reliance on a single DB for search/analytics later — mitigated by OD-09/10/12 staging triggers; (ii) heavy reporting queries contending with OLTP — mitigated by read replica + rollups before competition peaks; (iii) migration drift — mitigated by Flyway discipline (accepted) + transactional DDL.
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
- **Playback:** `<video>`/`<audio>` + HLS where transcoded renditions exist; responsive images via variant URLs; document preview strategy open (OD-07 dependent).
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
| Status | PROPOSED — PENDING PRODUCT/TECHNICAL REVIEW |
| Decision Owner | Product + Technical Review |

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
| Decision | Authentication + identity approach: OTP auth, sessions/tokens, provider choice. |
| Context | `[FRS §8]` mobile/email + OTP "or configured authentication mechanism"; password recovery where enabled; future social login; account states Active/Suspended/Blocked/Deactivated; **critical: TalentSkill ≠ SystemRole** — authN and authZ are separate `[BR-02]`; judge/admin may warrant stricter policy. |
| FRS References | §6 roles, §8 authN, §30 audit, §36 security |
| Options | **A. Managed identity provider w/ phone OTP** (Cognito, Firebase Auth, Auth0, Supabase-class). **B. In-house OTP module** (own challenge/verify + SMS aggregator). **C. Hybrid** — provider for users, in-house for judge/admin or vice versa. |
| Advantages | **A:** battle-tested OTP/rate-limit/recovery/token machinery; MFA options for admin/judge; less custom security code; social login nearly free later. **B:** full control of UX + costs (SMS aggregator pricing); no vendor lock-in; OTP logic simple enough in principle. **C:** tailor per-surface security (e.g., stronger IdP MFA for admins). |
| Disadvantages | **A:** per-MAU pricing grows with audience scale; provider quirks; migration pain if swapping. **B:** security surface you must own — OTP throttling, replay, enumeration, token issuance/recovery bugs are breach vectors; social login still external work. **C:** two auth systems to secure/maintain — complexity without clear gain at MVP. |
| StarMitra Fit | **A:** FRS explicitly allows "configured authentication mechanism"; OTP-first is supported by Firebase/Cognito-class providers; keeps team focused on product. **B:** viable if SMS cost at scale is a deciding factor — but requires senior security review. **C:** premature. |
| Team Impact | A: least auth code to own; B: ongoing auth-security ownership; C: double. |
| Cost/Complexity | A: moderate $$ per MAU, low complexity. B: low $$ (SMS only), high risk/complexity. C: highest. |
| Risks | A: vendor cost at audience scale → mitigation: keep identity behind adapter; exportable user store. B: OTP/abuse bugs → mitigation: senior review, proven libraries only. Either way: **authZ stays internal** — roles/permissions never delegated to provider claims alone; enforce UserSystemRole server-side `[BR-02]`. |
| Devin Recommendation | **A** — managed identity provider supporting phone-OTP + email; JWT access + refresh tokens; **all authorization stays in-app via UserSystemRole**. Keep auth behind an abstraction so a swap (or adding in-house OTP later for cost) doesn't touch domain code. Admin/Judge: same provider + mandatory MFA policy. If cost review favors B, it must pass dedicated security review first. |
| Status | PROPOSED — PENDING PRODUCT/TECHNICAL REVIEW |
| Decision Owner | Product + Technical Review |

## OD-07 — Media Storage & Processing

| Field | Content |
|-------|---------|
| Decision ID | OD-07 |
| Decision | Media pipeline: storage, processing, delivery for video/audio/images/documents `[FRS §10]`. |
| Context | Upload→validate→process(transcode/thumbnail/scan)→publish→CDN delivery with visibility-gated access `[FRS §9][§10]`; moderation hooks `[FRS §26]`; scalable delivery `[FRS §36]`. Largest infrastructure surface of the product. |
| FRS References | §9 portfolio, §10 media lifecycle, §16 submission media, §26 moderation, §36 scalable storage |
| Options | **A. Cloud object storage + CDN + managed transcoding** (S3/GCS+CDN + MediaConvert/Transcoder-class, behind adapter). **B. All-in-one media platform** (Cloudinary/Mux-class). **C. Self-managed pipeline** (object store + ffmpeg workers). |
| Advantages | **A:** best cost-control at scale; composable; keeps media-agnostic core; signed-URL delivery fits visibility model. **B:** fastest time-to-market (upload→delivery solved incl. adaptive streaming, thumbnails, basic moderation); excellent DX. **C:** max control, lowest unit cost at volume, no vendor premium. |
| Disadvantages | **A:** you assemble transcode/scan/orchestration — moderate build effort. **B:** pricing scales steeply with bandwidth/minutes — dangerous for a video-heavy talent platform; lock-in. **C:** you own ffmpeg ops, scaling, failures, format edge cases — real ops burden for small team. |
| StarMitra Fit | **A:** matches MEDIA-ARCHITECTURE design; video is core content so per-minute platform pricing (B) is risky long-term; C is honest about ops cost. **B:** acceptable for earliest MVP if speed dominates and caps are set. |
| Team Impact | A: moderate pipeline code; B: least code; C: dedicated ops attention. |
| Cost/Complexity | A: medium build, low unit cost. B: low build, high unit cost. C: low unit cost, high ops cost. |
| Risks | A: provider choice must not leak into domain code (adapter + storage abstraction). B: bill shock under competition traffic spikes. C: pipeline outages block submissions `[FRS §16]` — needs watchdog alerts (OBSERVABILITY §3). |
| Devin Recommendation | **A** — object storage + CDN + managed transcoding, all behind an adapter interface. Choose the concrete provider with OD-11 (same cloud for egress efficiency). Revisit B only if MVP timeline is extremely tight; avoid C for MVP. |
| Status | PROPOSED — PENDING PRODUCT/TECHNICAL REVIEW |
| Decision Owner | Product + Technical Review |

## OD-08 — Real-Time Communication

| Field | Content |
|-------|---------|
| Decision ID | OD-08 |
| Decision | Transport/architecture for StarMitra Connect messaging `[FRS §12]` and near-real-time surfaces. |
| Context | **MVP:** 1:1 + group/project conversations, delivery/read status, project-linked threads `[FRS §12]`; notification push optional `[FRS §25]`; live vote counts only where admin-enabled `[FRS §18]`. **Future (P2):** calls, live streaming `[FRS §4.2][§35]`. |
| FRS References | §12 Connect, §18 vote counts, §25 notifications, §35 priorities |
| Options | **A. WebSocket in the backend** (Socket.IO-class, in monolith). **B. Managed realtime platform** (Pusher/Ably/Stream Chat/Firebase RTDB-class). **C. Polling/SSE only.** |
| Advantages | **A:** no vendor cost; fits monolith; full control of events/auth; Socket.IO handles reconnect/fallback; delivery+read receipts straightforward. **B:** zero realtime ops; SDKs handle presence/typing/receipts; some offer moderation. **C:** simplest; no persistent connections; adequate for notifications. |
| Disadvantages | **A:** you own connection scaling, sticky sessions, heartbeat/reconnect edge cases; horizontal scaling needs adapter (e.g., Redis pub/sub — interacts with OD-09). **B:** per-connection/message pricing at scale; chat UX lock-in; less control. **C:** chat latency perceived as "not real-time"; read receipts awkward; mobile battery with polling. |
| StarMitra Fit | **A for MVP:** chat is core `[PD-07]`; WS module isolated in monolith, extractable later (OD-01 hybrid path). B justified only if ops capacity is near zero. C fails UX expectation for chat. **Future capability:** live video/calls explicitly P2 — no platform for it now. |
| Team Impact | A: one WS module + adapter complexity when multi-instance. B: least ops. C: none. |
| Cost/Complexity | A low-medium; B low build/high recurring; C low. |
| Risks | A: connection-scaling pain later → mitigate: isolate messaging module behind clean interface; add Redis adapter only when multi-instance (ties to OD-09 trigger). B: lock-in rewrites. |
| Devin Recommendation | **A — Socket.IO-class WebSocket inside the backend** for MVP chat + optional live counters; **defer** any managed realtime/broker platform until multi-instance scaling or feature needs (presence, typing indicators at scale) justify it. Messaging stays an isolated module for future extraction (OD-01). |
| Status | PROPOSED — PENDING PRODUCT/TECHNICAL REVIEW |
| Decision Owner | Product + Technical Review |

## OD-09 — Cache

| Field | Content |
|-------|---------|
| Decision ID | OD-09 |
| Decision | Does StarMitra need a dedicated cache (e.g., Redis) in MVP? |
| Context | Candidate uses: session storage, rate limiting, competition config reads, leaderboards, hot profiles, temporary data. FRS demands correctness for votes/scores `[BR-14][FRS §22]` — caching must never corrupt results. |
| FRS References | §18 voting, §22 scoring, §24 leaderboards, §36 NFRs |
| Options | **A. No dedicated cache for MVP** (DB + app-level memoization). **B. Redis/Valkey from day one** for rate limiting + sessions + hot reads. **C. Managed cache** (ElastiCache/MemoryDB/Upstash-class). |
| Advantages | **A:** zero extra infra; DB is single source of truth; no invalidation bugs; simplest. **B:** fast rate-limit counters, ephemeral locks, leaderboard ZSETs, session store, WS adapter (OD-08) when multi-instance. **C:** same as B without ops. |
| Disadvantages | **A:** rate limiting lives in app memory (per-instance inaccuracy) or DB writes; hot reads hit DB. **B:** another moving part to run/secure/back-up; invalidation discipline needed; premature if traffic is modest. **C:** same + vendor cost. |
| StarMitra Fit | **A for MVP:** no FRS requirement demands sub-ms reads; competition config is small/hot and memoizable in-process; vote/score correctness favors direct DB writes. **B earns entry when:** (i) multi-instance deploy makes in-memory rate-limiting/WS broadcast insufficient, or (ii) leaderboard/hot-read load is measured, or (iii) job queue needs Redis-class primitives (BullMQ does!). Note: if OD-02 adopts BullMQ, Redis arrives anyway — making B nearly free. |
| Team Impact | A: none. B/C: small ops/monitoring addition. |
| Cost/Complexity | A zero; B/C small infra + correctness discipline. |
| Risks | Cache-serve stale competition state/leaderboards → integrity bugs. **Never cache:** pending vote/evaluation writes, rubric versions (immutable anyway), authZ decisions (or keep TTL seconds-level). Failure mode: cache down → degrade to DB, never fail closed on reads. |
| Devin Recommendation | **A — not required for MVP as a standalone decision**; HOWEVER if OD-02's job queue (BullMQ-class) brings Redis along anyway, use it *then* for rate limiting + WS adapter + hot reads — i.e., "cache arrives as a byproduct, not as a decision." Document invalidation rules when introduced. |
| Status | PROPOSED — PENDING PRODUCT/TECHNICAL REVIEW |
| Decision Owner | Product + Technical Review |

## OD-10 — Search

| Field | Content |
|-------|---------|
| Decision ID | OD-10 |
| Decision | Search capability approach for MVP `[FRS §11]`. |
| Context | Search across talent names, skills, content, projects, competitions + category/skill and content-type filters `[FRS §11]`; advanced search is P1 `[FRS §35]`. |
| FRS References | §11 feed/discovery/search, §35 P1 advanced search |
| Options | **A. Database search** (PostgreSQL FTS `tsvector` + `pg_trgm` + structured filters). **B. Dedicated engine** (OpenSearch/Elasticsearch, Meilisearch, Typesense, or hosted Algolia). **C. Hybrid** — DB for filters, engine for relevance. |
| Advantages | **A:** zero extra infra; same transactions/backups; filters are trivially relational; good-enough ranking for MVP scale. **B:** superior relevance, typo-tolerance, facets, instant search, scale. **C:** best of both when needed. |
| Disadvantages | **A:** weaker relevance/typo/fuzzy matching; ranking tuning is manual; heavy queries on primary DB (mitigate: read replica). **B:** extra infra/ops or vendor bill; index-sync pipeline from system of record; eventual-consistency edge cases (deleted content surfacing). **C:** both costs. |
| StarMitra Fit | **A for MVP:** FRS search needs are filter-browse + name/title lookups — PG covers them honestly. **B trigger:** when typo-tolerance/faceted discovery becomes a product differentiator or query load demands it — likely P1 "advanced search" `[FRS §35]`. Design read models (D5) so a dedicated engine can be swapped in behind the same query contract. |
| Team Impact | A: none extra. B/C: index pipelines + ops. |
| Cost/Complexity | A lowest; B medium-high; C highest. |
| Risks | A: search quality disappoints vs consumer expectations — mitigate early with trigram + curated facets; B: index drift → sync jobs + reconciliation. |
| Devin Recommendation | **A — PostgreSQL FTS + `pg_trgm` + structured filters** for MVP; dedicated engine deferred to P1/advanced-search phase with explicit adoption criteria (volume, relevance complaints, facet needs). |
| Status | PROPOSED — PENDING PRODUCT/TECHNICAL REVIEW |
| Decision Owner | Product + Technical Review |

## OD-11 — Cloud / Deployment

| Field | Content |
|-------|---------|
| Decision ID | OD-11 |
| Decision | Cloud provider + deployment architecture for dev/testing/UAT/staging/production. |
| Context | Needs: containerized app hosting, managed PostgreSQL (OD-03), object storage+CDN+transcode (OD-07), CI/CD, secrets, monitoring, backups/DR, scaling, cost control. FRS requires backup/recovery `[FRS §36]`. |
| FRS References | §36 NFRs (scale, backup, observability) |
| Options | **A. Major cloud, managed services** (AWS / GCP / Azure — compute via ECS/Cloud Run/App Service-class). **B. PaaS** (Render/Fly.io/Railway/Heroku-class). **C. VPS/self-managed** (Hetzner/DigitalOcean-class + own Postgres/media). |
| Advantages | **A:** every needed managed primitive exists; scales with product; media/CDN/transcode ecosystem mature; enterprise-ready path; credits for startups. **B:** fastest deploys, minimal ops, predictable bills early. **C:** cheapest raw compute; full control. |
| Disadvantages | **A:** complexity, pricing nuance (egress!), needs IaC discipline. **B:** ceilings on media scale/networking; unit economics worsen with video bandwidth; migration later is non-trivial. **C:** you own DB HA/backups/security patching — real ops burden; media pipeline DIY. |
| StarMitra Fit | **A:** media-heavy + competition-scale + future Originals ambitions argue for real cloud; single-region MVP keeps cost sane. **B:** legitimate for a throwaway/fast alpha; risk re-platforming at first competition scale event. **C:** ops distraction a small team can't afford. |
| Team Impact | A: needs IaC/cloud competence (Terraform + GitHub Actions). B: near-zero ops. C: highest. |
| Cost/Complexity | A medium (optimize: managed PG, S3-class storage, CDN, container compute, free-tier credits). B low→rises with scale. C low $$$, high time. |
| Risks | A: provider pick without pricing model → egress/bandwidth bill shock at media scale (evaluate AWS vs GCP vs Azure on *media egress + transcode* pricing, not familiarity). B: outgrow mid-competition. Any: multi-region DR deferred consciously. |
| Devin Recommendation | **A — one major cloud, containerized services, managed DB/storage/CDN, Terraform IaC, GitHub Actions CI/CD, four environments** (dev/staging/prod + local). **Provider selection deferred:** evaluate AWS vs GCP vs Azure on media egress/transcode pricing + available startup credits before locking — that sub-decision needs Product Owner input on budget/credits. Avoid B for anything beyond alpha; avoid C entirely for MVP. |
| Status | PROPOSED — PENDING PRODUCT/TECHNICAL REVIEW |
| Decision Owner | Product + Technical Review |

## OD-12 — Analytics

| Field | Content |
|-------|---------|
| Decision ID | OD-12 |
| Decision | Analytics architecture: what runs on the transactional system vs dedicated analytics for MVP. |
| Context | `[FRS §29]` needs: registered users, active creators, skill popularity, uploads/engagement, competition participation, voting activity, judge completion, per-criterion score averages, round progression, collaboration stats, top-talent metrics. Three distinct layers must not be conflated. |
| FRS References | §29 reporting/analytics, §27 admin dashboards, §30 audit |
| Options | **A. Operational reporting on transactional DB** (admin queries + read replica/materialized views). **B. + Lightweight product analytics** (PostHog/Mixpanel/Amplitude-class event tracking). **C. Data warehouse/analytics platform** (BigQuery/Snowflake/Redshift + dbt + BI). |
| Advantages | **A:** zero new infra; admin KPIs are direct aggregates of system-of-record; always consistent. **B:** product insight (funnels, retention, feature usage) engineering can't get from aggregates; cheap/free tiers; fast. **C:** real BI, historical modeling, cross-source analysis. |
| Disadvantages | **A:** heavy queries compete with OLTP (mitigate: replica/scheduled rollups); not a product-analytics tool. **B:** vendor events pipeline; consent/privacy handling; another tool. **C:** serious build+ops+cost; unjustified before product-market scale. |
| StarMitra Fit | **A+B for MVP:** §29 metrics are mostly operational aggregates → A; product analytics → B cheaply. **C deferred** until data volume/BI questions justify. Audit `[FRS §30]` stays in transactional store regardless — it's not analytics. |
| Team Impact | A: report queries + dashboard UI work (D15). B: event taxonomy + SDK integration effort. C: data-engineering ownership. |
| Cost/Complexity | A low; B low-medium; C high. |
| Risks | A: reporting queries degrade OLTP at competition peaks → read replica + off-peak rollups. B: privacy/consent for tracking — align with `[FRS §9]` privacy posture; anonymize judge-affecting data. Skipping B → blind product decisions. |
| Devin Recommendation | **A + B:** §29 operational reporting directly on transactional DB (read replica + materialized/scheduled rollups in D15); add lightweight product-analytics tool for funnels/retention; **defer warehouse/data platform** to a post-MVP ADR. |
| Status | PROPOSED — PENDING PRODUCT/TECHNICAL REVIEW |
| Decision Owner | Product + Technical Review |

---

## Open Questions Requiring Product Owner Input

| # | Question | Blocks |
|---|----------|--------|
| Q1 | Existing cloud commitments/credits (AWS/GCP/Azure/other)? | OD-11 provider selection, OD-07 provider pick |
| Q2 | Expected MVP scale: users, concurrent voters during competition windows, media upload volume? | OD-01, OD-08, OD-09, OD-11 sizing |
| Q3 | Team skills today: TypeScript? Python? Java? | OD-02, OD-04, OD-05 |
| Q4 | iOS and Android both required at MVP, or Android-first? | OD-05 scope |
| Q5 | SMS/OTP volume expectations + budget (India-primary SMS aggregators vs IdP bundled OTP)? | OD-06 |
| Q6 | Media budget posture: managed platform premium acceptable vs engineering time? | OD-07 |
| Q7 | Any compliance/residency constraints on user data or media? | OD-06, OD-07, OD-11 |
| Q8 | Product analytics appetite — is funnel/retention insight wanted at MVP or later? | OD-12 |

## Next Step

Review each OD in this register. On approval, the decision moves to `Status: ACCEPTED`, an `ADR-NNN` document is created, and the baseline's Open Decisions table is updated. No implementation begins until decisions are accepted.
