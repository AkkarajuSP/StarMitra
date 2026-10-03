# M01 — Authentication & Identity — Implementation

**Slice:** first business implementation on the foundation. **Status: complete.**

## Architecture

```
AuthController (api)          — DTOs only, no persistence imports
  ├─ OtpService               — challenge lifecycle: generate/hash/expiry/
  │                             attempts/lockout/throttle/supersede/consume
  ├─ RegistrationService      — OTP-first find-or-register (User + USER role)
  ├─ AuthService              — session orchestration (JWT + refresh family)
  ├─ RefreshTokenService      — issue/rotate/reuse-detect/revoke
  ├─ SessionService           — session listing + scoped revocation (IDOR-safe)
  └─ AuthEventService         — authentication_audit_events writer (REQUIRES_NEW)
persistence: User, SystemRole, UserSystemRole, OtpChallenge,
             RefreshToken, AuthenticationAuditEvent (6 M01 tables — no new tables)
platform: JwtTokenService, SecurityConfig, SystemRoleGuard, SecurityUtils,
          CorrelationIdFilter, GlobalExceptionHandler, AuditService
```

## Flows

**Registration (OTP-first).** The contract exposes no `/auth/register`: identity is established on first `POST /auth/otp/request` for an unknown identifier — `RegistrationService` creates `users` row + `user_system_roles(USER)`. Duplicate protection = `UQ(email)`/`UQ(phone)`; concurrent first-registrations converge (idempotent). Privileged roles can never be self-granted — the only role assignment path writes `USER`.

**OTP request.** Crypto-secure 6-digit (`SecureRandom`), stored SHA-256 only, `expires_at` (5 min), resend cooldown (60 s), request-count lockout (≥5/15 min → refuse), supersede-all prior active challenges, enumeration-safe `202` externally (suspended/blocked → silent no-op + `OTP_REQUEST_UNKNOWN` event).

**OTP verify.** Latest unconsumed challenge; `isUsable()` (not expired/consumed/superseded); constant-time hash compare; wrong → `attempts++` (persisted via `noRollbackFor`), ≥max → locked + `OTP_LOCKED`; valid → `consume()` + session. All failure modes → `OTP_INVALID`/`OTP_LOCKED` (uniform).

**Session.** Access JWT (HS256, `roles` claim = SystemRole names only) + opaque 64-byte refresh (SHA-256 stored, family id). Rotate: new token, old `revoked_at`; replay of rotated → `reuse_detected=true` + **family revocation** + `REFRESH_REUSE_DETECTED` (kernel + module audit). Logout: revoke + expire cookies. `GET /auth/sessions` lists own active sessions (`current` by presented-token family); `DELETE /sessions/{id}` is IDOR-safe (foreign id → `NOT_FOUND`).

## Authorization model

JWT `roles` → `ROLE_*` authorities; `SystemRoleGuard` centralizes checks; `JudgeScopeService` (M12) adds assignment scope — `TalentSkill`/`JudgeExpertise`/`ProjectContributionRole` never appear in tokens or checks.

## Security controls

SecureRandom OTP · hashed-only storage · attempts lockout · request throttle · resend cooldown · superseding · constant-time compare · uniform errors · reuse→family-kill · `noRollbackFor` on security-state mutations · `clearAutomatically` on bulk revocation (L1-cache bypass) · never logs raw OTP/tokens.

## API mapping

`POST /auth/otp/request` → 202 · `POST /auth/otp/verify` → 200 AuthSession · `POST /auth/refresh` → 200 · `POST /auth/logout` → 204 · `GET /auth/sessions` → 200[] · `DELETE /auth/sessions/{id}` → 204 · `GET /auth/me` → 200 CurrentUser. Exactly per `openapi.yaml` (133 ops — unchanged).

## DB mapping

`users` (UQ email/phone, status check), `system_roles` (4 seeds), `user_system_roles` (composite PK), `otp_challenges` (attempts ≤ max check), `refresh_tokens` (family, reuse_detected, UQ hash), `authentication_audit_events` (append-only, jsonb metadata). No schema changes; Flyway authoritative.

## Testing

Unit: OtpService(9) · RefreshToken(4) · Registration(3) · Sessions(4) + platform/security/architecture/contract suites. IT (`IdentityFlowIT`, real PG17): full journey, family revocation, attempts persistence, superseding, session revocation, cross-user IDOR denial, constraint assertions. 44 tests total.

## Production notes (documented, not deferred silently)

- JWT HS256 — ADR-006 mandates no algorithm; evaluate RS256/JWKS for multi-instance sign-off.
- OTP delivery is `OtpSender` abstraction — wire real email/SMS provider per environment.
- `users.status` has no PENDING — unverified registrations are ACTIVE but unusable (OTP gates all access); revisit if policy requires verification-gated activation.
