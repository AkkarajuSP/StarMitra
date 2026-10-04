# StarMitra UAT Environment

Complete local setup for Product Owner acceptance testing. No developer-only
knowledge required beyond running three commands.

> Status: verified against commit `d132814` + UAT additions (see §12).

## 1. Prerequisites

| Tool | Version | Check |
|---|---|---|
| Docker Desktop | any recent | `docker --version` |
| Java | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Node.js | 18+ | `node -v` |

PostgreSQL 17 runs in Docker — no separate install needed. If port 5432 is
already used by a local PostgreSQL, either point `DB_URL` at it or let the
compose stack use it (see Troubleshooting).

## 2. Startup steps

From a clean checkout, three terminals:

```powershell
# Terminal 1 — PostgreSQL (persistent volume: pgdata)
docker compose up -d postgres

# Terminal 2 — Backend (UAT profile → seeds demo data + console OTP)
cd backend
$env:SPRING_PROFILES_ACTIVE = "uat"
mvn spring-boot:run
# wait for:  "Started StarMitraApplication" + "UAT SEED — DONE"
# busy port?  $env:SERVER_PORT = "9090"

# Terminal 3 — Frontend
cd frontend
npm install      # first time only
npm run dev      # http://localhost:5173
```

The backend runs Flyway migrations automatically at boot, then `UatSeedRunner`
creates all demo data **once** (idempotent — safe to restart).

## 3. URLs

| Thing | URL |
|---|---|
| Frontend (login page) | http://localhost:5173/login |
| Admin Portal | http://localhost:5173/admin |
| Judge Portal | http://localhost:5173/judge |
| Backend | http://localhost:8080 |
| API base | http://localhost:8080/api/v1 |
| Canonical OpenAPI contract | http://localhost:8080/openapi.yaml |
| CSRF bootstrap (browser-safe) | `GET /api/v1/public/csrf` |
| Health | `GET /api/v1/auth/me` (401 = server up, unauthenticated) |

## 4. Test credentials (UAT only — OTP, no passwords)

Authentication is **email OTP** — there are no passwords. In the `uat`
profile the OTP is printed to the **backend console** in a clear banner:

```
======================== UAT OTP ========================
channel=EMAIL destination=uat-admin@starmitra.dev otp=475610
===========================================================
```

This sender (`UatOtpSender`) exists **only** under the `uat` profile and is
never registered in local/test/prod. Nothing is exposed to the frontend.

| Account | Roles | Purpose |
|---|---|---|
| `uat-user@starmitra.dev` | USER | audience: votes, follows, reports |
| `uat-creator@starmitra.dev` | USER | talent: PUBLIC profile, room owner, project submission |
| `uat-judge@starmitra.dev` | USER + JUDGE | Judge Portal; assigned to UAT Round 1 |
| `uat-admin@starmitra.dev` | USER + ADMIN | Admin Portal |
| `uat-superadmin@starmitra.dev` | USER + SUPER_ADMIN | role administration |
| `uat-member@starmitra.dev` | USER | project member (Vocalist contribution role) |
| `uat-contestant2@starmitra.dev` | USER | solo competitor (Singing) |
| `uat-voter@starmitra.dev` | USER | cast audience votes |

**Login (UI):** open `/login`, enter the email, submit, copy the OTP from the
backend console, submit — done.

**Login (API):**
```powershell
# 1) CSRF bootstrap (cookie-mode mutations need X-XSRF-TOKEN)
Invoke-WebRequest http://localhost:8080/api/v1/public/csrf -WebSession $s -UseBasicParsing
# 2) request OTP → read console
Invoke-RestMethod -Method POST -WebSession $s -ContentType "application/json" `
  -Headers @{ "X-XSRF-TOKEN" = ($s.Cookies.GetCookies("http://localhost:8080") | ? Name -eq "XSRF-TOKEN").Value } `
  -Uri http://localhost:8080/api/v1/auth/otp/request `
  -Body '{"channel":"EMAIL","identifier":"uat-admin@starmitra.dev"}'
