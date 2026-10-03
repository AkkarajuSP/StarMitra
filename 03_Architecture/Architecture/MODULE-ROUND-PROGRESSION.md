# Module Design — 15: Round Progression

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 15)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–14 without modification. Scoring/ranking remain M14's domain.

## 1. Purpose

Own **what happens next**: consume M14's finalized qualification results and apply progression — advance/eliminate/retain entries, manage next-round participation, handle progression-boundary ties, finalize with full auditability `[FRS §17][§24]`. M14 says *whether* an entry qualifies; **M15 decides and executes the outcome.**

## 2. Qualification vs Progression — explicit

| Module | Answers |
|--------|---------|
| **M14** | "Entry **qualifies / does not qualify**" per scoring config |
| **M15** | "Entry **advances / does not advance**" per progression rules |

`QualificationResult` is **never silently rewritten** by M15 — consumed as input; a progression-side override is a separate auditable decision.

## 3. Round Model — consumed, not owned

`CompetitionRound` stays M09-authoritative. M15 consumes round config + controls **progression state** (a separate dimension). Progression scope (category / competition / round) = **OPEN** where FRS silent. Previous↔current↔next round relationships navigated via M09's sequence.

## 4. Progression Rules — configurable

`ProgressionConfiguration`: mode (threshold / top-N / qualification-result / combination), tie policy, eligibility filters, next-round mapping, retention/elimination rules. **No single hardcoded strategy**; exact rule precedence = open.

## 5. Round Transitions — progression-side lifecycle

Conceptual *(proposed, not final)*: `OPEN → SCORING_PENDING → QUALIFICATION_READY → PROGRESSION_READY → PROGRESSION_FINALIZED → NEXT_ROUND_OPEN`. Owns progression state only — **never a second competition lifecycle** (M09 owns that).

## 6. Entry Progression States — separate from all other states

Conceptual *(proposal/open)*: `Eligible → Qualified → Advanced | Eliminated | Withdrawn | Disqualified`. **Independent of** submission state, scoring state, qualification state, round state. A `Submission` is never marked "eliminated" — that's a `ProgressionRecord` outcome, not a submission state.

## 7. Tie Handling at the Boundary

M14 owns tie-break *calculation*. M15 consumes the finalized outcome and handles **progression-boundary** effects: tie-at-cutoff → more entries qualify *(policy open)*; resolved tie → ordered progression; unresolved tie → progression-blocked or manual-resolution path *(open)*. M15 never re-computes scores/tie-breaks.

## 8. Manual / Authorized Overrides

`ProgressionOverride` *(if supported — open)*: authorization *(matrix open)*, mandatory reason, actor, timestamp, affected scope+entry, previous→new decision, audit ref. **`ScoreOverride` (M14) ≠ `ProgressionOverride` (M15)** — separate audit events. Never mutates M14 records.

## 9. Progression Finalization

`ProgressionRecord` becomes authoritative at finalization: references M14 result version + qualification version + progression-config version + tie outcome + overrides. **Pre-finalize:** reprocessing possible per policy. **Post-finalize:** change requires explicit authorized correction; silent rewriting prohibited; auditable always.

## 10. Next-Round Participation

Contract for advancing an entry to the next round — **mechanism open** (progression record + participation activation vs domain event vs M09 command vs next-round entry creation). M15 never duplicates `CompetitionRound` or `Submission` ownership; it activates/links to M09's next-round context + M10's submission.

## 11. Team / Project Submissions — preserved

Team/project = **ONE competition entry**; progression operates on the entry; contributors never advanced as separate entries; votes stay attached; `ProjectContributionRole`/`TalentSkill` never authorize progression.

## 12. Multi-Talent Model — preserved

Multi-skill users participate under different skills; progression = participation/submission identity-driven, never skill-permission-driven.

## 13. Withdrawal / Disqualification — boundary

Each condition owned by its module: participant/submission withdrawal (M10), disqualification (Moderation), invalid submission (M10), revoked judge results (M12/13), invalidated votes (M11). M15 **consumes** these valid states and applies progression rules — never owns the disqualification decision. Policy precedence = open.

