# API Security Matrix

Legend — Auth: `ANON`/`AUTH`/`ROLE:{r}` · Resource: `OWN`=owner-only · `MBR`=member · `VIS`=visibility-filtered · `SCOPE`=judge-assignment · `PUB`=public · Sensitive: data requiring masking/audit. **Backend authz authoritative; `TalentSkill`/`ProjectContributionRole`/`JudgeExpertise` never appear as permission inputs.**

## Auth / Identity

| Endpoint | Auth | Resource | Sensitive |
|----------|------|----------|-----------|
| /auth/otp/request, /auth/otp/verify | ANON | — | enumeration-safe; hashed OTP never returned |
| /auth/refresh | refresh-token | own family | reuse→revoke+alert |
| /auth/logout, /auth/sessions* | AUTH | OWN | session metadata |
| /auth/me | AUTH | OWN | roles exposed to self only |

## User surfaces

| Endpoint | Auth | Resource | Sensitive |
|----------|------|----------|-----------|
| /profiles/me PUT | AUTH | OWN + If-Match | |
| /profiles/{userId} | AUTH/VIS | PUB | block+moderation filtered |
| /users/me/skills* | AUTH | OWN | |
| /portfolios/me* | AUTH | OWN + If-Match | |
| /portfolios/{userId} | VIS | PUB | |
| /social/follows,likes,comments | AUTH | OWN actions | target VIS+block+restriction |
| /social/engagement | AUTH | PUB counts | derived only |
| /users/me/blocks/{userId} | AUTH | OWN | |

## Messaging

| /conversations* | AUTH | MBR per send/read | member list masked for non-members |
| /{id}/messages GET/POST | AUTH | MBR + block check | attachment access = membership+media VIS |
| /{id}/read | AUTH | MBR | |

## Media

| /media POST | AUTH | OWN | metadata |
| /{id}/upload-url, /complete | AUTH | OWN asset | short-lived URL |
| /{id}/delivery-url | AUTH | VIS + domain rule | signed, expiring; no storage internals |

## Rooms

| /rooms GET | AUTH | VIS (discovery) | private rooms non-member-invisible |
| /rooms POST | AUTH | creator=owner | |
| /{roomId} PUT | AUTH | owner/member-role + If-Match | |
| members/skills/invitations/tasks/assets/outputs/credits | AUTH | MBR (invite: inviter-rights; respond: invitee) | contribution roles = context, not permissions |

## Competitions / Submissions

| /competitions GET, /{id} GET | AUTH/VIS | PUB published only | unpublished invisible |
| /competitions POST + config writes | ROLE:ADMIN | — | audited config changes |
| /participants/register | AUTH | eligibility check (rule ≠ permission) | |
| /{id}/submissions POST | AUTH | participant-owned entry | |
| /submissions/{id} GET | AUTH | participant/judge-scope/admin/VIS | contributor privacy |
| /{id}/submit,/finalize,/withdraw | AUTH | participant-owned + IK | finalize captures snapshot+audit |
| /{id}/media,/contributors | AUTH | participant-owned, pre-finalize only | evidence frozen after |

## Voting

| /votes POST | AUTH | eligible voter + window + scope, IK+clientMsgId | voter privacy on team votes |
| /votes/counts | VIS | derived | never authoritative |
| /vote-configs POST | ROLE:ADMIN | — | |

## Judges / Rubrics

| /judges POST, /expertise, /assignments, /revoke | ROLE:ADMIN | — | assignment=scope grant; audited |
| /judges/me* | ROLE:JUDGE + SCOPE | assignment-resolved server-side | no cross-judge leakage; blind-judging-ready |
| /evaluation-templates* | ROLE:ADMIN | draft editable; publish=immutable | weights=100 gate |
| /evaluations POST | ROLE:JUDGE + SCOPE + IK | within assignment+window; UQ triple | confidential pre-release |
| /evaluations/{id} GET,/reopen | SCOPE / ROLE:ADMIN | own eval / authorized amend | audited |

## Scoring / Progression / Leaderboards

| /scoring-configs*, /tie-break*, /qualification* | ROLE:ADMIN | versioned | published immutable |
| /scoring/calculate,/finalize,/overrides | ROLE:ADMIN + IK | audited; no arbitrary score mutation | result integrity |
| /scoring/results | ROLE:ADMIN | CUR | |
| /progression/* | ROLE:ADMIN + IK | consumes M14; overrides separate | |
| /leaderboards GET | VIS/PUB | publication-gated | judge-confidentiality respected |
| /leaderboards/publications,/snapshot | ROLE:ADMIN + IK | | |

## Notifications

| /notifications*, /preferences | AUTH | OWN | delivery state ≠ business truth |

## Moderation

| /moderation/reports POST | AUTH | own report; IK | reporter privacy masked |
| /moderation/cases*,/decisions,/restrictions | ROLE:MODERATOR/ADMIN | queue + scope | evidence privacy; audited |

## Admin

| /admin/dashboard, /admin/audit | ROLE:ADMIN/SUPER_ADMIN | views only | sensitive-data masking; audit access itself audited |

## Rate-limit categories (values = open config)

`auth` (OTP/login) · `send` (messages/comments/votes) · `read` (lists/feed) · `admin` (management) · `media` (uploads) · `report` (moderation reports)
