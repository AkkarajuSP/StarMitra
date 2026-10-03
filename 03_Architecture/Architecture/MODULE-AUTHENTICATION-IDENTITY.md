# Module Design — 01: Authentication & Identity

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 01 of N)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 without modification.

## 1. Purpose

Establish StarMitra's identity foundation: how users are created, authenticated, verified, kept in session, revoked, and how their **system authorization** is represented — the boundary every other module relies on before doing its own domain authorization.

## 2. Scope

Module Design D1 (Identity & Access Management, per baseline §6 domain map) as a bounded module of the ADR-001 modular monolith. Covers: user identity, OTP-first authentication, token/session lifecycle, system-role assignment, account lifecycle/states, authentication audit evidence.

## 3. Responsibilities

- User identity record + account lifecycle (Active / Suspended / Blocked / Deactivated `[FRS §8]`)
- Authentication: OTP (primary), optional password (open), social login (deferred) — via **first-party Spring Security** (ADR-006)
- Session/token issuance, rotation, revocation, reuse detection
- **System role assignment** (Audience/User, Creator/Talent, Judge, Admin, Super Admin `[FRS §6]`)
- OTP delivery abstraction (provider-agnostic — hands payloads to Notifications)
- Authentication audit evidence (security events, not business audit)
- Credential/verification state

## 4. Non-Responsibilities

Does **not** own: TalentSkill, TalentSkillAssignment, portfolio/media, Competition/Submission/Vote/JudgeEvaluation/Ranking, **ProjectContributionRole**, Creative Room membership, profile/bio, moderation decisions, notification *content/delivery* (provides security-notification payloads only).

**Preserved distinctions (BR-2):** `User` — who you are · `SystemRole` — what you may do in the app · `TalentSkill` — creative capabilities (data, not permission) · `ProjectContributionRole` — per-project collaboration role (per-room concept, not permission).

## 5. Domain Model (conceptual — no schema)

| Entity | Type | Notes |
|--------|------|-------|
| `User` | Domain entity | `id` (UUID), mobile, email, status, verified flags, timestamps — minimal FRS-justified fields `[§8]` |
| `SystemRole` | Reference data | Audience/Creator/Judge/Admin/SuperAdmin |
| `UserSystemRole` | Join entity | user ↔ role; separate from TalentSkillAssignment |
| `RefreshToken` | Security entity | opaque token record: id, userId, family/rotation id, status, device/session identifier *(proposal)*, expiry, timestamps |
| `OTPChallenge` | Ephemeral security record | challenge id, destination (hashed/masked), code hash, expiry, attempts, resend count, status |
| `AuthenticationAuditEvent` | Audit record | auth-relevant events (§15) — feeds `AuditLog` |
| *(deferred)* `PasswordCredential` | Security entity | only if password auth is approved later |

*Proposals marked; all records persisted in PostgreSQL — no Redis.*

## 6. Authentication Model (ADR-006 — preserved verbatim)

- **Mechanism:** OTP-first via first-party Spring Security; password optional (open PO decision); social login deferred; no external IdP for MVP.
- **Tokens:** short-lived JWT access + opaque DB-persisted refresh; **rotation on every refresh**; **reuse detection** (revokes token family on reuse); server-side revocation.
- **Transports:** Web = httpOnly `SameSite` cookies + CSRF; RN/mobile = `Authorization: Bearer` + platform-secure storage (SecureStore-class); Judge/Admin = same mechanism.
- **WS:** connection authenticated via same principal; revocation honored (ADR-009).
- Exact durations = **implementation/product configuration** (not invented here).

## 7. OTP Security Design (conceptual — no implementation)

| Concern | Design principle |
|---------|------------------|
| Generation | Cryptographically-random numeric OTP; stored **hashed**, never plaintext |
| Expiry | Short TTL *(config, PO/product)*; single-use; invalidated on success/expiry/superseded |
| Verification | Constant-time compare; generic failure response |
| Retry limits | Bounded attempts per challenge; lockout on exhaustion → new challenge required |
| Resend limits | Rate-limited resends; new challenge supersedes old |
| Throttling | Per-identifier + per-IP/device throttling on request + verify endpoints |
| Brute-force | Attempt counters + progressive delays; alert on anomalous patterns |
| Replay | Challenge consumed on first success; idempotent verify (consumed = success-response or error, never re-verify) |
| Enumeration | **Uniform responses** ("if account exists, code sent") — no account-existence signals in status, body, or timing |
| Failure handling | Bounded retries; escalation to lockout/audit |
| Audit | Every request, resend, verify-success, verify-fail, lockout logged |
| Delivery | Abstracted channel (SMS/email) — **provider not selected**; Notifications module delivers; auth decides payload + expiry |

