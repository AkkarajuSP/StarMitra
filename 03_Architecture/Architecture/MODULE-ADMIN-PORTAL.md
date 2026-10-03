# Module Design — 19: Admin Portal

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 19)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–18 without modification. **M19 is a presentation/orchestration/access layer — never a second owner of domain data.**

## 1. Purpose

Provide authorized administrators a unified web portal for administrative workflows and views over existing domain modules `[FRS §28]`. M19 orchestrates domain commands/queries; the domain modules remain authoritative.

## 2. Admin Portal vs Domain Ownership — the decisive rule

```text
Admin Portal says:   "Create competition"   → M09 owns Competition
                     "Assign judge"         → M12 owns JudgeAssignment
                     "Publish rubric"       → M13 owns EvaluationTemplateVersion
                     "Review case"          → M18 owns ModerationDecision
                     "Show leaderboard"     → M16 owns projection
```

**M19 = interface + orchestration. Domain modules = authoritative business ownership. No business logic in M19.**

## 3. Module Boundary

| M19 does | M19 does NOT |
|----------|--------------|
| Admin UI surface, domain command orchestration, admin views, workflow UX, aggregate operational display | Own any domain entity (User/Profile/Skill/Media/Message/Room/Portfolio/Competition/Submission/Vote/Judge/Rubric/Evaluation/Score/Ranking/Progression/Leaderboard/Notification/ModerationCase); bypass domain authz; duplicate business logic |

## 4. Admin Authorization

- **SystemRole controls admin capability** (M01) — `TalentSkill`/`ProjectContributionRole`/`JudgeExpertise`/`JudgeAssignment` never grant admin access
- Admin / Super-Admin distinction *(if FRS-supported)*; Moderator/Competition-Manager/etc. = **OPEN roles**
- **Backend authorization is authoritative — frontend route visibility is not authorization**

## 5. Route Boundary (conceptual)

`/admin/{users|skills|media|rooms|competitions|submissions|voting|judges|rubrics|results|progression|leaderboards|notifications|moderation|audit|dashboard}` — conceptual groups only; never a substitute for authz.

## 6. Dashboard

Administrative operational surface: active competitions, pending moderation, pending judge assignments, attention-required submissions, result status, system/notification status, operational alerts — **operational metrics, not FRS requirements; no separate analytics authority.**

## 7–10. Domain Orchestration Map

| Area | Portal provides | Authoritative owner |
|------|-----------------|---------------------|
| User management | search, profile view, role view, restriction/deactivation workflows | M01/M02 (+M18 for restrictions) |
| Role management | assign/revoke/view `UserSystemRole` | M01 — `TalentSkill`/`ProjectContributionRole` never appear as permissions |
| Talent taxonomy | create/update/activate/deactivate skill, view associations | M03 — configurable, not hard-coded |
| Media oversight | media status, moderation status, review/removal workflows | M04 (+M18 for decisions) |
| Rooms oversight | admin views + authorized actions | M07 |
| Competition admin | create/edit/categories/rounds/eligibility/submission-config/lifecycle | M09 |
| Submission oversight | views, evidence, history, authorized actions | M10 |
| Voting oversight | status, config, counts, abuse indicators | M11 — no direct vote edits |
| Judge management | onboarding, expertise, assignments, scope, revocation | M12 |
| Rubrics | templates, versions, criteria, weights, publish workflow | M13 — published versions immutable |
| Scoring/results | config views, aggregates, results, authorized override workflow | M14 — override via M14 mechanism only |
| Progression | decisions, readiness, next-round, authorized override | M15 |
| Leaderboards | views, publication controls, archives | M16 |
| Notifications | templates, preference views, delivery monitoring | M17 |
| Moderation | queues, review, decisions, restrictions, evidence, history | M18 |

**Each entry: M19 orchestrates the domain's own command — never recreates its logic or mutates internals.**

## 11. Audit / Activity

Platform `AuditLog` stays the audit authority — M19 provides search/filtered views/entity-history; no duplicate audit DB; access-controlled + sensitive-data-aware.

## 12. Analytics Boundary — explicit

**M19 ≠ Analytics.** ADR-013 governs analytics/reporting architecture. Portal consumes operational metrics where needed but: analytics never generates competition outcomes; M19 is not an analytics datastore; M14/M15/M16 stay authoritative. Dedicated analytics module = separate future design if required.

## 13. API / Orchestration Boundary

Portal operations map to **domain commands/queries** — no "admin mutate anything" generic APIs, no direct-database bypass. `/api/v1/{domain}`; DTOs; Problem Details; correlationId; idempotent; explicit commands.

## 14. Security

Privileged-action authorization, role-escalation protection, sensitive-user-data masking, moderation evidence, judge confidentiality, results protection, audit-info restriction, destructive-action confirmation *(MFA = open)* — least privilege; backend authz on every privileged op.

## 15. Observability

Privileged action success/failure, authz failures, API latency, domain-command failures, dashboard performance, bulk-operation status, audit-write failures — telemetry ≠ audit.

## 16. Failure Handling

Domain-command failure, partial workflow, duplicate command (idempotent), stale read models, source-module unavailable, restarts — **domain transaction boundaries remain authoritative**; multi-module workflows use orchestration/compensation conceptually; no distributed transactions.

## 17. Multi-Module Workflow — conceptual

Where one portal action spans domains (e.g., competition publish + notifications): portal issues sequential domain commands via their APIs; each domain commits in its own tx; compensation/rollback = design consideration, not silent coupling.

## 18. Data Integrity

No second transaction authority; no duplicated entities; stale-view reconciliation; domain invariants preserved.

## 19. Open Product Owner Decisions

Exact admin roles, Admin-vs-Super-Admin capabilities, MFA, destructive confirmation, bulk permissions, role-assignment approval, taxonomy permissions, competition/result/moderation admin permissions, audit visibility, data masking, retention, dashboard metrics, analytics scope, multi-tenancy.

## 20. FRS Traceability

`[FRS §28]` admin portal · §6 roles · skills §8 · competitions §14–17 · voting §18 · judges §19–21 · results §22–25 · moderation §26 · audit §30 · notifications §31.

## 21. ADR Validation

ADR-001 (module, not service) ✓ · ADR-003 PG ✓ · ADR-004 React web portal ✓ · ADR-006 auth/session ✓ · ADR-007 API ✓ · ADR-008 media ✓ · ADR-010 no-cache-authority ✓ · ADR-012 deployment ✓ · ADR-013 analytics boundary ✓ · no DB bypass ✓ · no duplicated entities ✓

## 22. Acceptance Validation

Presentation/orchestration only ✓ · domain ownership preserved ✓ · SystemRole-gated ✓ · skills/roles never grant admin ✓ · every action via domain command ✓ · no business logic ✓ · no analytics authority ✓ · no generic-mutate APIs ✓ · backend authz authoritative ✓ · audited ✓ · PG ✓ · no infra ✓ · no impl ✓
