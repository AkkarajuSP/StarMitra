# Module Design — 12: Judge Management

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 12)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–11 without modification. Rubrics/evaluations are contract-referenced only.

## 1. Purpose

Own the **judge identity within the judging domain**: judge profile/expertise, judge assignments, assignment lifecycle, and the *authorization scope* that constrains a judge to only their assigned submissions. Not the evaluation itself.

## 2. Core Model — distinct concepts

`SystemRole(Judge)` (M01 — capability) ≠ `Judge` (this module's domain identity) ≠ `JudgeExpertise` (qualification) ≠ `JudgeAssignment` (scope) ≠ `JudgeEvaluation` (M13's domain) ≠ `TalentSkill` (M03 — irrelevant to judging).

- **SystemRole=Judge grants judge-capability, not data access** — assignment grants scope.
- **TalentSkill never grants judge permission** — a Director-skill user isn't automatically a judge.

## 3. FRS Scope `[FRS §19]` — traced

Judge role · assignments by expertise/category/competition/round · judges see only assigned submissions · independent judging · judge portal (later) · configurable rubrics (later — M13).

## 4. Module Boundary

| Owns | Does NOT own |
|------|--------------|
| `Judge`, `JudgeExpertise`, `JudgeAssignment`, assignment-scope config | User/SystemRole/auth (M01), TalentSkill (M03), Competition/Category/Round (M09), Submission (M10), Vote (M11), **EvaluationTemplate/Rubric/JudgeEvaluation/Score/Ranking** (M13–15), moderation policy, notification delivery, portal UI |

## 5. Judge Entity

`Judge`: id, userId (→M01), status, assignment-eligibility, timestamps. **Every Judge must hold SystemRole=Judge** *(policy open if a provisional-judge model is wanted)*; user↔judge is 1:1; expertise areas can be multiple.

## 6. Judge Expertise — critical distinction

`JudgeExpertise`: judgeId + expertiseRef + qualification/status. **TalentSkill = capability (M03); JudgeExpertise = judging qualification (M12) — never equated.** Expertise may *reference* skill IDs (illustrative examples: Vocal/Acting/Direction/Writing) but is its own concept — verification/taxonomy = **OPEN**.

## 7. Judge Assignment — the core responsibility

`JudgeAssignment`: judgeId + competitionId + optional categoryId + optional roundId + scope + state + assignedAt/revokedAt. **Scope must be explicit** — no blanket "Judge sees all submissions" default.

## 8. Assignment Granularity

Supported scopes: competition / category / round *(FRS-named)*; submission-level = **OPEN implementation option**, not product requirement. Overlapping scopes → precedence = **open product policy**, not invented.

## 9. Assignment Lifecycle

`Pending → Active → Revoked → Completed` *(proposed states — open)*. Covers create, accept*(if applicable)*, activate, revoke, replace, reassign. Judge-acceptance workflow = open.

## 10. Conflict of Interest

FRS defines no detailed COI workflow — **explicitly OPEN product decision**. Design an extension point only (judge↔project/participant/team relationships; self-evaluation prevention) — no mandatory workflow implemented.

## 11. Independent Judging

Assignment isolation preserved — one judge's scope/evaluations never exposed to another judge (eval-visibility policy = M13's domain; M12 provides the isolation boundary).

## 12. Submission Access Control

`User → Judge → JudgeAssignment → {Competition|Category|Round} → eligible Submissions` — the authorization chain. **Never** via TalentSkill/ProjectContributionRole/participation. Protected: cross-assignment access, IDOR, enumeration, unauthorized result visibility.

## 13. Evaluation Boundary

`JudgeEvaluation`, `EvaluationTemplate`, `EvaluationCriterion`, scoring = **M13–15**. M12 supplies the assignment/authz context only.

## 14. Competition / Submission Integration

