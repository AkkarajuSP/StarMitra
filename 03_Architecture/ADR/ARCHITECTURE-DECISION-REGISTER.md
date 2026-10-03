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
| OD-03 | Primary database | **PostgreSQL** — detailed review completed; already inside OD-02 accepted stack | PROPOSED — PENDING REVIEW (recommendation: accept) |
| OD-04 | Web frontend | React + Next.js (one framework, all four surfaces) | PROPOSED — PENDING REVIEW |
| OD-05 | Mobile technology | React Native (Expo) — Flutter strongest alternative | PROPOSED — PENDING REVIEW |
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
| Status | PROPOSED — PENDING PRODUCT/TECHNICAL REVIEW (recommendation: accept) |
| Decision Owner | Product + Technical Review |

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
| Decision | Web technology for Public Web + Creator + Admin Portal + Judge Portal `[FRS §5][§27][§28]`. |
| Context | Public discovery needs SEO/crawlability `[FRS §11]`; admin/judge portals are authenticated SPA-like apps; brand/design system must be implementable (verified palette, Poppins/Inter proposed); accessibility required. |
| FRS References | §5 channels, §11 discovery, §27–28 portals, §34 screens |
| Options | **A. React + Next.js (one framework, SSR public + app router portals).** **B. React SPA (Vite) + static pre-render for public pages.** **C. Vue + Nuxt.** **D. Angular.** **E. Svelte/SvelteKit.** |
| Advantages | **A:** one framework serves all four surfaces; SSR/SSG where SEO matters, client rendering where not; largest ecosystem/hiring; RSC/route handlers flexible; design-system friendly (Tailwind/CSS-in-JS/component libs). **B:** simplest mental model; cheap static hosting. **C:** excellent DX, similar capability to A. **D:** batteries-included, strong for enterprise forms-heavy admin. **E:** smallest bundles, fast. |
| Disadvantages | **A:** complexity budget (caching semantics, RSC learning curve); Vercel-centric docs (self-hostable). **B:** SEO needs extra machinery (prerender/SSR service); two rendering models anyway. **C:** smaller hiring pool than React. **D:** heavier for public consumer surfaces; opinionated = slower customization. **E:** smallest ecosystem/talent; enterprise libraries thinner. |
| StarMitra Fit | **A:** public discovery + 2 internal portals + creator surface = one codebase, shared design system, per-surface rendering mode — strongest fit. **C** credible if team is Vue-fluent. **D** overkill for consumer side, plausible for portal-only. |
| Team Impact | A/C: one framework, one design system → shared components across surfaces (judge portal reuses form system for dynamic rubric UI §20). B: split rendering approaches. |
| Cost/Complexity | A moderate (one framework, some platform learning); B low but hidden SEO cost; D higher ramp; E lowest runtime cost, higher ecosystem risk. |
| Risks | A: over-engineering public site (mitigate: static-first pages). Team unfamiliarity → ramp time. |
| Devin Recommendation | **A — React + Next.js**, deployed as: public site (SSR/SSG), creator app + admin + judge (auth'd app surfaces, possibly one codebase with route groups or three thin apps sharing a design-system package). Final packaging (one app vs multiple) is an implementation detail — the framework decision is the same. |
| Status | PROPOSED — PENDING PRODUCT/TECHNICAL REVIEW |
| Decision Owner | Product + Technical Review |

## OD-05 — Mobile Technology

| Field | Content |
|-------|---------|
| Decision ID | OD-05 |
| Decision | Mobile strategy for the mobile-first Audience+Creator app `[FRS §5]`. |
| Context | Requires camera/microphone media capture & upload, push notifications, smooth feed/discovery UX, chat, competition flows `[FRS §5][§10][§12][§25]`; Android+iOS eventually; offline niceties; small team. |
| FRS References | §5 mobile app, §10 media, §12 messaging, §25 notifications |
| Options | **A. React Native (+Expo).** **B. Flutter.** **C. Native (Kotlin + Swift).** **D. PWA only.** |
| Advantages | **A:** shares TypeScript/React mental model + some logic with OD-04 web; one team; Expo eases camera/media/notifications/OTA updates; mature (Meta, Shopify-class usage). **B:** best-in-class UI consistency + performance (own renderer); excellent animation; single codebase incl. possible web/desktop. **C:** peak performance; zero framework impedance for camera/media edge cases. **D:** zero install friction; one codebase; instant updates. |
| Disadvantages | **A:** bridge/native-module edge cases; heavy media editing feels less native; performance good not great. **B:** Dart = separate language/ecosystem from web stack; hiring smaller; can't share TS logic. **C:** two codebases, two skill sets — ~2x cost for a small team. **D:** iOS PWA limits (push landed but constrained); no store presence; weaker camera/media UX; doesn't feel "mobile-first premium". |
| StarMitra Fit | **A:** aligns with OD-02/OD-04 (TypeScript everywhere) — shared types, shared devs; covers all FRS mobile needs including media upload + push. **B:** strong if UI polish is judged paramount and team accepts Dart. **C:** right answer at 10x scale, wrong at MVP. **D:** insufficient for FRS mobile-first intent. |
| Team Impact | A: web devs contribute directly. B: learn Dart (easy, but separate). C: hire/staff 2 tracks. D: none added. |
| Cost/Complexity | A lowest-effective given TS stack; B similar raw cost but splits languages; C highest; D lowest but fails product goals. |
| Risks | A: niche media edge cases need native modules (Expo dev-client mitigates). B: second-language overhead long-term. Both: app-store review cycles slow iteration (OTA mitigates A; CodePush-class for both). |
| Devin Recommendation | **A — React Native with Expo** — given OD-02/OD-04 = TypeScript/React. If review favors UI-perfection over stack unity, **B (Flutter)** is the credible challenger. Reject C for MVP; D doesn't meet FRS intent. |
| Status | PROPOSED — PENDING PRODUCT/TECHNICAL REVIEW |
| Decision Owner | Product + Technical Review |

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
