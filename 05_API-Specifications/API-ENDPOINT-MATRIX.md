# API Endpoint Matrix — 133 operations

`A`=auth'd · `R`=system-role · `O`=ownership · `S`=judge-assignment-scope · `PUB`=public-read (visibility-filtered) · `CUR`=cursor · `OFF`=offset · `IK`=Idempotency-Key · `IM`=If-Match

## M01 Auth (7)
| Op | Auth | Notes |
|----|------|-------|
| POST /auth/otp/request | anon | enumeration-safe 202 |
| POST /auth/otp/verify | anon | issues JWT+refresh |
| POST /auth/refresh | refresh-token | rotation+reuse-detect |
| POST /auth/logout | A | revoke |
| GET/DELETE /auth/sessions{,/{id}} | A+O | own sessions only |
| GET /auth/me | A | identity+roles |

## M02 Profile (3)
GET/PUT /profiles/me (A+O, IM) · GET /profiles/{userId} (PUB+block/visibility filter)

## M03 Skills (7)
GET /skills (PUB, CUR) · POST/PUT/deactivate /skills{,/{id}} (A+R admin) · GET /users/me/skills · PUT/DELETE /users/me/skills/{skillId} (A+O, IK)

## M04 Media (5)
POST /media (A, IK) · POST /{id}/upload-url · POST /{id}/complete (IK) · GET /{id} · GET /{id}/delivery-url — all A+visibility/domain authz

## M05 Discovery (3)
GET /search · /discovery · /feed — A/PUB+visibility scoping, CUR

## M06 Connect (8)
GET/POST /conversations (A, CUR, IK) · GET /{id} · GET/POST /{id}/messages (member, CUR, IK, clientMessageId dedup) · POST /{id}/read · PUT/DELETE /users/me/blocks/{userId} (A+O)

## M07 Rooms (13)
GET/POST /rooms (CUR, IK) · GET/PUT /{roomId} (member/owner, IM) · GET /{roomId}/members · PUT/DELETE /{roomId}/required-skills/{skillId} · POST /{roomId}/invitations · GET /invitations/me · POST /invitations/{id}/respond (invitee) · POST /{roomId}/contributions · GET/POST /{roomId}/tasks · POST /{roomId}/assets · POST /{roomId}/final-outputs · GET /{roomId}/credits

## M08 Portfolio (9)
GET/PUT /portfolios/me (O, IM) · GET/POST /portfolios/me/items (IK) · PUT/DELETE /items/{id} (IM) · POST /items/{id}/media · POST /items/{id}/contributions (verified-credit only) · GET /portfolios/{userId} (PUB)

## M09 Competitions (11)
GET/POST /competitions (PUB-list/CUR + admin-create IK) · GET/PUT /{competitionId} (IM) · POST /{id}/categories · POST /{id}/rounds · POST /{id}/eligibility-rules · PUT /{id}/submission-config (IM) · GET /{id}/participants (CUR) · POST /{id}/participants/register (A, IK, XOR participant)

## M10 Submissions (9)
POST /competitions/{id}/submissions (A+participant, IK) · GET /submissions/{id} (visibility) · POST /{id}/media · PUT /{id}/contributors · POST /{id}/submit · POST /{id}/finalize (IK — snapshot capture) · POST /{id}/withdraw · GET /{id}/history · GET /competitions/{id}/submissions/list (CUR, visibility)

## M11 Voting (3)
POST /votes (A+eligibility, IK+clientMsgId) · GET /votes/counts (PUB-gated, derived) · POST /vote-configs (R)

## M12 Judges (5)
POST /judges · GET /judges (R, OFF) · PUT /judges/{id}/expertise (R/O) · POST /judges/{id}/assignments (R, IK) · POST /assignments/{id}/revoke (R)

## M20 Judge Portal (4)
GET /judges/me · /judges/me/assignments · /judges/me/submissions (A+JUDGE+S, CUR) · /judges/me/rubrics/{contextId} · /judges/me/results (release-gated)

## M13 Rubrics (7)
POST /evaluation-templates (R, IK) · GET list (R, OFF) · PUT /{id} (draft, IM) · POST /evaluation-template-versions/{id}/publish (R, IK, weights=100) · POST /evaluations (JUDGE+S, IK, DB-09 UQ) · GET /evaluations/{id} (S) · POST /evaluations/{id}/reopen (R, audited)

## M14 Scoring (7)
POST /scoring-configs · /tie-break-configs · /qualification-configs (R) · POST /scoring/calculate · /finalize (R, IK) · GET /scoring/results (R, CUR) · POST /scoring/overrides (R, IK, audited)

## M15 Progression (5)
POST /progression-configs (R) · POST /progression/calculate · /finalize (R, IK) · GET /progression/records (R, CUR) · POST /progression/overrides (R, audited)

## M16 Leaderboards (3)
GET /leaderboards (publication-gated, CUR) · POST /leaderboards/publications (R, IK) · POST /leaderboards/{id}/snapshot (R)

## M17 Notifications (5)
GET /notifications (O, CUR) · POST /{id}/read · POST /read-all · GET/PUT /notification-preferences (O)

## M18 Moderation (5)
POST /moderation/reports (A, IK) · GET /moderation/cases · /cases/{id} (moderator R, OFF) · POST /cases/{id}/decisions (R, IK, audited) · POST /moderation/restrictions (R, IK) — **no appeals API**

## M19 Admin (2)
GET /admin/dashboard · /admin/audit (R ADMIN/SUPER_ADMIN, CUR) — views only; mutations via domain endpoints

## M21 Social (7)
PUT/DELETE /social/follows/{userId} (A, IK) · GET /social/follows/me (CUR) · POST/DELETE /social/likes{,/{id}} (A, IK) · POST/GET /social/comments · DELETE /social/comments/{id} (author/moderation) · GET /social/engagement
