# M11 — Audience Voting — Implementation

**Slice:** twelfth business implementation. **Status: complete.** M11 owns vote truth — never scoring/ranking (M14 seam provided).

## Ownership

`votes`, `vote_configs` (versioned series) — full V1.9 surface.

## Contract surface (3 ops, unchanged)

`POST /votes` (clientMsgId idempotent) · `GET /votes/counts?submissionId` (derived=true) · `POST /vote-configs` (ROLE_ADMIN).

## Models

- **Vote**: voter + submission + comp/category/round + target_type + cast_at + client_msg_id. Comp/category/round/target_type derived from M10 `submissionView` — never client-supplied.
- **PROJECT vote = ONE target** — never split among contributors.
- **Vote config**: versioned `(series_key, version_no)` + JSONB payload — `maxVotesPerVoter`, `windowStart`, `windowEnd` data-driven; round references config_id (M09 holds ref only).

## Enforcement

- **Validity**: submission must be FINALIZED (M10 truth); non-existent/not-votable → reject.
- **Window**: round start/end always; config window overrides when set.
- **Limit**: `maxVotesPerVoter` per round from config.
- **Dedup**: `uq_votes_dedup` (voter+sub+round) + pre-check replay returns existing; `uq_votes_client` (clientMsgId) → same vote returned. Race-safe: UQ violation → lookup winner.
- **Audit**: `VOTE_CAST` (voter, target, participant type); vote row carries voter+target+comp+round+cast_at.

## Cross-module

| Dir | Contract |
|---|---|
| M11 → M10 | `SubmissionTruthContract.submissionView` (new) |
| M11 → M09 | `roundWindow` (+voteConfigId — extended) |
| M14 → M11 | `VoteTruthContract` — `countForSubmission`, `countsBySubmission(roundId)` |

## Bug surfaced & fixed

`jsonb` columns need `@JdbcTypeCode(SqlTypes.JSON)` — applied to `vote_configs.payload` **and** the latent same bug in M09's `submission_configs.config`/`eligibility_rules.rule_params` (unflushed in M09 tests).

## Testing

`VoteFlowIT` 5 real-PG17: individual+project (one target), dedup replay + clientMsgId + per-voter counts, invalid/non-finalized targets, config window + maxVotesPerVoter limit + round-deadline interplay, comp/round isolation + `countsBySubmission`. Suite: **150**.

## Known follow-ups

- Eligible-voter scoping (payload key exists; enforcement rules are product-level).
- M14 weighting consumes `countsBySubmission` — implemented elsewhere.
