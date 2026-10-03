# StarMitra API Design — Canonical Contract

**Contract:** [openapi.yaml](openapi.yaml) — 133 operations, OpenAPI 3.0.3 | **Rules:** ADR-006/007 | **Status:** Design — no implementation

## 1. Principles

- `/api/v1/{domain}` **module-owned namespaces** — API ownership = domain ownership; M19/M20 are orchestration surfaces only
- **DTO-only** — no persistence entity is ever exposed directly; request/response/list/transition DTOs per endpoint
- **RFC 9457** Problem Details on all errors — `type`/`title`/`status`/`detail`/`code`/`correlationId`/`errors[]`
- **DB-enforced idempotency first** — `Idempotency-Key` header only where a retry-sensitive command needs it
- **Explicit state transitions** — `POST /{id}/{transition}` (submit/finalize/withdraw/revoke/publish/reopen); **no PATCH status=…**
- **Backend authorization authoritative** — route visibility never grants access

## 2. Authentication (ADR-006)

OTP-first flow: `POST /auth/otp/request` → `/auth/otp/verify` → access JWT + opaque persisted refresh. `POST /auth/refresh` rotates (reuse-detection revokes token family); `logout` + session management own-scope only. **Web:** httpOnly+SameSite `sm_access` cookie + CSRF token. **Mobile:** Bearer + platform-secure storage. Enumeration-safe OTP responses (uniform `202`). Password login: **open — not exposed.**

## 3. Pagination

- **Cursor/keyset** (opaque `cursor` + `nextCursor`/`hasMore`): feeds, search, messages (keyed on `(conversation_id, sequence)`), notifications, submissions lists, participants, leaderboards
- **Offset**: bounded admin/reviewer lists only (judges, templates, moderation queue)
- `limit` default 20, max 100 *(configurable)*; stable order = cursor key (sequence/createdAt/id)

## 4. Concurrency

`If-Match` header → entity `version` column on: profile/portfolio/competition updates. Violations → `409 CONFLICT_VERSION`.

## 5. Media flow (ADR-008)

`POST /media` → `POST /{id}/upload-url` (provider-neutral pre-signed) → client uploads direct-to-storage → `POST /{id}/complete` → async processing → `GET /{id}` (4D state) → `GET /{id}/delivery-url` (visibility+domain authz). No binary proxying.

## 6. Module ownership boundaries enforced

- `Vote` (M11) ≠ `Like` (M21) — separate namespaces, separate entities
- `UserBlock` (M06 user-privacy) ≠ `ModerationRestriction` (M18 admin)
- `ProjectContributionRole`/`TalentSkill`/`JudgeExpertise` never appear as permission fields
- Judge access = `SystemRole(JUDGE)` + active `JudgeAssignment` scope — `/judges/me/*` resolves server-side only
- M19 `/admin/*` = dashboard + audit **views**; all mutations go through domain-owned endpoints — **no `/admin/update-anything`**
- Leaderboard/scoring reads are projection views — never score authority

## 7. Versioning policy

`/api/v1` — additive-compatible changes only within v1; breaking changes → v2 (not created now). Deprecation via `Sunset` header + docs.

## 8. Realtime

Per ADR-009: **REST = commands/history/recovery**; WebSocket = `message.new`/`delivered`/`read` fan-out (at-least-once + `clientMessageId` dedup). WS contract documented in `REALTIME-ARCHITECTURE.md`, not in OpenAPI paths.

## 9. Deferred — not exposed

`password_credentials` · `moderation_appeals` · `assignment_scope` — no endpoints for deferred entities.