# 3) verify → returns accessToken (15 min) + refresh cookie
Invoke-RestMethod -Method POST -WebSession $s -ContentType "application/json" `
  -Headers @{ "X-XSRF-TOKEN" = ($s.Cookies.GetCookies("http://localhost:8080") | ? Name -eq "XSRF-TOKEN").Value } `
  -Uri http://localhost:8080/api/v1/auth/otp/verify `
  -Body '{"identifier":"uat-admin@starmitra.dev","otp":"<console>"}'
```

## 5. Seeded test data

| Domain | Data |
|---|---|
| Skills | UAT Singing, UAT Acting, UAT Dancing |
| Profiles | "UAT Creator" + "UAT Contestant Two" (PUBLIC) |
| Creative Room | "UAT Room - The Trio": owner=uat-creator, member=uat-member (Vocalist) |
| Competition | "UAT Competition - Talent Showcase" — categories **UAT Category - Singing** / **UAT Category - Acting**; rounds **UAT Round 1** (ACTIVE, rubric bound) + **UAT Round 2** |
| Rubric | "UAT Rubric - Performance" (published): Technique 60% + Stage Presence 40% |
| Participants | solo USER (contestant2→Singing); PROJECT (room→Acting) |
| Submissions | 2 × FINALIZED (one per participant, Round 1) |
| Votes | 3 audience votes (2 solo + 1 project) |
| Judging | uat-judge assigned to Round 1; evaluations submitted for both submissions |
| Scoring | config 40/60; calculated + finalized; TOP_N=1 qualification |
| Progression | maxAdvance=1 → contestant2 ADVANCED |
| Leaderboard | PUBLISHED for Singing/R1 (rank 1, score 91.6) |
| Moderation | 1 OPEN case (demo report by uat-user) |
| Notifications | producer events fired: COMPETITION_PUBLISHED, EVALUATION_STATUS, LEADERBOARD_PUBLISHED, FOLLOW/LIKE (dedup-protected) |

## 6. Role descriptions

- **USER** — self-service: profile, skills, portfolio, media, discovery,
  social, rooms, competitions, submissions, votes.
- **JUDGE** — USER + Judge Portal. Only sees FINALIZED submissions inside
  active M12 assignments. Cannot open `/admin` (403).
- **ADMIN** — USER + Admin Portal. Competition config, judge assignment,
  scoring/progression, moderation decisions, audit.
- **SUPER_ADMIN** — ADMIN + system-role grants.

## 7. Admin login

`http://localhost:5173/login` → `uat-admin@starmitra.dev` → console OTP →
auto-redirects to `/admin`. Pages: dashboard, users, skills, competitions,
judges, rubrics, scoring, progression, leaderboards, moderation,
notifications, audit.

## 8. Judge login

Same flow with `uat-judge@starmitra.dev` → `/judge`. Pages: dashboard,
assignments (UAT Round 1), submissions (2 finalized), evaluate (rubric),
notifications. Visiting `/admin` shows "no portal access" UX; the API denies
it regardless (403).

## 9. Normal user login

`uat-user@starmitra.dev` (or any fresh email — first OTP registers a new
USER). **There is no end-user web UI yet** (see Known Limitations); exercise
the user journey through the REST API: `GET /api/v1/profiles/me`,
`PUT /api/v1/profiles/me`, `POST /api/v1/skills/me`,
`GET /api/v1/search?q=UAT&type=PROFILE`, `GET /api/v1/feed`,
`POST /api/v1/votes`, `POST /api/v1/submissions`.

## 10. Database reset

