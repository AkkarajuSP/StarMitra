# StarMitra Architecture Baseline v1.0

**Status:** Draft for review — Phase 2 Architecture Baseline
**Functional baseline:** [StarMitra FRS v1.1 — Multi-Talent / Multi-Skill Model](../../01_Requirements/FRS/StarMitra_FRS_v1.1_MultiTalent.docx)
**Scope:** Architecture and design only. No application implementation, no production migrations, no unapproved technology commitments.

## Decision Status Legend

Every significant statement in this document carries one of the following statuses:

| Label | Meaning |
|-------|---------|
| `[FRS §n]` / `[BR-xx]` | Functional requirement or business rule directly established by FRS v1.1 — binding unless a product-level change is approved |
| `[PD-xx]` | Approved product decision captured in FRS v1.1 §40 — binding |
| `[Proposed]` | Proposed technical/architecture decision — NOT yet approved; requires ADR review |
| `[Accepted]` | Approved by product/technical review — ADR formalization pending |
| `[Open]` | Open decision — insufficient information; must be resolved before dependent work |

---

## 1. Purpose

This document establishes the architecture baseline for StarMitra: the agreed set of product constraints from FRS v1.1, the proposed technical structure that satisfies them, and an explicit record of which decisions are settled versus still open. It is the reference against which module design, database design, API design, and implementation are reviewed.

## 2. Architecture Principles

| # | Principle | Status | Rationale |
|---|-----------|--------|-----------|
| AP-1 | FRS v1.1 is the functional source of truth; deviations require explicit product review | `[FRS §42]` | Stated in FRS traceability clause |
| AP-2 | Five core concepts are modeled separately: User, System Role, Talent Skill, Project Contribution Role, Competition Participation | `[PD-02][PD-03]` | Multi-talent model is the defining product constraint |
| AP-3 | Creative skills never imply authorization | `[BR-02]` | Prevents privilege escalation through profile data |
| AP-4 | Configuration over code: skill taxonomy, competition categories, rubrics, scoring weights, voting rules, round behavior are data-driven | `[FRS §7][§15][§18][§20][§22]` | Admin must reconfigure without deployments |
| AP-5 | Immutability where history matters: published rubric versions, submitted evaluations, vote records, submissions' competition/round association | `[FRS §16][§20][§21][BR-09][BR-10]` | Auditability and scoring integrity |
| AP-6 | Coherent domain architecture over service proliferation: one deployable backend with explicit domain modules for MVP | `[Proposed]` | Team scale and FRS coupling (e.g., scoring spans voting + judging) favor a modular monolith; see SYSTEM-ARCHITECTURE |
| AP-7 | Auditability is a first-class cross-cutting concern, not an afterthought | `[FRS §30]` | Votes, evaluations, overrides, moderation, config changes must be traceable |
| AP-8 | Mobile-first with four channel experiences | `[FRS §5]` | Mobile app, public web, admin web, judge web |
| AP-9 | Privacy and visibility are per-resource configuration | `[FRS §9][§10][§36]` | Public/Followers/Collaboration-Only/Private content |

## 3. Functional Baseline Reference

| Artifact | Version | Location |
|----------|---------|----------|
| StarMitra FRS | v1.1 — Multi-Talent / Multi-Skill Model | `01_Requirements/FRS/` |
| Business rules | BR-01 … BR-15 | FRS §32 |
| Product decisions | PD-01 … PD-07 | FRS §40 |
| Logical data model (recommended entities) | FRS §31 | Refined in DATA-ARCHITECTURE |
| MVP priority | P0/P1/P2 | FRS §35 |

## 4. System Context

Summary — detail in [SYSTEM-CONTEXT.md](SYSTEM-CONTEXT.md).

StarMitra is a mobile-first talent discovery, competition, and collaboration platform `[FRS §1]`. Four channels `[FRS §5]`:

- **Mobile app** — creators and audience
- **Public web** — visitors, discovery, public content
- **Admin web** — administrators and super admins
- **Judge web** — judges evaluating assigned submissions

External system touchpoints `[Proposed/Open]`: push notification provider, email/SMS provider (OTP and notifications), media storage/CDN, media transcoding service. All are proposed integrations pending ADRs.

## 5. Logical Architecture

Detail in [SYSTEM-ARCHITECTURE.md](SYSTEM-ARCHITECTURE.md).

