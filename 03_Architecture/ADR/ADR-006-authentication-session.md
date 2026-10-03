# ADR-006 — Authentication & Session Architecture

| Field | Content |
|-------|---------|
| ADR | ADR-006 |
| Register entry | OD-06 — Authentication / Identity |
| Status | **ACCEPTED** |
| Date | 2026-10-03 |
| Baseline refs | §12 Security Architecture, §20 Decisions, SECURITY-ARCHITECTURE.md |
| Related | ADR-007 (API auth), ADR-009 (WS auth), ADR-004/005 (client transports) |

## Decision

**First-party Spring Security** authentication: OTP as the primary mechanism `[FRS §8]` + short-lived JWT access tokens + opaque, server-persisted refresh tokens (rotation + reuse detection + revocation) — **one unified mechanism** across all clients, differing only in transport.

## Context

`[FRS §8]` mobile/email + OTP "or configured authentication mechanism"; account states Active/Suspended/Blocked/Deactivated; **TalentSkill ≠ SystemRole** `[BR-2]` — authN and authZ are separate.

## Alternatives Considered

- **Managed IdP (Cognito/Firebase Auth/Auth0-class)** — viable fallback; triggers provider + per-MAU cost review; rejected as the default.
- **Hybrid** — two systems to secure; rejected.
- **Session-only (no JWT)** — awkward for RN/WebSocket; rejected as unified mechanism.

## Rationale

Full control of OTP UX/cost; zero new infra (PostgreSQL only); revocable sessions support account suspension immediately; FRS's "configured mechanism" fits; no vendor lock-in on identity.

## Guardrails / Constraints (binding)

1. First-party Spring Security is the MVP authentication architecture.
2. OTP is supported as the primary authentication mechanism.
3. Optional password support remains an **open product decision**.
4. Short-lived JWT access tokens are used.
5. Refresh tokens are opaque, server-controlled and persisted.
6. Refresh token rotation is required.
7. Refresh-token reuse detection is required.
8. Server-side session/token revocation must be supported.
9. Web uses secure HTTP-only cookie transport with appropriate CSRF protection.
10. React Native uses bearer authentication with platform-secure credential storage.
11. Admin/Judge use the same authentication architecture.
12. MFA for Admin/Judge remains a **separate security/product decision**.
13. WebSocket connections must authenticate and respect session/token revocation.
14. Social login is **deferred**.
15. External identity providers are **not required** for MVP.
16. Authentication, authorization, talent skills and project contribution roles remain separate concepts.
17. OTP security controls must be explicitly designed before implementation.
18. No Redis or other infrastructure is implied by this decision.

## Consequences

Team owns OTP throttling/enumeration/replay defenses — mitigated by proven patterns + security review; instant revocation; no per-MAU cost.

## Deferred Items / Future Triggers

Managed-IdP revisit (scale/cost trigger); social login (FRS "future"); MFA policy for admin/judge.

## Open Items

- Password in addition to OTP at MVP — product decision
- MFA for admin/judge timing — product/security decision
- SMS/OTP provider selection — implementation-phase
