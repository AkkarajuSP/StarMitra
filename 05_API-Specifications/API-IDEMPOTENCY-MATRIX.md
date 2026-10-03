# API Idempotency Matrix

Rule: **database-enforced uniqueness first**; `Idempotency-Key` header only for retry-sensitive commands; safe retries return the existing record (replay semantics), never double-effect.

| Endpoint | Required? | Mechanism | DB constraint | Retry behavior |
|----------|-----------|-----------|---------------|----------------|
| POST /auth/otp/verify | no | OTP consumed-once | consumed_at | retry → OTP_INVALID |
| POST /conversations | IK | key dedup | — | replay → existing conversation |
| POST /{id}/messages | **yes** | `clientMessageId` (body) + IK | `PK(client_message_id)` | replay → same messageId |
| POST /{id}/read | natural | forward-only receipts | PK pair | re-read no-op |
| PUT/DELETE /users/me/blocks/{id} | natural | unique pair | PK(blocker,blocked) | no-op |
| PUT /social/follows/{userId} | natural | unique pair | PK(follower,followee) | no-op (or ALREADY_FOLLOWING→204) |
| POST /social/likes | **yes** | IK + UQ | `UQ(user,target)` | replay → existing |
| POST /social/comments | IK | key dedup | — | replay |
| DELETE likes/comments | natural | existence | — | 404-safe |
| POST /rooms, invitations | IK | key + partial UQ | `UQ(room,invitee) WHERE pending` | replay |
| POST /rooms/{id}/contributions | IK | UQ triple | `UQ(room,member,role)` | replay |
| POST /portfolios/me/items | IK | key | — | replay |
| POST /competitions + config | IK | key | — | replay |
| POST /participants/register | **yes** | IK + UQ | `UQ(comp,cat,user,project) NULLS NOT DISTINCT` | replay → existing |
| POST /submissions | IK | key + config UQ | `UQ(participant,round,attempt)` | replay |
| POST /{id}/submit, /finalize | **yes** | IK + state transition | transition idempotent | replay → current state |
| POST /{id}/withdraw | natural | transition | — | no-op if withdrawn |
| POST /votes | **yes** | `clientMsgId` + IK | `UQ(voter,submission,round)` + `UQ(client_msg_id)` | replay → same vote |
| POST /judges/{id}/assignments | IK | scoped UQ | `UQ(judge,comp,cat,round) NULLS NOT DISTINCT` | replay |
| POST /evaluations | **yes** | IK + UQ | `UQ(judge,submission,round)` — DB-09 | replay → same eval |
| POST /{id}/publish (rubric) | natural | transition | published immutable | no-op |
| POST /scoring/calculate,/finalize | **yes** | IK + version-bound UQ | `UQ(sub,round,config,ver)` | replay/skip |
| POST /scoring/overrides, /progression/overrides | IK | key | append-only rows | replay → no dup |
| POST /progression/calculate,/finalize | **yes** | IK + `UQ(sub,round,config)` | | replay |
| POST /leaderboards/publications | IK | transition | — | replay |
| POST /notifications/{id}/read, /read-all | natural | read-state | PK pair | no-op |
| POST /moderation/reports | IK | key | — | replay |
| POST /cases/{id}/decisions, /restrictions | **yes** | IK | audited append-only | replay → no dup |
| GET endpoints | never | — | — | — |
| If-Match writes | concurrency (not idem) | version | `version` column | 409 on stale |

**Non-goal:** no Idempotency-Key on GETs; no distributed idempotency store — dedup is Postgres uniqueness (ADR-010).