`[Accepted]` Modular monolith backend (OD-01 direction; ADR formalization pending) exposing a versioned REST API, with internal domain modules enforcing ownership boundaries. Async work (media processing, notifications, score computation triggers, audit writes) through an internal job/event mechanism. Rationale: FRS domains are highly interdependent (competition → submission → vote → evaluation → scoring → ranking → leaderboard → notification); a monolith preserves transactional integrity where the FRS demands it `[FRS §36]` while module boundaries preserve the option to extract services later.

`[Accepted]` Backend platform (OD-02): **Java 17+, Spring Boot 3.x, PostgreSQL, Flyway, REST/OpenAPI, WebSocket capability, Docker.** Module boundaries enforced via **Spring Modulith** or equivalent. Realtime (StarMitra Connect) architecturally isolated from core domain logic; extraction deferred until scale requires.

`[Open]` Whether any capability (e.g., media transcoding, real-time messaging) justifies a separate deployable from day one.

## 6. Domain Architecture

Detail in [DOMAIN-ARCHITECTURE.md](DOMAIN-ARCHITECTURE.md).

The FRS capability list consolidates into **15 bounded domains**:

| # | Domain | FRS coverage |
|---|--------|--------------|
| D1 | Identity & Access | Registration/auth (§8), system roles (§6) |
| D2 | Talent Profile | Profile, skills, portfolio (§7, §9) |
| D3 | Media | Upload, processing, publishing (§10) |
| D4 | Social Engagement | Likes, comments, follows (§4, §11) |
| D5 | Discovery | Feed, browse, search, trending (§11) |
| D6 | Messaging (StarMitra Connect) | 1:1/group/project messaging (§12) |
| D7 | Collaboration | Creative Rooms, projects, contribution roles, credits (§13, §14) |
| D8 | Competition | Competitions, rounds, categories, participation (§15) |
| D9 | Submission | Individual and project entries (§16, §17) |
| D10 | Voting | Audience voting, controls, aggregation input (§18) |
| D11 | Judging | Judge management, assignment, rubrics, evaluations (§19–21) |
| D12 | Scoring & Ranking | Aggregation, ranking, qualification, leaderboards (§22–24) |
| D13 | Notification | All notification types (§25) |
| D14 | Moderation | Reports, actions, safety (§26) |
| D15 | Administration & Audit | Admin portal, platform config, audit, reporting (§27, §29, §30) |

### 6.1 Mandatory Core Model

`[PD-01][PD-02][PD-03]` — non-negotiable structure:

```text
User
 ├── UserTalentSkill ──> TalentSkill          (profile claims; many allowed)
 └── UserSystemRole ──> SystemRole            (authorization; admin-assigned)

Project
 └── ProjectMember
       └── ProjectContributionRole            (contextual capacity per project)

Competition participation is recorded independently as a Submission
linked to User (individual) or Project (team), CompetitionRound, and
the TalentSkill category under which the entry competes.
```

Authorization checks MUST read `UserSystemRole` only. `TalentSkill`, `ProjectContributionRole`, and competition category are never inputs to authorization decisions `[BR-02][FRS §38]`.

## 7. Application Architecture

`[Proposed]` Four client applications over one backend API:

| Client | Stack direction (proposed, open) | Notes |
|--------|----------------------------------|-------|
| Mobile | React Native + TS recommended — `[OD-05 pending]` | FRS §5 scope: creator/audience; admin/judge remain web |
| Public web | React + TS + Vite SPA `[Accepted]`; SSR/prerender carve-out only if SEO confirmed critical | Discovery crawlability is inference, not FRS — open sub-decision |
| Admin web | SPA — `[Proposed]` | Configuration-heavy; form-driven UI for rubric builder |
| Judge web | SPA, may share codebase/components with Admin web — `[Proposed]` | Focused workflow: assigned entries → dynamic form → submit |

`[Accepted]` Backend modules mirror D1–D15 (Java 17+ / Spring Boot 3.x, Spring Modulith boundary enforcement — OD-01/OD-02). Each module owns its API surface, business rules, and data. Cross-module reads go through explicit contracts (queries/events), not shared tables — enforced by module-boundary checks `[Open: enforcement tooling]`.

## 8. Data Architecture

Detail in [DATA-ARCHITECTURE.md](DATA-ARCHITECTURE.md).

`[Proposed]` Single relational database as system of record. `[Open]` DBMS selection — relational integrity for votes/scores/rankings argues strongly for relational; the FRS logical model (§31) maps cleanly.

Key data rules:

- Immutable records: `EvaluationTemplateVersion`, submitted `JudgeEvaluation`, `Vote`, `Submission` competition/round association `[FRS §16][§20][§21]`
- Soft-delete/status transitions for content: Draft → Processing → Published → Hidden/Rejected/Removed `[FRS §10]`
- `AuditLog` as append-only `[FRS §30]`
- Media binaries in object storage; `MediaAsset` rows hold metadata and status `[Proposed]`

## 9. API Architecture

Detail in [API-ARCHITECTURE.md](API-ARCHITECTURE.md).

`[Proposed]` REST + JSON, OpenAPI 3.x contracts (contracts live in `05_API-Specifications/OpenAPI/`). Versioned base path (`/api/v1`). Four API surfaces by audience: public, authenticated user, judge, admin. Real-time needs (messaging, live vote counters where enabled) — `[Open]` WebSocket vs SSE vs polling; decision deferred to messaging/feed design phase.

Non-negotiable API rules from FRS §38: APIs must let one user submit in multiple eligible categories without duplicate identities; authorization middleware evaluates system roles/permissions, never talent category.

## 10. Security Architecture

Detail in [SECURITY-ARCHITECTURE.md](SECURITY-ARCHITECTURE.md).

- RBAC over `UserSystemRole`; permission checks enforced server-side `[BR-02][FRS §36]`
- Authentication: mobile/email + OTP or configured mechanism `[FRS §8]`; `[Open]` identity provider (managed IdP vs in-house)
- Password reset/recovery where password auth enabled `[FRS §8]`
- Account status machine: Active / Suspended / Blocked / Deactivated `[FRS §8]`
- Consent capture for Terms, Privacy Policy, community guidelines `[FRS §8][§26]`
- Voting abuse controls: duplicate detection, rate limiting `[BR-14]`
- Judge isolation: judges see only assigned submissions `[FRS §19]`

## 11. Media Architecture

Detail in [MEDIA-ARCHITECTURE.md](MEDIA-ARCHITECTURE.md).

`[Proposed]` Client → direct upload to object storage (pre-signed URLs) → async processing pipeline (validation, transcoding, thumbnails, moderation hooks) → publish via CDN. `MediaAsset` lifecycle mirrors FRS §10 statuses. Visibility enforced at delivery (signed URLs or token-gated CDN for non-public content) `[FRS §9][§10]`.

`[Open]` Storage provider, CDN, transcoding technology, document preview strategy.

## 12. Notification Architecture

Detail in [NOTIFICATION-ARCHITECTURE.md](NOTIFICATION-ARCHITECTURE.md).

`[FRS §25]` ~10 notification event families (account, submissions, competition lifecycle, rounds, voting, judge assignments, results, messages/rooms, project invitations, announcements). `[Proposed]` Event-driven: domains emit domain events; notification domain renders per-channel templates (in-app, push, email, SMS) honoring per-user preferences `[FRS §12 notification controls]`. `[Open]` Channel providers; whether in-app feed is polled or pushed.

## 13. Observability

Detail in [OBSERVABILITY-ARCHITECTURE.md](OBSERVABILITY-ARCHITECTURE.md).

`[FRS §36]` Application logs, metrics, alerts required. `[Proposed]` Structured logging with correlation IDs; RED metrics per API surface; health/readiness endpoints; alerting on scoring pipeline failures, vote ingestion anomalies, media processing backlog. **Audit ≠ observability:** `AuditLog` is a product-level, queryable, append-only business record `[FRS §30]`; telemetry is operational.

## 14. Deployment Architecture

`[Proposed]` Containerized backend + static/SSR web frontends + managed database + object storage + CDN. Single region for MVP. `[Open]` Cloud provider, Kubernetes vs managed container service, IaC tooling. All to be resolved via ADRs before implementation.

## 15. Environment Strategy

`[Proposed]` Four environments:

| Environment | Purpose |
|-------------|---------|
| `dev` | Feature development, unstable |
| `staging` | Pre-production mirror for QA/UAT `[FRS §37 UAT scenarios]` |
| `prod` | Live |
| `local` | Developer machines, fully containerized dependencies |

`[Open]` Data seeding/anonymization policy for non-prod.

## 16. Scalability Considerations

`[FRS §36]` Scalable media storage/delivery is required. `[Proposed]` Scaling priorities: (1) media delivery via CDN offload; (2) read-heavy feed/discovery via query optimization and caching before service splits; (3) vote ingestion bursts during competition windows via buffering/batch aggregation; (4) stateless API tier for horizontal scaling. `[Open]` Target load numbers — needed to size decisions; pending product volume estimates.

