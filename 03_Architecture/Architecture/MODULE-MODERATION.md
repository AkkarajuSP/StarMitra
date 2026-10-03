# Module Design — 18: Moderation

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 18)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–17 without modification. **M18 owns moderation decisions — never the underlying business content.**

## 1. Purpose

Own the moderation domain: reports → cases → review → decisions → actions, restriction signals, evidence references, escalation, appeals, and auditability — consumed by every content-owning module as the authoritative moderation state `[FRS §26]`.

## 2. Ownership Boundary — decisive rule

```text
Reported MediaAsset  → M04 owns the asset;   M18 owns the moderation decision
Reported Message     → M06 owns the message; M18 owns the decision
Reported Submission  → M10 owns the entry;   M18 owns the decision
Restricted Profile   → M02/M01 own profile;  M18 owns the restriction decision
```

**Decision owner ≠ content owner — ever.**

## 3. Module Boundary

| Owns | Does NOT own |
|------|--------------|
| `ModerationReport`, `ModerationCase`, `ModerationDecision`, `ModerationAction`, `ModerationEvidenceReference`, `ModerationRestriction`, `ModerationAppeal`*(open)*, `ModerationPolicyReference`*(versioned)* | User/Profile/Media/Message/Room/Portfolio/Competition/Submission/Vote/Judge/Score/Progression/Leaderboard/Notifications |

## 4. FRS Scope `[FRS §26]` — traced

Reporting · user/content moderation · admin capability · auditability · visibility/restriction · privacy/security. No trust-and-safety platform invented; workflow/policy/retention gaps = open.

## 5. Reporting Model

`ModerationReport`: reporterId, targetRef *(typed reference — not a copy)*, reason/category, description, evidenceRef, createdAt, status, caseId. Supported targets per FRS: user/profile, media, message/conversation, room/project content, portfolio, competition/submission. **Reporter identity not exposed unnecessarily.**

## 6. Moderation Case

`Report → ModerationCase → Review → Decision → Action`. Multi-report→one-case, merge, escalation semantics = **open**. Four distinct states: **report state ≠ case state ≠ decision state ≠ restriction state** — never collapsed.

## 7. Moderation States

Conceptual *(proposal/open)*: `Reported → Open → UnderReview → Actioned | Dismissed | Escalated → Closed | Appealed`. Not silently adopted.

## 8. Decisions

`ModerationDecision`: outcome *(examples: no-violation / warning / content-restriction / content-removal / account-restriction-temp / account-restriction-permanent / escalation — taxonomy open)*, attributable + timestamped + reasoned + auditable + target/case-linked + reproducible.

## 9. Action Contract — decision vs enforcement

```text
M18: records ModerationDecision + issues ModerationAction (restriction-request)
         ↓ cross-module contract
Owning module (M01/02/04/06/10/16/…): applies its own domain state transition
```

M18 = decision + audit; source module = domain enforcement. Never duplicated state.

## 10. Visibility ≠ Authorization — preserved

Moderated content hidden at backend via visibility + authz checks — never frontend-only hiding. M18 supplies moderation-state read contracts; doesn't replace M01 authorization.

## 11. User Restrictions

Restriction signals (account/messaging/content-creation/participation — taxonomy **open**): M18 records decision; **M01/M02/owning module enforces** the domain restriction via its own state model.

## 12. Media Moderation (M04)

`ModerationEvidenceReference → MediaAsset`. **Technical safety scanning ≠ human moderation decision** — separate concerns; automated/AI moderation = future/open. M04 remains asset lifecycle owner.

## 13. Message Moderation (M06)

Report messages/conversations/attachments by reference. Least-privilege: moderators see only moderation-relevant scope — private conversations never broadly exposed.

## 14. Competition / Submission Moderation (M09–M16)

Submission restricted → M10 owns state. Result/leaderboard hidden → M16 owns publication. **M18 never modifies scores/rankings** — M14 independence preserved; moderation affects eligibility/validity only through defined contracts.

## 15. Reporter / Target Privacy