## 14. Reprocessing / Idempotency

Duplicate requests → idempotent (same `ProgressionRecord`); job retry → safe re-run; concurrent finalize → state guard; recalculation → new versioned record preserving prior + audit; already-finalized → rejected; repeated advancement → deduplicated. PG-durable; **no in-memory authoritative state.**

## 15. Failure Handling

Missing M14 result → progression-blocked + alert; incomplete scoring → wait/block; unresolved tie → blocked or manual path *(open)*; invalid qualification → rejected; missing next round → blocked; cancelled competition → progression-halted; withdrawn/disqualified → excluded at intake; partial execution → recoverable via durable state + reconciliation; restart → resume.

## 16. Progression Configuration

`ProgressionConfiguration` — **ownership boundary open** (likely M09 config consumed by M15, or M15-owned): mode, threshold, top-N, tie policy, eligibility, next-round mapping. **Versioned; immutable once progression begins.** Never duplicated with M14 scoring config or M09 competition lifecycle.

## 17. Auditability — every decision explainable

`Competition/Category/Round → M14 result → QualificationResult → ProgressionConfig → TieOutcome → ProgressionDecision → NextRoundParticipation → Override(if any)` — actor/system, timestamp, before/after, config/version refs, reason, correlation ID. Established audit model, not a competing one.

## 18. API Boundary (conceptual — ADR-007)

`GET /api/v1/progressions/{contextId}/status` · `POST /api/v1/progressions/{contextId}/evaluate` (readiness) · `POST /{contextId}/finalize` · `GET /{contextId}/decisions` · `GET /entries/{id}/history` · `POST /{decisionId}/override` (authorized) · `GET /{roundId}/next-participation`. `/api/v1/progressions`; DTOs; Problem Details; idempotent.

## 19. Downstream Contract → M16

`{competitionId, categoryId, roundId, entry, finalScoreRef, rank, qualificationResult, progressionResult, releaseStatus}` → M16 owns leaderboard presentation — never decides progression.

## 20. Observability

Progression-execution success/failure, duration, pending progressions, failed transitions, duplicate attempts, recovery/retry, reconciliation — telemetry ≠ business truth; audit separate.

## 21. Security / Authorization

Progression admin = explicit system authorization — **never** via TalentSkill, ProjectContributionRole, JudgeExpertise, JudgeAssignment, or competition participation. Exact role matrix = open.

## 22. Data Integrity

One authoritative `ProgressionRecord` per entry+round+version; finalized immutable; next-round advancement non-duplicable; M14-result ref traceable; config-version retained; team entry stays one; progression records never modify source scoring/voting/evaluation records.

## 23. Versioning / Reproducibility

Historical progression reproducible from: competition/category/round + progression-config version + M14 qualification/ranking/tie outcome + eligibility state + overrides + next-round mapping. Post-finalize config changes never rewrite history.

## 24. Open Product Owner Decisions

Progression modes, threshold/top-N semantics, boundary-tie behavior + unresolved handling, config ownership, finalize timing, correction/reopen policy, withdrawal/disqualification precedence, next-round enrollment mechanism, override matrix, publication timing, late-correction handling, cancellation interaction.

## 25. FRS Traceability

`[FRS §17]` progression · `[§16]` submissions · `[§18]` voting · `[§22–24]` scoring/ranking/qualification · `[§30]` audit · team/multi-talent rules per BR-2 — FRS terminology preserved.

## 26. Acceptance Validation

Progression≠qualification ✓ · M14 result never rewritten ✓ · M09 rounds authoritative ✓ · team=one-entry ✓ · configurable rules ✓ · tie handling consumed not recalculated ✓ · finalized immutable + auditable ✓ · overrides separate + auditable ✓ · next-round without duplicating ownership ✓ · skills/roles never authorize ✓ · PG authoritative ✓ · no infra ✓ · no impl ✓
