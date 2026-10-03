# StarMitra Backend

Modular monolith (ADR-001) — Java 17+/Spring Boot 3.3, PostgreSQL + Flyway, Spring Security (ADR-006), REST/OpenAPI (ADR-007), WebSocket (ADR-009).

## Layout

```
backend/
  pom.xml  Dockerfile
  src/main/java/com/starmitra/
    StarMitraApplication.java
    platform/            # kernel: correlation, error, audit, idempotency,
    |                    #   pagination, security, websocket
    modules/
      identity/  profile/  skill/  media/  discovery/  connect/  room/
      portfolio/ competition/ submission/ voting/ judge/ rubric/
      scoring/ progression/ leaderboard/ notification/ moderation/
      admin/ judgeportal/ social/          # M01..M21
      # every module: api/ application/ domain/ persistence/
  src/main/resources/application{,-local,-test,-uat,-staging,-prod}.yaml
  src/test/…             # unit + contract + architecture + security + IT
```

**Flyway is schema-authoritative** — migrations from `db/migration/` (repo root) are packaged into `classpath:db/migration`; `ddl-auto=none`.

## Run

```bash
docker compose up -d postgres          # or local PG on 5432
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

See [LOCAL-DEVELOPMENT.md](LOCAL-DEVELOPMENT.md), [BACKEND-FOUNDATION.md](BACKEND-FOUNDATION.md), [TESTING-GUIDE.md](TESTING-GUIDE.md), [SECURITY-IMPLEMENTATION.md](SECURITY-IMPLEMENTATION.md), [MODULE-IMPLEMENTATION-GUIDE.md](MODULE-IMPLEMENTATION-GUIDE.md).
