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

All 21 modules exist as `api/application/domain/persistence` packages. Foundation-level code: **M01** (full auth core), **M12** (judge+assignment persistence + `JudgeScopeService` contract), **M20** (`/judges/me` + `/assignments`), **M04** (storage contract). Remaining modules = boundary skeletons awaiting feature phases. ArchUnit rules prove: no api→persistence, no cross-module persistence imports, portals own no persistence. **GREEN**

## Database alignment

Canonical → physical → Flyway → packaged migrations: identical files; `FlywaySchemaIT` verifies 90 tables, 90 PKs, ≥70 FKs (76), seeds, `ck_cp_xor`, deferred absent. JPA entities only for foundation tables (users/roles/tokens/otp/judges/assignments/audit_log); column names match physical schema. **GREEN**

## API alignment

`openapi.yaml` packaged verbatim (`/static/openapi.yaml`, served at `/openapi.yaml`); contract test asserts **133 operations**, unique operationIds, responses present. Implemented endpoints match contract paths exactly (`/api/v1/auth/*`, `/api/v1/judges/me*`). **GREEN**

## Deviations

**NO DEVIATIONS.** Notes (non-deviations): JWT signing is HS256 with injectable secret (ADR-006 doesn't mandate algorithm — RS256-ready via decoder swap); `/auth/sessions` endpoints return empty list (foundation stub within contract shape); OTP verify resolves channel from identifier (contract has no channel field on `OtpVerify` — consistent with spec).