```powershell
# drop + recreate the UAT database (schema replays via Flyway on next boot)
$env:PGPASSWORD = "starmitra"
psql -h localhost -U starmitra -d postgres -c "DROP DATABASE starmitra_uat"
psql -h localhost -U starmitra -d postgres -c "CREATE DATABASE starmitra_uat"
# restart backend → migrations + UAT seed run again automatically
```

- DB: `starmitra_uat` @ `localhost:5432` (docker-compose `postgres:17-alpine`;
  this machine's shared PG17 instance was used for verification).
- Env vars: `DB_URL` `DB_USER` `DB_PASSWORD` `JWT_SECRET` `SERVER_PORT`
  `CORS_ORIGINS`.
- Migrations: Flyway, `classpath:db/migration`, `validate-on-migrate`,
  `ddl-auto=none`.

## 11. Troubleshooting

| Symptom | Fix |
|---|---|
| `port 5432 already allocated` | another PG is running — set `DB_URL`/`DB_USER`/`DB_PASSWORD` to it, or stop it |
| `port 8080 already in use` | `$env:SERVER_PORT="9090"` on the backend, and start the frontend with `$env:VITE_API_TARGET="http://localhost:9090"` so the Vite proxy reaches it |
| OTP not visible | backend terminal scrollback — search `UAT OTP`; only in `uat` profile |
| 401/403 on POST | cookie-mode needs `GET /api/v1/public/csrf` first + `X-XSRF-TOKEN` header (the SPA does this automatically) |
| Seed didn't run | only `uat` profile; marker `uat-admin@starmitra.dev` exists → reset DB |
| OTP expired | 5 min TTL; request again (60 s cooldown) |

## 12. Known limitations

- **No end-user web UI** — Admin + Judge portals are the only implemented
  frontend surfaces. Normal-user journeys are API-verified; a consumer SPA
  is a product follow-up, not hidden.
- **UAT OTP = backend console log** — no email/SMS provider locally. This is
  a documented `uat`-profile helper, not production behavior.
- **Media storage** — filesystem adapter (`LocalObjectStorageClient`/`LocalMediaProcessor`
  now also active under `uat`); object storage is the deploy-time adapter.
- `/v3/api-docs` is not generated; `/openapi.yaml` is the canonical contract.
- WebSocket realtime is ADR-accepted but has no module UI consumer yet.

## 13. UAT test entry points

| Journey | Entry |
|---|---|
| Admin oversight | `/admin` as uat-admin — all 11 admin pages live |
| Judge evaluation | `/judge` as uat-judge — R1 assignment, 2 finalized submissions, rubric |
| Individual E2E | API: register→profile→skills→competition→submission→vote (u2) → judge eval → score → progression → leaderboard (already seeded through leaderboard; redo upstream stages freely) |
| Project E2E | seeded: room→member→project participant→finalized submission→vote→eval→leaderboard |
| Moderation | seeded OPEN case on `/admin/moderation`; restrictions propagate to M04/M05/M21 |
| Notifications | `/admin/notifications` or `/judge` bell; producer events already delivered |

## Verification evidence (this environment)

- Boot: `Started StarMitraApplication` + Flyway → v1.6+ migrations on PG 17.6,
  `UAT SEED — DONE` logged.
- Admin: OTP→verify→`GET /api/v1/auth/me` → `systemRoles:["USER","ADMIN"]`;
  `GET /api/v1/admin/dashboard` → `{openModerationCases:1, recentAuditEntries:25}`.
- Judge: assignments OK; `GET /api/v1/judges/me/submissions` → 2 FINALIZED
  scoped submissions with `mediaIds`; `GET /api/v1/admin/dashboard` as judge → 403.
- User: `GET /api/v1/profiles/me`, skills list, `GET /api/v1/search?q=UAT`
  → both PUBLIC profiles; `GET /api/v1/leaderboards?competitionId=…&categoryId=…&roundId=…`
  → rank 1, `finalScore 91.6000`, `ADVANCED`.
- Frontend: `tsc -b && vite build` clean; 11/11 vitest.
