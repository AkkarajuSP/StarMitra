# Backend Foundation — What Exists Now

**Phase: foundation only.** Infrastructure for the accepted architecture; no business workflow implementations.

## Implemented

| Area | Implementation | Proven by |
|------|----------------|-----------|
| Build | Maven, Java 17 bytecode on JDK 21, Spring Boot 3.3.5 | `mvn compile` green |
| Module boundary | 21 modules × `api/application/domain/persistence` packages; ArchUnit rules | `ModuleBoundaryTest` 4/4 |
| Auth (ADR-006) | OTP request/verify (hashed, supersede, throttle, lockout, enumeration-safe), short-lived JWT (HS256 dev, key-injectable), opaque persisted refresh + rotation + family revocation + reuse detection, httpOnly cookie + Bearer | `OtpServiceTest` 5/5, `RefreshTokenServiceTest` 4/4 |
| Authz | `SystemRoleGuard` + JWT `roles` claim (`ROLE_*`); judge scope via `JudgeScopeService` (M12 contract) — role → judge → active assignment → scope | `SecurityBaselineTest` 3/3 |
| Errors | RFC 9457 via Spring `ProblemDetail`, `code`+`correlationId`+`errors[]`, catalog enum mirroring API-ERROR-CATALOG | `ProblemDetailsTest` 2/2 |
| Correlation | `CorrelationIdFilter` — X-Correlation-Id echo + MDC + Problem property | covered in security test |
| Idempotency | `IdempotencyService` replay executor; DB-uniqueness first per API-IDEMPOTENCY-MATRIX | unit-covered pattern |
| Pagination | `Cursor` (opaque base64url keyset) + `PageResult` envelope | `CursorTest` 3/3 |
| Audit | `audit_log` writer (`AuditService`, REQUIRES_NEW, correlationId capture) — append-only is grant-enforced at deploy | — |
| WebSocket (ADR-009) | STOMP `/ws`, simple broker, JWT-authenticated CONNECT interceptor | config wired |
| Media (ADR-008) | `ObjectStorageClient` provider-neutral contract (upload/delivery URL, delete) | interface only |
| OpenAPI | canonical `openapi.yaml` packaged → `/openapi.yaml`; contract test asserts 133 ops | `OpenApiContractTest` 1/1 |
| Flyway | migrations packaged + applied; IT verifies 90 tables/90 PKs/deferred absent | `FlywaySchemaIT` 4/4 |
| Profiles | local/test/uat/staging/prod | config files |

## Explicitly NOT built (next phases)

Business flows (competitions, voting, scoring, progression, moderation, social, rooms), repository layer for non-foundation tables, WS event fan-out, media processing pipeline, admin views.