## 8. Token / Session Lifecycle

```text
Register/Login request → OTP verify → User.authenticate
  → issue JWT access (short-lived) + opaque refresh (persisted, family-scoped)
  → client uses access until expiry → refresh → ROTATE (new refresh, old consumed, family updated)
  → logout/revocation → refresh consumed + family revoked; access left to short expiry
  → reuse detected → entire token family revoked + security alert
```

- **Expiration:** access = minutes-scale *(config)*; refresh = days-to-weeks *(config — PO/product)*.
- **Concurrent sessions:** supported; family-scoped refresh enables per-device session (device identifier = design proposal).
- **Compromised refresh:** reuse detection = theft signal → family revocation + notification.

## 9. Authorization Boundary

`Authentication` (who you are — this module) **≠** `Authorization` (what you may do — domain-enforced) **≠** `TalentSkill` (creative capability — data) **≠** `ProjectContributionRole` (per-room participation — data).

- Module issues an authenticated principal + system-role claims.
- Domain modules enforce **their own** authorization (competition access, room membership, judge assignment) — never delegated here.
- **TalentSkill never grants permissions; ProjectContributionRole never grants permissions.**

## 10. Role Model

`User 1—* UserSystemRole *—1 SystemRole` — a user may hold multiple system roles (e.g., Creator + Judge); roles are system-level only. **TalentSkillAssignment lives in a different module (D2/User Profile area) — never co-located with UserSystemRole.**

## 11. Admin / Judge Access

- **Judge:** system role grants *judge-capability*, not data access — actual access scoped by assigned competition/round/submission, enforced by the Competition/Judging modules. **Judge ≠ blanket read.**
- **Admin/SuperAdmin:** explicit system-role authorization on admin surfaces; no implicit rights.
- Account suspension/blocking: Admin action → module updates account status → **all sessions revoked**.

## 12. API Surface (conceptual — no OpenAPI files)

| Endpoint | Method/Route (conceptual) | Purpose | Auth | Notes |
|----------|---------------------------|---------|------|-------|
| OTP request | `POST /api/v1/auth/otp/request` | Request challenge | none | Uniform response; throttled |
| OTP verify | `POST /api/v1/auth/otp/verify` | Verify + issue tokens | none | Idempotent-ish (consumed challenge); issues JWT+refresh |
| Register | `POST /api/v1/auth/register` | Create account (then OTP) | none | Creates User + UserSystemRole(Audience) |
| Refresh | `POST /api/v1/auth/refresh` | Rotate refresh → new access | refresh cookie/bearer | Rotation + reuse detection |
| Logout | `POST /api/v1/auth/logout` | Revoke session | authenticated | Consumes refresh + family revoke |
| Sessions | `GET/DELETE /api/v1/auth/sessions` | List/revoke sessions | authenticated | Optional MVP+ — device/session mgmt |
| Account status | `GET /api/v1/auth/me` | Principal snapshot | authenticated | status + system roles only |
| *(deferred)* password, social, MFA | — | — | — | Separate decisions |

All endpoints: RFC 9457 errors, correlation ID, validation, no enumeration signals.

## 13. Data Ownership