## 17. Availability Considerations

`[Proposed]` MVP targets standard availability for a single-region deployment with automated backups `[FRS §36 backup/recovery]`. Competition deadlines and scoring runs are the availability-critical windows; degrade gracefully (e.g., cached leaderboards) rather than fail closed on non-critical paths. `[Open]` Formal availability/RTO/RPO targets — business input needed.

## 18. Auditability

`[FRS §30]` Mandatory audit coverage: account changes, profile/skill changes where required, submission lifecycle, vote events, judge assignments, evaluation submission/re-evaluation, rubric version changes, qualification/ranking overrides, moderation actions, admin configuration changes.

`[Proposed]` Implementation: append-only `AuditLog` written transactionally with the business operation (or via guaranteed event), capturing actor, action, entity, before/after where applicable, timestamp, and authorization context for overrides `[BR-13]`.

## 19. Architecture Risks

| # | Risk | Impact | Mitigation |
|---|------|--------|------------|
| R1 | Model erosion: skills/contribution roles leaking into authorization | Critical product violation `[BR-02]` | Code-level separation enforced in module boundaries; security tests asserting authz ignores talent data |
| R2 | Config-driven complexity (rubrics, rounds, scoring) underestimated | Delivery risk | Build a single generic "configuration engine" pattern; spike early in module design |
| R3 | Voting abuse (bots, duplicate votes) undermines competition credibility | Trust/product risk | `[BR-14]` controls; rate limits; anomaly detection; audit |
| R4 | Media pipeline complexity (formats, transcoding, moderation) | Cost/schedule risk | Managed services `[Proposed]`; constrain MVP formats |
| R5 | Real-time expectations (chat, vote counts) vs MVP scope | Scope risk | FRS limits live features to P2 `[FRS §35]`; confirm interpretation |
| R6 | Single-user-multiple-identity UX confusion | Adoption risk | Clear IA in product design phase |
| R7 | Immutable-version discipline (rubrics, evaluations) accidentally violated by "quick fixes" | Data-integrity risk | DB-level constraints + write-path review |

## 20. Decisions

### Accepted Technology Decisions

`[Accepted]` — approved in review, ADR formalization pending:

| # | Decision | Result |
|---|----------|--------|
| OD-01 | Architecture style | **Modular monolith** — 15-domain structure, strong boundaries via Spring Modulith (or equivalent), future extraction preserved |
| OD-02 | Backend technology | **Java 17+, Spring Boot 3.x, PostgreSQL, Flyway, REST/OpenAPI, WebSocket capability, Docker** |
| OD-03 | Primary database | **PostgreSQL** — relational typed core; JSONB selectively for config entities; constraint/transaction-first invariants; advanced features (partitioning, replicas, CDC, multi-region) deferred |
| OD-04 | Web frontend | **React + TypeScript + Vite SPA** — one codebase, route-group surfaces (public/app/judge/admin); backend authZ authoritative; mobile-first mandatory; SEO/SSR open sub-decision; specific libraries not auto-approved |
| OD-05 | Mobile | **React Native + TypeScript + Expo** — creator/audience scope only; admin/judge stay web; no UI reuse assumed; platform sequencing, push provider, OTA tooling, native modules = separate decisions |
| OD-06 | Authentication/session | **First-party Spring Security** — OTP primary, JWT access + opaque persisted refresh (rotation + reuse detection + revocation); web=httpOnly cookies+CSRF, mobile=bearer+secure enclave; unified mechanism; social login/IdP deferred; no Redis implied |
| OD-07 | API architecture | **REST + OpenAPI** — `/api/v1/{domain}` module-owned namespaces; DTO boundary; RFC 9457 errors; cursor+offset pagination; constraint-first idempotency; provider-agnostic media contracts; REST=commands, WS=events |
| OD-08 | Media architecture | **Object storage + direct-to-storage upload + async processing + CDN** — provider-neutral interfaces; PG=metadata only; authz-before-signed-URL; no broker/CDN/provider selected (OD-12) |
| OD-09 | Real-time | **WebSocket in monolith, isolated D6** — PG persistence-first, at-least-once + idempotent dedup, REST recovery, per-event authz; protocol detail open (spike); no broker; single-instance MVP |
| OD-10 | Cache | **No distributed cache for MVP** — in-process + HTTP/CDN layers only; PG authoritative; Redis deferred (explicit triggers, separate decision) |
| OD-11 | Search | **PostgreSQL-native** — FTS + trigram + relational filters; deterministic documented relevance; authz-scoped; dedicated engine deferred w/ triggers |

