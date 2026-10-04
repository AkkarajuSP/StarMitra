# M10 — Submissions — Implementation

**Slice:** eleventh business implementation. **Status: complete.** M10 owns submission truth — never votes/judges/rubrics/scoring/progression/leaderboards.

## Ownership

`submissions` (@Version), `submission_media`, `submission_contributors` (DB-02 live-ref+snapshot), `submission_history` (append-only) — full V1.8 surface.

## Contract surface (9 ops, unchanged)

createSubmission · getSubmission · attachMedia · declareContributors (PUT) · submit · finalize · withdraw · history · list (roundId/categoryId filters).

## Models

- **Submission**: DRAFT→SUBMITTED→FINALIZED; DRAFT/SUBMITTED→WITHDRAWN (FINALIZED frozen). Comp/category derived from M09 participant — never client-supplied. One per participant per non-WITHDRAWN round → CONFLICT.
- **Team submission**: ONE submission per PROJECT participant — never split. Contributors = `member_ref` (project member) + `role_ref` (M07 contextual role) — declared, never inferred from TalentSkill, never manufactured.
- **DB-02 snapshots**: finalize captures `snapshot_member_display`+`snapshot_role_name`+`captured_at` — attribution survives later M07 changes.
- **History**: append-only trail (created/DRAFT→SUBMITTED→FINALIZED/WITHDRAWN).

## Enforcement

- **Eligibility**: participant ACTIVE (M09 `participantView`), round `belongsTo` comp, category required.
- **Deadline**: `round.end_at` via M09 `roundWindow` — submit after → STATE_TRANSITION_INVALID.
- **Freeze**: FINALIZED/WITHDRAWN → media attach/contributors rejected.
- **Auth**: USER → caller==participant user; PROJECT → active M07 member. IDOR → NOT_FOUND.

## Cross-module

| Dir | Contract |
|---|---|
| M10 → M09 | `CompetitionStructureContract` + new `participantView`/`roundWindow` |
| M10 → M07 | `ProjectContributionContract.resolve` (new — member+role validation, snapshot display) |
| M10 → M04 | `MediaReferenceContract.isUsableBy` |
| M11–M16 → M10 | `SubmissionTruthContract` — `isFinalizedSubmission(sub,round)`, `belongsToCompetition` |

## Testing

`SubmissionFlowIT` 6 real-PG17: individual lifecycle+dup+history, IDOR read/submit/withdraw, team submission with contextual roles+non-member reject+DB-02 snapshots, deadline, M04 media+freeze, `SubmissionTruthContract` for voting targets. Suite: **145**.

## Known follow-ups

- `snapshot_member_display` = member userId string until a profile-display read contract exists (documented gap, same seam).
- Voting/judging visibility on submissions (M11+ consumes truth contract).
- Withdraw after FINALIZED — product decision (currently invalid).
