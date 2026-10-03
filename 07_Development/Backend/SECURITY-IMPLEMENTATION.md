# Security Implementation

## Auth flow (ADR-006)

1. `POST /auth/otp/request` — enumeration-safe 202; rate/lockout via `otp_challenges` counts; supersede-on-resend.
2. `POST /auth/otp/verify` — SHA-256 hashed OTP compare (constant-time `MessageDigest.isEqual`), attempts ≤ max, expiry — consume on success; issues access JWT + opaque refresh (new family), sets httpOnly `sm_access` + `sm_refresh` cookies.
3. `POST /auth/refresh` — rotation: old token revoked+replaced; reuse of revoked → family revocation + `REFRESH_REUSE_DETECTED` audit.
4. `POST /auth/logout` — revoke refresh, expire cookies.
5. `GET /auth/me` — identity + systemRoles.

Mobile uses `Authorization: Bearer` + refresh via `X-Refresh-Token`; web uses cookies + CSRF (cookie-token repo, Bearer-header requests exempt).

## Never-logged

OTP values, refresh tokens (only SHA-256 hashes stored), Authorization headers, passwords (no password flow exists).

## Authorization model

JWT `roles` claim → `ROLE_*` authorities (SystemRole only). `SystemRoleGuard` centralizes role checks. Judge access chain: `ROLE_JUDGE` → active `Judge` → ACTIVE `JudgeAssignment` matching (competition[,category[,round]]) — `JudgeScopeService.requireScope`. Domain roles never authorize.

## Config

`app.jwt.secret` env-injected (HS256 dev default — replace for non-local); cookie `Secure`+`SameSite` profile-driven; CORS allow-list via `app.cors.allowed-origins`.
