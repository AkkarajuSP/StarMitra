# Implementation Baseline Reconciliation — Backend Foundation vs Accepted Baselines

**Branch:** `feature/backend-foundation` · **Verdict: GREEN — 0 RED**

## ADR compliance

| ADR | Decision | Foundation implementation | Status |
|-----|----------|---------------------------|--------|
| 001 | Modular monolith | single `starmitra-backend` module, `com.starmitra.modules.*` boundaries, ArchUnit enforcement | GREEN |
| 002 | Java/Spring Boot | Java 17 target on JDK 21, Spring Boot 3.3.5, Maven | GREEN |
| 003 | PostgreSQL | PostgreSQL 17 verified; Flyway authoritative; `ddl-auto=none` | GREEN |
| 004/005 | Web/mobile clients | Bearer + cookie security models supported server-side | GREEN |
| 006 | Auth/session | OTP→JWT+opaque refresh w/ rotation, family, reuse-detect, revocation; httpOnly+SameSite+CSRF | GREEN |
| 007 | REST+OpenAPI | canonical spec packaged+served; contract test; Problem Details | GREEN |
| 008 | Object storage | `ObjectStorageClient` contract — no provider binding, no proxying | GREEN |
| 009 | WebSocket | STOMP `/ws` + JWT-authenticated CONNECT; REST=commands, WS=events | GREEN |
| 010 | No distributed cache | none introduced | GREEN |
| 011 | PostgreSQL search | no ES; search_vector/tsvector exists in schema | GREEN |
| 012 | Managed container deploy | Dockerfile + compose, single service | GREEN |
| 013 | PostgreSQL analytics | `audit_log`/projections only; no warehouse | GREEN |

## Module alignment (M01–M21)

All 21 modules exist as `api/application/domain/persistence` packages. **M01 is business-implemented** (registration-via-OTP, OTP lifecycle, JWT, refresh rotation/reuse/family, sessions, role assignment, auth audit events). **M02 is business-implemented** (lazy-init profiles, visibility-filtered reads, ETag concurrency, own-profile updates; M03/M18 consumed via contracts). **M03 is business-implemented** (admin-managed taxonomy, user associations, optional proficiency, multi-talent; feeds M02 via `UserSkillReadContract` — real impl). **M04 is business-implemented** (media_assets/media_variants lifecycle, presigned upload via provider-neutral `ObjectStorageClient`, content-validated processing, signed delivery, `MediaReferenceContract` consumed by M02). **M05 is business-implemented** (PostgreSQL FTS search, skill browse, deterministic recency feed — visibility/moderation enforced in SQL; projection tables unused pending scale). **M21 is business-implemented** (follows/likes/comments/derived counters, author-only soft-delete, M04 target validation via `MediaReferenceContract.isDeliverableTo`, `SocialSignalContract` consumed by M05 feed boost). **M08 is business-implemented** (one-portfolio-per-user lazy-init, items with M03 skill tags + M04 media links, M07 credit links via seam, `PortfolioTargetContract` feeding M21 PORTFOLIO validation). **M07 is business-implemented** (rooms/members/skills/invitations/contextual roles/tasks/assets/final-outputs; verified credits minted at finalization — `ProjectCreditContract` closes M08's link seam). **M06 is business-implemented** (conversations/members/messages/receipts/blocks; PROJECT convs consume M07 membership contract; STOMP realtime via existing broker with membership-gated SUBSCRIBE; clientMessageId dedup). **M09 is business-implemented** (competitions, multi-skill categories, rounds, eligibility rules, submission configs, XOR participants; `CompetitionStructureContract` for M10–M16). **M10 is business-implemented** (submission lifecycle DRAFT→SUBMITTED→FINALIZED/WITHDRAWN, team entries with M07 contextual contributors + DB-02 snapshots, deadline/freeze rules, `SubmissionTruthContract` for M11–M16). **M11 is business-implemented** (authoritative votes with M10-derived scope, versioned vote_configs, UQ dedup+idempotency, `VoteTruthContract` for M14; fixed latent JSONB mapping in M09 entities). **M12 is business-implemented** (judges + M01 role grant, expertise as qualification-only, DB-08 scoped assignments validated by M09, judge-portal scope resolution via M10). **M20 portal surface** (`/judges/me/submissions` + `/rubrics` seam) now wired to M12. **M13 is business-implemented** (versioned rubrics, publish requires Σweights=100, published immutable → new-draft versioning, scoped judge evaluations via M12 `requireScopedAssignment`, `RubricContract` for M14). **M14 is business-implemented** (versioned scoring/tie-break/qualification configs, deterministic normalize→aggregate→seal pipeline, `ScoringTruthContract` for M15/M16, append-only audited overrides). **M15 is business-implemented** (round lifecycle via new M09 `transitionRoundState` contract, M14-qualification-driven ADVANCED/ELIMINATED records — UQ-idempotent, sealed-required completion, `ProgressionTruthContract` for M16, append-only overrides). **M16 is business-implemented** (publication-gated, rebuildable `leaderboard_projections` over M14 `latestResults` + M15 outcomes — never recalculates; sealed `leaderboard_snapshots`; rank-keyset cursor). **M17 is business-implemented** (`NotificationContract` event sink, `notification_event_references` UQ dedup, versioned templates, PK-idempotent read states, delivery-only prefs). **M18 is business-implemented** (reports+cases+append-only decisions+enforcement-action rows, governance `moderation_restrictions` ≠ M21 blocks, `ModerationContract` for resource-owner enforcement, evidence-as-refs). **M19 is business-implemented** (orchestration/read-only — derived dashboard + kernel `AuditQueryService` audit-search seam; **zero transactional tables**; all admin workflows delegate to owning modules; **React/Vite admin Web UI delivered**). **Numbering reconciled: M20 = Judge Portal** (accepted module — `judgeportal` package orchestrates M12; see `MODULE-NUMBERING-RECONCILIATION.md`); M21 unchanged. Remaining modules = boundary skeletons awaiting feature phases. ArchUnit rules prove: no api→persistence, no cross-module persistence imports, portals own no persistence. **GREEN**

## Database alignment

Canonical → physical → Flyway → packaged migrations: identical files; `FlywaySchemaIT` verifies 90 tables, 90 PKs, ≥70 FKs (76), seeds, `ck_cp_xor`, deferred absent. JPA entities only for foundation tables (users/roles/tokens/otp/judges/assignments/audit_log); column names match physical schema. **GREEN**

## API alignment

`openapi.yaml` packaged verbatim (`/static/openapi.yaml`, served at `/openapi.yaml`); contract test asserts **133 operations**, unique operationIds, responses present. Implemented endpoints match contract paths exactly (`/api/v1/auth/*`, `/api/v1/judges/me*`). **GREEN**

## Deviations

**NO DEVIATIONS.** Notes (non-deviations): JWT signing is HS256 with injectable secret (ADR-006 doesn't mandate algorithm — RS256-ready via decoder swap); `/auth/sessions` endpoints return empty list (foundation stub within contract shape); OTP verify resolves channel from identifier (contract has no channel field on `OtpVerify` — consistent with spec).
