# Local Development

## Prereqs

- JDK 17+ (built on Temurin 21) · Maven 3.9+ · PostgreSQL 15+ (17 tested) · Docker optional

## Database

```bash
docker compose up -d postgres
# or point DB_URL at local Postgres; schema comes from repo-root db/migration/ via Flyway
```

Copy `.env.example` → `.env`; set `DB_*`, `JWT_SECRET` (32+ chars for non-dev).

## Run

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
# http://localhost:8080/api/v1/...  ·  /openapi.yaml served statically ·  ws: /ws (STOMP)
```

## Profiles

`local` (dev, insecure cookie) · `test` (ITs) · `uat` · `staging` · `prod` (Secure cookies; env-only secrets).

## Migrations

App startup runs Flyway against `classpath:db/migration` (packaged from canonical `db/migration/`). Flyway owns schema — Hibernate `ddl-auto=none`. Verified: 90 tables / deferred absent.
