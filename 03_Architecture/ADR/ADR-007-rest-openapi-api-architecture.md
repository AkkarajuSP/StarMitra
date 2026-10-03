# ADR-007 — API Architecture: REST + OpenAPI

| Field | Content |
|-------|---------|
| ADR | ADR-007 |
| Register entry | OD-07 — API Architecture / API Contract Strategy |
| Status | **ACCEPTED** |
| Date | 2026-10-03 |
| Baseline refs | §9 API Architecture, §20 Decisions, API-ARCHITECTURE.md |
| Related | ADR-001 (module-owned APIs), ADR-006 (auth integration), ADR-009 (WS boundary) |

## Decision

**REST + OpenAPI** is the external API architecture: module-owned namespaces under `/api/v1/{domain}`, URI versioning, RFC 9457 Problem Details errors, DTO-boundary enforcement, constraint-first idempotency, cursor+offset pagination.

## Context

Four client surfaces consume one backend; API boundaries must mirror ADR-001 module boundaries and support future extraction without breaking consumers.

## Alternatives Considered

- **GraphQL** — rejected: authZ/caching complexity, N+1 risk; FRS resources are REST-shaped.
- **gRPC/RPC** — rejected: internal tool; wrong for public API.

## Rationale

Universal clients, codegen, cacheable, mature tooling; URI versioning handles mobile upgrade lag; Problem Details gives a consistent machine-readable error model.

## Guardrails / Constraints (binding)

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
16. WebSocket protocol selection is deferred to ADR-009.
17. API authorization is based on system roles and domain authorization rules; talent skills do not grant permissions.
18. Module boundaries must prevent direct cross-module persistence access.
19. GraphQL and gRPC are not selected as the primary external API.
20. API design must preserve future extraction capability without implementing microservices now.

## Consequences

URI versioning adds maintenance overhead vs invisible versioning; code-first spec risk of drift mitigated by CI diff-checking; split pagination adds convention overhead.

## Deferred Items / Future Triggers

Client version-support window; codegen tool choice; sparse fieldsets; WS protocol (ADR-009).

## Open Items

- N/N−1 client support window — product input
- Build-time vs spec-first OpenAPI authoring — implementation detail