`JudgeAssignment` refs `Competition`/`Category`/`Round` (M09-authoritative — validated against active context); judge-visible submissions resolved via **scope → Submission read-contract** (M10) — no judge-owned submission copies.

## 15. Judge Portal Boundary (future UI)

Domain capabilities only: judge profile/expertise, active assignments, scope resolution, assigned-submission lookup, assignment status. **Portal UI = separate later concern.**

## 16. Admin Boundary

Domain ops for future Admin Portal: create/deactivate judge, assign/revoke, update expertise, manage scope. Explicit system-role authz — not all admin roles get all judge ops *(open)*.

## 17. Security

SystemRole=Judge validated; assignment-scope enforcement on every access; revoked→immediate denial; cross-category/round blocked; IDOR; impersonation; privilege escalation. **TalentSkill never grants; expertise never auto-grants without assignment.**

## 18. Data Model

| Entity | Purpose | Invariants |
|--------|---------|-----------|
| `Judge` | domain judge identity | userId unique; SystemRole=Judge required |
| `JudgeExpertise` | qualification | judgeId+expertiseRef; multiple allowed |
| `JudgeAssignment` | scope grant | judgeId+competitionId(+categoryId+roundId); scoped, stateful |
| *(proposal)* `AssignmentScope` | scope detail | only if granularity needs it |

## 19. API Surface (conceptual — ADR-007)

`POST /api/v1/judges` · `GET/PUT /api/v1/judges/me` · `PUT /api/v1/judges/{id}/expertise` · `POST /api/v1/judges/{id}/assignments` · `PUT /api/v1/assignments/{id}/revoke` · `GET /api/v1/judges/me/assignments` · `GET /api/v1/judges/me/scope/{competitionId}` · `GET /api/v1/judges/me/submissions` (scope-resolved). Errors: `JUDGE_NOT_FOUND`, `ASSIGNMENT_SCOPE_INVALID`, `JUDGE_FORBIDDEN`, `ASSIGNMENT_REVOKED`, `CROSS_SCOPE_DENIED` + RFC 9457.

## 20. Concurrency / Consistency

Duplicate assignment → unique constraint; concurrent changes → optimistic locking; revocation during judging → state transition + immediate scope-denial; judge deactivation → cascade assignment revocation; competition closure → assignments complete. PG-only.

## 21. Audit / Observability

Judge create/deactivate, expertise changes, assignment create/change/revoke, unauthorized-access attempts, scope changes — judge/actor/competition/category/round/assignment/correlation IDs. `AuditLog` separate.

## 22. Notifications

Signals: `JudgeAssigned`, `AssignmentChanged`, `AssignmentRevoked`, `JudgingOpened` — delivery owned by Notifications.

## 23. Testing (not implemented)

Judge creation/role validation, expertise, assignment/scopes (competition/category/round), revocation, deactivation cascade, submission authz, cross-scope denial, IDOR, privilege escalation, concurrent changes, audit, signals.

## 24. Open Decisions

Onboarding/approval, expertise taxonomy + verification, assignment acceptance, granularity + precedence, COI policy, replacement, revocation semantics, deactivation behavior, judge-identity visibility/anonymity, compensation, notifications.

## 25. Acceptance Validation

M12 owns judge-domain context ✓ · M01 identity authoritative ✓ · assignments determine access ✓ · expertise ≠ auto-access ✓ · TalentSkill never grants ✓ · M09/M10 authoritative ✓ · rubrics/evals outside M12 ✓ · assigned-only access ✓ · revocation enforceable ✓ · audited ✓ · PG authoritative ✓ · no infra ✓ · no impl ✓

## 26. Traceability

- **FRS:** §19 judges/assignments · §20 rubrics (boundary) · §22 scoring (boundary) · §30 audit · BR-2
- **ADRs:** ADR-001 boundary · ADR-003 PG · ADR-006 auth · ADR-007 API
- **Modules:** M01 role · M09 competition context · M10 submissions · M13+ consumers · Notifications
