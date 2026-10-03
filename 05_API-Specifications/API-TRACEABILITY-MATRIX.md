# API Traceability Matrix — FRS → Module → Endpoint → Authz → Entity → Acceptance

Coverage of the primary FRS v1.1 flows (§7.4–7.6 core user flows). Not every endpoint maps to a distinct FRS line — convenience/support endpoints are marked `SUPPORT` (not FRS-required).

## Core lifecycle flows

| FRS flow | Module | Endpoint(s) | Authz | Entity | Acceptance |
|----------|--------|-------------|-------|--------|------------|
| Registration/login | M01 | /auth/otp/request,/verify,/refresh | ANON | users, otp_challenges, refresh_tokens | session established; reuse-detect |
| Profile creation | M02 | /profiles/me PUT | OWN | user_profiles | visibility honored |
| Skill selection | M03 | /users/me/skills PUT | OWN | user_talent_skills | talent ≠ permission |
| Media upload | M04 | /media + /upload-url + /complete | OWN | media_assets | direct upload, async process |
| Profile/skill discovery | M05 | /search,/discovery,/feed | VIS | discovery/feed_projections | visibility-filtered |
| Connect/DM | M06 | /conversations, /messages POST | MBR+block | conversations, messages | dedup'd send |
| Project rooms/collaboration | M07 | /rooms*, /invitations, /contributions, /tasks, /final-outputs, /credits | MBR/owner | creative_rooms + member/task/credit tables | contribution role ≠ permission |
| Portfolio publish | M08 | /portfolios/me*, /items*, /contributions | OWN | portfolios, portfolio_items* | verified credits only |
| Competition creation | M09 | /competitions + categories/rounds/rules/config | ROLE:ADMIN | competitions* tables | structure only |
| Competition entry | M09 | /participants/register | AUTH+eligibility | competition_participants | XOR user/project |
| Submission upload | M10 | /submissions + /media + /contributors + /submit + /finalize | participant | submissions* | snapshot at finalize; evidence frozen |
| Voting | M11 | /votes POST, /votes/counts | voter+window | votes | dedup; derived count |
| Judge assignment | M12 | /judges/{id}/assignments | ROLE:ADMIN | judge_assignments | scope = authz boundary |
| Rubric publish | M13 | /evaluation-templates* + /publish | ROLE:ADMIN | evaluation_template_versions | weights=100; immutable |
| Judge evaluation | M13 | /evaluations POST | JUDGE+SCOPE+IK | judge_evaluations | one per judge/sub/round |
| Scoring | M14 | /scoring/calculate,/finalize | ROLE:ADMIN | final_scores, aggregations | version-bound, sealed |
| Progression | M15 | /progression/calculate,/finalize | ROLE:ADMIN | progression_records | consumes M14 |
| Leaderboard | M16 | /leaderboards + /publications | PUB-gated | leaderboard_projections* | derived only |
| Notifications | M17 | /notifications*, /preferences | OWN | notifications* | status ≠ truth |
| Moderation report | M18 | /moderation/reports | AUTH | moderation_reports | case routing |
| Moderation decision | M18 | /cases/{id}/decisions, /restrictions | MODERATOR | moderation_decisions/actions | ≠ UserBlock |
| Follow/like/comment | M21 | /social/* | AUTH | follows, likes, comments | Like≠Vote; MEDIA\|PORTFOLIO |
| Admin oversight | M19 | /admin/dashboard,/admin/audit | ADMIN | audit_log (read) | view-only |
| Judge workspace | M20 | /judges/me/* | JUDGE+SCOPE | reads across M12/13/10 | scope-resolved |

## Supporting endpoints (not FRS-required — marked SUPPORT)

Sessions list/revoke · media/delivery-url · submissions/history · vote-configs · scoring-configs CRUD · progression/records read · leaderboard snapshot · notifications/read-all · blocks list semantics.

## Explicit non-mappings (by design)

- **No** REST endpoint for WS realtime (documented separately) · **no** moderation-appeal endpoint (deferred) · **no** password-login endpoint (open) · **no** endpoint exposing `audit_log` writes · **no** `/admin/update-*` generic mutation.