**Owns:** User (identity subset), SystemRole, UserSystemRole, RefreshToken, OTPChallenge, AuthenticationAuditEvent.
**References (doesn't own):** Notification requests (→ D13), profile fields (→ D2), media avatar (→ D4 via D2), account-deletion domain events.
**Ephemeral:** OTPChallenge is short-lived (expiry/cleanup job).

## 14. Module Dependencies

| Module | Direction | Why |
|--------|-----------|-----|
| Notifications (D13) | Auth → Notif | OTP delivery, security alerts |
| User Profile (D2) | Auth → Profile (event) | post-registration profile creation |
| Moderation (D14) | both | account-status enforcement; moderation can suspend → session revocation |
| Media (D4) | none direct | avatar is profile-domain |
| Admin/Judge portals | consuming | role-scoped access |

## 15. Security Threats & Mitigations

| Threat | Mitigation |
|--------|------------|
| Account enumeration | Uniform responses, no timing/error-channel leakage |
| OTP brute force | Hashed storage, attempt caps, lockout, throttling |
| Credential stuffing (if password added) | Rate limits, lockout, optional MFA path |
| Token theft / replay | httpOnly cookies, short TTL, rotation, reuse detection |
| Session hijack | Family-scoped refresh, revocation on suspicious reuse |
| CSRF | SameSite + CSRF tokens on cookie-authenticated mutations |
| XSS | Cookie transport (not localStorage); CSP/secure headers (web tier) |
| Privilege escalation | Role assignment is admin-only + audited; roles never self-granted |
| Unauthorized role assignment | `UserSystemRole` writes restricted; audit trail |
| Insecure logout | Explicit revocation; access TTL bounds residual access |
| Broken authorization | Domain-enforced authz (this module only authenticates) |
| Endpoint abuse | Throttling, bounded attempts, alerting |

*No security certification claimed — architectural mitigations only.*

## 16. Audit

Auth-relevant events → `AuthenticationAuditEvent` → `AuditLog`: registration, verification, login success/fail, OTP request/resend/fail/lockout, refresh rotation + **reuse detection**, logout, revocation, role assign/remove, account status change. Separate from analytics/observability per ADR-013.

## 17. Notifications Integration

Auth produces payloads (`OTPRequest`, `SecurityAlert`, `SessionRevoked`, `RoleChanged`); Notifications owns delivery + channels. Notifications **never** makes authentication decisions.

## 18. Error Model

RFC 9457 Problem Details + stable codes (`AUTH_OTP_EXPIRED`, `AUTH_OTP_INVALID`, `AUTH_RATE_LIMITED`, `AUTH_TOKEN_EXPIRED`, `AUTH_TOKEN_REVOKED`, `AUTH_TOKEN_REUSE_DETECTED`, `AUTH_ACCOUNT_SUSPENDED`, `AUTH_ACCOUNT_BLOCKED`, `AUTH_ACCOUNT_DEACTIVATED`) + correlation IDs + validation detail — **never** revealing account existence/lockout internals in client-facing messages.

## 19. Transaction & Consistency Rules

- `User` + `UserSystemRole(Audience)` creation in **one transaction** at registration.
- OTP verify + token issue: challenge-consumption + user-authenticate + token-write atomic.
- Refresh rotation: consume-old + write-new + family-update in **one transaction** (idempotent — same-family replay fails).
- Revocation: session/family update + audit write atomic.
- PostgreSQL authoritative; unique constraints enforce identity invariants; row-lock for concurrent refresh on same family.

## 20. Testing Strategy (not implemented)

- Unit: OTP generation/verify/consume, token rotation/reuse-detection, role-join integrity.
- Integration: register→verify→login→refresh→logout round-trip; revocation propagation; suspension→session-invalidation.
- Security: enumeration-diff probing, brute-force/lockout, reuse-detection, CSRF, concurrent-session, role-assignment boundaries.
- Boundary tests: TalentSkill/ProjectContributionRole never grant access; Judge scoped-access (not blanket).

## 21. Open Questions (Product Owner — preserved, not resolved)

1. Password support + timing (ADR-006 open)
2. Admin/Judge MFA timing
3. OTP delivery provider (SMS/email)
4. Exact access/refresh durations, OTP TTL
5. Session/device management UX scope
6. Account recovery policy (lost device/number)

## 22. Traceability

- **FRS:** §5 channels · §6 roles · §7 platform security · §8 registration/account status · §30 audit · §35 OTP-first MVP, social deferred
- **ADRs:** ADR-001 module boundary · ADR-003 PG · ADR-006 auth (primary) · ADR-007 REST/error model · ADR-009 WS auth · ADR-012 single-deployable · ADR-013 audit-separate