### Future Infrastructure Decisions — NOT approved

The following are **not** automatically approved by OD-02 and each requires its own justification/decision: **Redis, Kafka, RabbitMQ, Elasticsearch/OpenSearch, Kubernetes, Service Mesh.** They remain evaluated inside their respective ODs (OD-09 realtime, OD-10 cache, OD-11 search, OD-12 cloud/deployment).

### Open Decisions

Numbering aligns with `03_Architecture/ADR/ARCHITECTURE-DECISION-REGISTER.md` (single authoritative scheme).

| # | Decision | Blocks |
|---|----------|--------|
| OD-07 | API architecture/contract — **accepted**, ADR-007 formalization pending | API design |
| OD-08 | Media architecture — **accepted**, ADR-008 formalization pending | — |
| OD-09 | Real-time — **accepted**, ADR-009 formalization pending | — |
| OD-10 | Cache — **accepted**, ADR-010 formalization pending | — |
| OD-11 | Search — **accepted in principle**, ADR-011 formalization pending | — |
| OD-12 | Cloud provider + compute (Kubernetes/Service Mesh **not** auto-approved) | Deployment design |
| OD-13 | Analytics approach (in-app reporting vs warehouse) | D15 reporting |

Tracked alongside: notification channel providers, target scale/availability/RTO-RPO numbers, and "any module as separate service at MVP" — see register Open Questions Q1–Q8 and OD-12. *(OD numbering aligned to the register; OD-07 was inserted as API Architecture and the original sequence renumbered.)*

## 21. Proposed ADRs

To be created in `03_Architecture/ADR/` upon review (numbered when drafted):

| # | Subject | Status |
|---|---------|--------|
| ADR-001 | Architecture style — modular monolith | Accepted — pending formalization |
| ADR-002 | Backend platform — Java 17+ / Spring Boot 3.x | Accepted — pending formalization |
| ADR-TBD-3 | API style/versioning/authn scheme | Proposed |
| ADR-TBD-4 | Media pipeline (upload → process → deliver) | Proposed |
| ADR-TBD-5 | Notification event-driven design | Proposed |
| ADR-TBD-6 | Audit log implementation approach | Proposed |
| ADR-TBD-7 | Mobile client technology | Proposed |
| ADR-TBD-8 | Web client technology (public/admin/judge) | Proposed |

## 22. FRS Traceability

| FRS section | Architecture coverage |
|-------------|----------------------|
| §3 Multi-talent model | §6.1, DOMAIN-ARCHITECTURE (D1/D2/D7/D8), DATA-ARCHITECTURE, SECURITY-ARCHITECTURE |
| §5 Platform strategy | §4, §7, SYSTEM-CONTEXT |
| §6 System roles | SECURITY-ARCHITECTURE, D1 |
| §7 Skill taxonomy | D2, DATA-ARCHITECTURE |
| §8 AuthN/account | SECURITY-ARCHITECTURE, D1 |
| §9 Profile/portfolio | D2, DATA-ARCHITECTURE |
| §10 Media | MEDIA-ARCHITECTURE, D3 |
| §11 Feed/discovery | D4/D5 |
| §12 Connect | D6, API-ARCHITECTURE |
| §13–14 Rooms/contribution roles | D7 |
| §15–17 Competitions/submissions | D8/D9 |
| §18 Voting | D10, SECURITY-ARCHITECTURE |
| §19–21 Judges/rubrics/evaluations | D11, JUDGE-RUBRIC-MODEL |
| §22–24 Scoring/rounds/leaderboards | D12 |
| §25 Notifications | NOTIFICATION-ARCHITECTURE, D13 |
| §26 Moderation | D14, SECURITY-ARCHITECTURE |
| §27–28 Portals | §7, D15 |
| §29 Analytics | D15, `[OD-13]` |
| §30 Audit | §18, OBSERVABILITY-ARCHITECTURE |
| §31 Data model | DATA-ARCHITECTURE |
| §32 BR-01–15 | §6.1 + per-domain rule mapping in DOMAIN-ARCHITECTURE |
| §34 Screens | Deferred to `02_Product-Design/` |
| §35 MVP priority | §5/§6 phasing; P0 in scope for baseline |
| §36 NFRs | §8, §10, §13, §16, §17 |
| §38 Implementation guidance | AP-2/AP-3, §6.1, API-ARCHITECTURE rules |
| §40 PD-01–07 | Binding product decisions throughout |
