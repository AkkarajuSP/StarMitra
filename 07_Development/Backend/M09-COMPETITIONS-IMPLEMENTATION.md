# M09 — Competitions — Implementation

**Slice:** tenth business implementation. **Status: complete.** M09 owns competition truth — never submissions/votes/judges/rubrics/scoring/progression/leaderboards.

## Ownership

`competitions` (@Version real column), `competition_categories`, `competition_category_skills`, `competition_rounds`, `eligibility_rules`, `submission_configs`, `competition_participants` (DB-03 XOR) — full V1.7 surface.

## Contract surface (10 ops, unchanged)

list/create/get/update competitions (+IfMatch=version) · createCategory · createRound · addEligibilityRule · putSubmissionConfig (+IfMatch) · listParticipants · registerParticipant.

## Models

- **Competition**: `config_status` DRAFT→CONFIGURED (putSubmissionConfig), `participation_status` OPEN/CLOSED, `round_state` NOT_STARTED/ACTIVE/COMPLETE — all three lifecycle columns pre-designed for M14/M15.
- **Category**: UQ(comp,name); **multi-skill** via `competition_category_skills` — M03 `isActiveSkill` validation; capability tags, never permissions. No hardcoded categories.
- **Round**: UQ(comp,sequence); ref-ids for M11 `vote_config_id`, M13 `rubric_version_id`, M15 `progression_config_id` — stored, not validated (downstream modules don't exist yet).
- **Eligibility**: rule_type + JSONB params — canonical storage; per-type evaluator pluggable (open).
- **Submission config**: freeform JSONB, versioned; marks competition CONFIGURED.
- **Participant**: USER xor PROJECT (ck_cp_xor + UQ nulls-not-distinct); USER→caller self; PROJECT→caller must be ACTIVE M07 member (contract).

## Cross-module

| Dir | Contract |
|---|---|
| M10–M16 → M09 | `CompetitionStructureContract` — `roundBelongsTo`, `categoryBelongsTo`, `isActiveParticipant`, `isOpenForParticipation` |
| M09 → M03 | `SkillTaxonomyContract` (category skills) |
| M09 → M07 | `ProjectMembershipContract` (PROJECT registration) |

## Security

Owner (`created_by`)-only mutations → NOT_FOUND (no leak); registration self-scoped; dup → CONFLICT; closed participation → STATE_TRANSITION_INVALID.

## Testing

`CompetitionFlowIT` 6 real-PG17: lifecycle+@Version+non-owner, multi-skill category + M03 validation, rounds (UQ seq) + eligibility + config→CONFIGURED, USER/PROJECT registration + M07 membership + dup UQ, `CompetitionStructureContract` answers, category-scoped registration. Suite: **139**.

## Known follow-ups

- Eligibility-rule evaluation engine per `rule_type` (storage canonical; pluggable).
- Competition creator role gate (owner=caller today; ADMIN policy = product decision).
- Round state transitions driven by M15.
- Category-scoped submission_configs (table supports; API is competition-wide).
