# Module Design — 20: Judge Portal

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 20 — final module)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–19 without modification. **M20 is a presentation/orchestration/access layer for judges — never a domain owner.**

## 1. Purpose

Provide judges a web portal for their authorized scope: assigned competitions/categories/rounds, eligible submissions, applicable published rubrics, evaluation workflow, status, evidence, deadlines, and released results `[FRS §19–§21]`. M20 orchestrates; M12/M13/M10/M14–M16 own the domain truth.

## 2. Critical Authorization Rule

```text
User → Judge identity → JudgeAssignment → {competition|category|round} scope → eligible Submission
```

- `SystemRole=Judge` (M01) grants **capability**; `JudgeAssignment` (M12) grants **scope**
- **Never** authorize via `TalentSkill`, `UserTalentSkill`, `JudgeExpertise`, `ProjectContributionRole`, competition participation, frontend route, or leaderboard visibility — the distinction is explicit throughout.

## 3. M20 vs Domain Ownership

```text
"Show my assignments"   → M12 JudgeAssignment authoritative
"Show submissions"      → M10 Submission authoritative
"Show rubric"           → M13 EvaluationTemplateVersion authoritative
"Submit evaluation"     → M13 JudgeEvaluation authoritative
"Show ranking/result"   → M14/M16 authoritative
```

M20 = interface + orchestration; no duplicated entities or state machines.

## 4. Module Boundary

| M20 does | M20 does NOT own |
|----------|-------------------|
| Judge dashboard/UI, assignment-scope navigation, submission review surface, rubric display, evaluation entry workflow, result views, notifications surface | Judge/JudgeExpertise/JudgeAssignment (M12), Submission (M10), MediaAsset (M04), EvaluationTemplate/Version/Criterion (M13), JudgeEvaluation (M13), Score/Ranking/Qualification (M14), Progression (M15), Leaderboard (M16) |

## 5. Assignment Dashboard

Assigned competitions/categories/rounds, pending evaluations, completed evaluations, judging deadlines, recently submitted, released results *(metrics open — not invented)*.

## 6. Assignment Scope — narrowest effective

M12 scope applied strictly: `Competition A + Category B + Round 2` grants exactly that — never A/Cat-C, never another competition, never Rounds 1 or 3 unless a valid assignment covers them. Server-side validation on every request; **frontend filtering is never enforcement.**

## 7. Assigned Submission Access

`Judge → active JudgeAssignment → scoped competition/category/round → eligible Submission → required evidence` — server-validated chain. Never expose unrelated/hidden/private/out-of-scope submissions or restricted content without authorization.

## 8. Submission Review (M10 + M04)

Displays: submission metadata, permitted participant/project info, media via M04 signed access, competition context, permitted contributor info, status, evidence — M10/M04 authoritative; M20 never modifies.

## 9. Blind / Anonymous Judging

FRS-explicitness unclear → **optional/PO decision**, not mandatory. If supported: presentation-level identity masking — never weakens authz; hidden identity never leaks through payloads, media metadata, filenames, URLs, or deep links.

## 10. Rubric Display (M13)

Shows applicable published version: criteria, descriptions, weights, scoring scale, comment fields, instructions. M13 authoritative; published versions immutable; M20 never edits.

## 11. Judge Evaluation Workflow

Open evaluation → enter criterion scores → comments (if configured) → completeness validation → save draft*(if M13 supports)* → submit/finalize → own status. `JudgeEvaluation` owned by M13; no duplicate entity here.

## 12. Independent Judging — enforced

Judge never sees another judge's scores/comments/drafts/status pre-release — not via UI, API, payloads, client state, or deep links. M12/M13 confidentiality authoritative.

## 13. Evaluation Lifecycle

`NotStarted → Draft → Submitted → Locked | Released` *(examples — open)*. Portal presentation state ≠ M13 authoritative state; draft support only if M13 provides it.

## 14. Deadlines / Timing

Displays evaluation deadlines, round timing, judging windows — M09/M12/M13 timing authoritative; countdown is presentation only; server validates.

## 15. Evaluation Validation — server-side

Active assignment + in-scope submission + valid applicable rubric version + required criteria + in-scale scores + eligible submission + open window — **M13 validation authoritative**; M20 never implements conflicting rules.

## 16. Results / Feedback Visibility

Own submitted eval + *(if released)* aggregate score/ranking/qualification/leaderboard — **judges don't automatically see results**; visibility = PO decision; never cross-judge confidential data.

