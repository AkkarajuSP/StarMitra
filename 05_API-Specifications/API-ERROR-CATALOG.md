# API Error Catalog — RFC 9457 Problem Details

Envelope: `{type, title, status, detail, code, correlationId, errors[]}` — `type` = `https://starmitra.app/problems/{code}`. Enumeration-safe on identity endpoints. Never expose internals.

## Authentication / Session

| code | status | When |
|------|--------|------|
| AUTH_REQUIRED | 401 | missing/invalid token |
| OTP_INVALID | 401 | wrong/expired OTP (uniform — no existence hint) |
| OTP_LOCKED | 429 | max attempts exceeded |
| SESSION_REVOKED | 401 | revoked session |
| REFRESH_REUSE_DETECTED | 401 | token-family reuse → revoke all |
| CSRF_REQUIRED | 403 | web cookie flow missing CSRF |

## Generic

| code | status | When |
|------|--------|------|
| FORBIDDEN | 403 | authenticated but denied |
| ROLE_REQUIRED | 403 | missing SystemRole |
| NOT_FOUND | 404 | also used to prevent enumeration where appropriate |
| VALIDATION_FAILED | 422 | field errors in errors[] |
| CONFLICT | 409 | domain conflict |
| CONFLICT_VERSION | 409 | If-Match/version mismatch |
| STATE_TRANSITION_INVALID | 409 | illegal transition for current state |
| IDEMPOTENCY_CONFLICT | 409 | same key, different payload |
| RATE_LIMITED | 429 | limit category exceeded |
| UNSUPPORTED_MEDIA_TYPE | 415 | wrong content type |
| PAYLOAD_TOO_LARGE | 413 | body limit |

## Media

| code | status | When |
|------|--------|------|
| MEDIA_NOT_FOUND | 404 | asset missing |
| MEDIA_UPLOAD_EXPIRED | 409 | pre-signed URL window passed |
| MEDIA_CHECKSUM_MISMATCH | 422 | upload verification failed |
| MEDIA_PROCESSING_FAILED | 409 | async pipeline failure |
| MEDIA_NOT_READY | 409 | delivery requested before ready |
| MEDIA_FORBIDDEN | 403 | visibility/domain rule denies |

## Competitions / Submissions

| code | status | When |
|------|--------|------|
| COMPETITION_NOT_FOUND | 404 | |
| ELIGIBILITY_DENIED | 403 | business-rule ineligibility (not permission) |
| PARTICIPANT_TYPE_INVALID | 422 | user/project XOR violated |
| SUBMISSION_DEADLINE_PASSED | 409 | server-authoritative deadline |
| SUBMISSION_ALREADY_FINALIZED | 409 | evidence frozen |
| SUBMISSION_STATE_INVALID | 409 | illegal lifecycle transition |
| SUBMISSION_MEDIA_LOCKED | 409 | post-finalize media change |

## Voting

| code | status | When |
|------|--------|------|
| VOTING_CLOSED | 409 | window not open |
| VOTE_DUPLICATE | 409 | dedup constraint |
| VOTE_LIMIT_EXCEEDED | 409 | configured limit |
| VOTE_TARGET_INVALID | 422 | wrong targetType/scope |
| VOTER_INELIGIBLE | 403 | |

## Judges / Evaluations

| code | status | When |
|------|--------|------|
| JUDGE_NOT_FOUND | 404 | |
| JUDGE_FORBIDDEN | 403 | SystemRole=Judge absent |
| ASSIGNMENT_SCOPE_INVALID | 422 | scope doesn't cover resource |
| ASSIGNMENT_REVOKED | 410 | scope withdrawn |
| CROSS_SCOPE_DENIED | 403 | outside assignment |
| RUBRIC_NOT_PUBLISHED | 409 | unpublished version |
| RUBRIC_WEIGHT_INVALID | 422 | weights ≠ 100% at publish |
| RUBRIC_VERSION_IMMUTABLE | 409 | edit on published |
| EVALUATION_DUPLICATE | 409 | UQ(judge,submission,round) — DB-09 |
| EVALUATION_FINALIZED | 409 | needs authorized reopen |
| EVALUATION_OUT_OF_SCOPE | 403 | |

## Scoring / Progression / Leaderboard

| code | status | When |
|------|--------|------|
| CONFIG_NOT_PUBLISHED | 409 | scoring on draft config |
| CONFIG_IMMUTABLE | 409 | published config edit |
| SCORING_INCOMPLETE | 409 | missing inputs |
| OVERRIDE_REASON_REQUIRED | 422 | audited override without reason |
| PROGRESSION_INPUT_MISSING | 409 | M14 result missing |
| RESULT_NOT_FINALIZED | 409 | read before finalization |
| LEADERBOARD_NOT_PUBLISHED | 404 | hidden state |

## Messaging / Social / Moderation

| code | status | When |
|------|--------|------|
| NOT_A_MEMBER | 403 | conversation access |
| BLOCKED | 403 | UserBlock active |
| MESSAGE_DUPLICATE | 409 | clientMessageId dedup (returns original) |
| ALREADY_FOLLOWING | 409 | follow exists (idempotent-OK) |
| ALREADY_LIKED | 409 | duplicate like |
| TARGET_NOT_LIKEABLE | 422 | targetType not MEDIA/PORTFOLIO |
| TARGET_RESTRICTED | 403 | moderation-restricted target |
| MODERATION_ROLE_REQUIRED | 403 | non-reviewer on queue/decision |

## System

| code | status | When |
|------|--------|------|
| INTERNAL_ERROR | 500 | sanitized — correlationId only |
| SERVICE_UNAVAILABLE | 503 | |
