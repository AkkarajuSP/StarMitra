# StarMitra — Security Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Draft for review

## 1. Authentication

| Requirement | Source | Approach |
|-------------|--------|----------|
| Registration via mobile/email with OTP or configured mechanism | `[FRS §8]` | OTP challenge flow; mechanism pluggable |
| Secure login/logout | `[FRS §8]` | Stateless session tokens + refresh `[Proposed]` |
| Password reset/recovery where password auth enabled | `[FRS §8]` | Token-based recovery flow |
| Optional social login | `[FRS §8]` | **Future** — design for provider abstraction now |
| Consent to Terms/Privacy/guidelines | `[FRS §8][§26]` | `ConsentRecord` with versioned policy refs `[Proposed]` |

`[Open OD-5]` Identity provider decision (managed IdP vs in-house). Constraints either way: OTP-first flows, judge/admin accounts possibly separate MFA policy `[Proposed]`.

## 2. Authorization Model

### 2.1 The mandatory separation `[PD-02][BR-02]`

```text
Authorization input   = UserSystemRole → SystemRole   (ONLY)
TalentSkill           = profile data                  (NEVER authz)
ProjectContributionRole = project context             (NEVER authz)
Competition category  = entry context                 (NEVER authz)
```

Creative skills MUST NOT grant system permissions `[BR-02]`. Having "Judge" as a creative-sounding category grants nothing — the Judge *system role* is an administrative assignment `[FRS §6]`.

### 2.2 RBAC + resource scoping `[Proposed]`

System roles map to permission sets. Beyond role checks, resource scoping applies:

| Scope rule | Source |
|------------|--------|
| Judges see only assigned submissions | `[FRS §19]` |
| Room/project content limited to members per room visibility | `[FRS §13]` |
| Content visibility: Public/Followers/Collaboration-Only/Private | `[FRS §9][§10]` |
| Admin actions limited by Admin vs Super Admin boundary | `[FRS §6]` |
| Overrides require explicit authorization + audit | `[BR-13]` |

Enforcement: server-side middleware + domain-level checks; clients never trusted `[Proposed]`.

## 3. Account Security

`[FRS §8]` Account status machine: **Active → Suspended / Blocked / Deactivated**. Status enforced at authN middleware — suspended/blocked tokens rejected or scoped-down immediately `[Proposed]`.

## 4. Abuse Controls

`[BR-14][FRS §18]`

- Duplicate/abusive voting detection: per-user/per-device/per-IP signals, rate limits, anomaly flagging `[Proposed]`.
- Idempotency keys on sensitive mutations.
- Public surface rate limiting; OTP attempt throttling.
- Report/block controls on content, profiles, messages `[FRS §12][§26]`.

## 5. Data Protection

`[Proposed]`

- TLS everywhere; at-rest encryption for DB and object storage.
- Signed/short-lived URLs for non-public media `[FRS §9][§10]` — see MEDIA-ARCHITECTURE.
- PII minimization; location/bio visibility per user privacy controls `[FRS §9]`.
- Secrets in managed secret store; never in repo/config files.
- `[Open]` Data residency requirements — pending legal/product input.

## 6. Application Security

`[Proposed]` — standard hardening, none StarMitra-specific: OWASP ASVS-aligned input validation, parameterized queries/ORM defaults, CSRF (web), secure headers, dependency scanning in CI, upload content-type/size validation `[FRS §16][§26]`.

## 7. Security-Relevant Audit Events

`[FRS §30]` feeds the audit stream: account changes, role changes, vote events, judge assignment changes, evaluation submission/re-evaluation, rubric version changes, overrides, moderation, admin config changes. Audit records are append-only and tamper-evident `[Proposed]`.

## 8. Threat Model

To be developed in `10_Security/Threat-Model/` during design phase `[Open]`. Priority threat areas already identifiable:

| Area | Threat |
|------|--------|
| Voting | Bots, duplicate votes, vote-buying → undermines competition integrity |
| Judge evals | Collusion, leaked submissions before close `[BR-12]` |
| Media | Malicious uploads, hotlink abuse of private content |
| Messaging | Harassment → mitigated by report/block + moderation `[FRS §12]` |
| AuthZ | Skill-as-role confusion (this baseline's cardinal rule) |