## 17. API Boundary (conceptual — ADR-007)

`GET /api/v1/judges/me/assignments` · `/{contextId}/scope` · `GET /api/v1/judges/me/submissions` · `/{id}` · `/{id}/evidence` · `GET /api/v1/judges/me/rubrics/{contextId}` · `GET/POST/PUT /api/v1/judges/me/evaluations` · `/{id}/submit` · `GET /{id}/history` · `GET /api/v1/judges/me/results` (release-gated). Judge self-service namespace standardized on `/api/v1/judges/me` (CM-07); DTOs; Problem Details; idempotent submission.

## 18. Security

Cross-assignment/cross-competition denial, unauthorized evaluation submit, tampering, score manipulation, IDOR/enumeration, confidential-judge-data exposure, private-participant leakage, media-URL leakage, revoked-assignment enforcement, session compromise — M01 auth + M12 scope + M13 authority + M04 media control; never frontend-only.

## 19. Judge Confidentiality

Own-eval visibility + cross-judge invisibility + aggregate release timing + admin-access boundaries + audit access — **no side channel around M12/M13 rules.**

## 20. Notifications (M17)

Assignment/changes, window-opening, deadlines/reminders, submission updates, result publication — M17 delivers; M12/M13/M20 own events; failure never alters state.

## 21. Auditability

Assignment access, eval started/saved/submitted/reopened/corrected, result access — via established platform audit; M13/M12 authoritative for their records.

## 22. Observability

Portal latency, assignment/evaluation/media loading failures, authz failures, duplicate submits, session issues — telemetry ≠ business truth.

## 23. Failure Handling

Eval-submit retry/duplicate → idempotent; concurrent submit → guard; revoked assignment mid-eval → scope re-validated + blocked; rubric-version change → eval bound to version at start; submission withdrawn/restricted mid-eval → blocked + flagged; restarts → domain state authoritative; **no in-memory authoritative eval state.**

## 24. Admin / Judge Separation

Judge Portal ≠ Admin Portal — separate authorization boundaries; `SystemRole=Judge` grants no admin capability; no admin controls exposed; moderator/admin access to judge data via their own roles.

## 25. Team / Project Submissions

Team = one competition entry; judge evaluates the entry per rubric; contributors = context only; `ProjectContributionRole`/`TalentSkill` never authorize; votes not split; no contributor-level scoring unless competition config requires — **never multiple evaluations just because multiple contributors exist.**

## 26. Multi-Talent Model — preserved

Skills = capability metadata only; `JudgeExpertise` doesn't auto-authorize a competition; `JudgeAssignment` determines access; eligibility from M09; rubric from M13 — concepts never collapsed.

## 27. Versioning / Reproducibility

`JudgeEvaluation` always references judge + submission + competition/category/round + **exact rubricVersion** + criterion scores + scale + timestamps + audit — displayed via references; UI never silently rewrites history.

## 28. Open Product Owner Decisions

Dashboard metrics, blind judging, draft/save, reopen/correction, deadline policy, result visibility to judges, historical-eval access, aggregate visibility, post-result access, revocation-mid-eval, replacement behavior, notification policy, audit visibility, media rules, participant-identity display.

## 29. FRS Traceability

`[FRS §6]` roles · `[§19]` judges/assignments · `[§14–15]` competitions/rounds · `[§16]` submissions · `[§10]` media · `[§20]` rubrics · `[§22–25]` scoring/ranking/qualification/progression/leaderboards · `[§31]` notifications · `[§30]` audit/security.

## 30. ADR Validation

ADR-001 monolith boundary ✓ · ADR-003 PG ✓ · ADR-004 React portal ✓ · ADR-006 auth/session ✓ · ADR-007 API ✓ · ADR-008 media ✓ · ADR-009 realtime ✓ · ADR-010 no-cache-authority ✓ · ADR-012 deployment ✓ · ADR-013 analytics ✓ · no DB bypass ✓ · no infra ✓ · no duplicated entities ✓

## 31. Acceptance Validation

M20 = presentation layer ✓ · M12 assignment = authz boundary ✓ · M13 eval/rubric authoritative ✓ · M10/M04 sources ✓ · independent judging enforced ✓ · narrowest scope ✓ · server-side validation ✓ · confidentiality preserved ✓ · judge ≠ admin ✓ · team = one entry ✓ · skills/roles never authorize ✓ · history explainable ✓ · no infra ✓ · no impl ✓
