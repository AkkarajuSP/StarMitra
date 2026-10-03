# Testing Guide

## Pyramid

- **Unit** (`*Test`) — services, rotation/OTP logic, error rendering, cursor codec, ArchUnit boundaries. Mocked repos; no Spring context where avoidable.
- **Contract** (`OpenApiContractTest`) — packaged `openapi.yaml` parses, 133 ops, unique operationIds, responses present. Fails on contract drift.
- **Security** (`SecurityBaselineTest`) — `@WebMvcTest` + `jwt()` post-processor: 401 unauth'd, role-less → scope service decides, correlation echo.
- **Integration** (`*IT`, `FlywaySchemaIT`) — `@SpringBootTest` + real PostgreSQL: full Flyway chain, table/constraint counts, deferred absence, seeds, XOR check.

## Run

```bash
mvn test                                  # unit + contract + architecture + security
mvn test -Dtest=FlywaySchemaIT            # needs Postgres at $DB_URL (test profile: starmitra_it)
mvn verify                                # everything CI runs
```

CI (`.github/workflows/ci.yml`): OpenAPI lint → `mvn verify` with Postgres service → Flyway clean-DB apply.

## What to add when implementing features

Per slice: service unit tests · `@WebMvcTest` authz matrix (role/ownership/scope/IDOR) · repo IT for constraint behavior · idempotency replay test · negative transition test.