Reporter identity, reported-user info, private content, moderator notes, evidence, internal decisions — all access-controlled; internal moderation data never exposed to ordinary users. Transparency/appeal visibility = open.

## 16. Evidence

`ModerationEvidenceReference` → target resource (Media/Message/Submission/Profile/Project) — **references, never copied binaries**; access-controlled + auditable; snapshot-for-preservation = open PO decision.

## 17. Moderator Access — system-level only

`TalentSkill`/`ProjectContributionRole`/`JudgeExpertise`/`JudgeAssignment` **never authorize moderation**. Moderator/admin/super-admin distinction — role model = **open** (FRS silent on granularity). Conflict-of-interest rules = open.

## 18. Review / Escalation

`Report → triage → review → decision → action → closure`; escalation to higher/specialist/legal review — **open** (FRS silent on staffing/workflow).

## 19. Appeals

FRS-explicitness unclear → **extension point, not mandatory MVP**; if supported: appeal is a separate record preserving original decision — **original decision never silently overwritten.**

## 20. Auditability

Every decision explainable: target + report/case + decision + action + actor + timestamp + reason + before/after + evidence + correlation. Moderation-specific refs link to platform `AuditLog` — no competing audit.

## 21. Notifications (M17)

Signals: report-received*(optional)*, action-taken, content-removed, restriction, appeal-outcome — M17 delivers; notification failure never changes moderation state.

## 22. API Boundary (conceptual — ADR-007)

`POST /api/v1/moderation/reports` · `GET /reports/me` · `GET /cases` *(moderator)* · `GET /cases/{id}` · `POST /{id}/assign` · `POST /{id}/decide` · `POST /{id}/action` · `GET /{id}/history` · `POST /appeals` *(if open)*. `/api/v1/moderation` — moderator-scoped; RFC 9457; idempotent; audited.

## 23. Observability

Report volume, case latency, pending queue, failed actions, action retries, evidence-access failures, notification failures, reconciliation — telemetry ≠ audit.

## 24. Data Integrity

PG invariants: report-target valid, case linkage, decision/action/actor/evidence/restriction/audit refs; duplicate report processing prevented; duplicate actions prevented; silent state replacement prohibited.

## 25. Failure / Recovery

**Decision durable even if downstream enforcement fails** — failed enforcement surfaced + recoverable; duplicate reports deduplicated; evidence-unavailable handled; restart-resumable; partial workflow reconciled. No in-memory authority.

## 26. Security

Unauthorized-moderator access, evidence leakage, reporter leakage, API abuse, privilege escalation, tampering, self-moderation conflicts — least-privilege enforced; COI = open.

## 27. Versioning / Policy

`ModerationPolicyReference` versioned (reason/action taxonomies, rules) — historical decisions explainable against original policy version; policy changes never rewrite history.

## 28. Open Product Owner Decisions

Report categories, target types, duplicate handling, case-merge, lifecycle, decision/action taxonomy, restriction durations, role model, escalation, appeals, reporter anonymity, evidence/data retention, automated/AI moderation, COI rules, notification policy, transparency.

## 29. FRS Traceability

`[FRS §26]` moderation/reporting · visibility §11 · media §10 · messaging §12 · rooms §13 · competitions §14–17 · notifications §31 · audit §30 · roles §6 · privacy §9.

## 30. ADR Validation

ADR-001 module-not-service ✓ · ADR-003 PG ✓ · ADR-006 auth ✓ · ADR-007 API ✓ · ADR-008 media ✓ · ADR-010 no-cache-authority ✓ · ADR-012 jobs ✓ · ADR-013 analytics boundary ✓ · source modules stay owners ✓ · no infra ✓

## 31. Acceptance Validation

M18 owns decisions only ✓ · content ownership preserved ✓ · action contract defined ✓ · visibility≠authz ✓ · reporter/target privacy ✓ · evidence referenced not copied ✓ · system-level authz ✓ · decisions auditable + reproducible ✓ · original decisions never overwritten ✓ · M14 independence ✓ · skills/roles never authorize ✓ · PG authoritative ✓ · no infra ✓ · no impl ✓
