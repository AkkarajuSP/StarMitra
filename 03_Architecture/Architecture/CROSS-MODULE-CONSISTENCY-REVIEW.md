# StarMitra Cross-Module Consistency Review

**Scope:** M01–M20 module designs vs FRS v1.1 + Architecture Baseline + ADR-001…ADR-013
**Branch:** `feature/cross-module-consistency-review` | **Status:** Review complete — findings below

## 1. Executive Summary

The 20-module design is **architecturally coherent**. No CRITICAL defects and **no module requires redesign** — every module correctly preserves the accepted ownership boundaries, the multi-talent model, the three-weight separation, the qualification-vs-progression split, and the presentation-layer boundary of M19/M20.

The review identified **9 findings**: **2 HIGH** (ownership gaps that must be resolved before schema design), **4 MEDIUM** (naming/config-boundary alignments), **3 LOW/INFO** (documentation alignment). None require re-opening an accepted ADR; all are resolvable as corrections or PO/technical decisions without redesigning accepted modules.

## 2. Source Documents

FRS v1.1 (docx) · `STARMITRA-ARCHITECTURE-BASELINE-v1.0.md` · ADR-001…013 · `MODULE-*.md` M01–M20 · `DOMAIN-ARCHITECTURE.md`, `DATA-ARCHITECTURE.md`, `API-ARCHITECTURE.md`, `SECURITY-ARCHITECTURE.md`, `MEDIA-ARCHITECTURE.md`, `REALTIME-ARCHITECTURE.md`, `SEARCH-ARCHITECTURE.md`, `NOTIFICATION-ARCHITECTURE.md`, `OBSERVABILITY-ARCHITECTURE.md`, `ANALYTICS-ARCHITECTURE.md`.

## 3. Module Ownership Matrix

| Entity/Concept | Owner | Referencing modules | Conflict? |
|----------------|-------|---------------------|-----------|
| User, SystemRole, UserSystemRole, session/token, account status | M01 | all | ✅ none |
| UserProfile, profile visibility/presentation | M02 | M05, M08, M18, M19 | ✅ none |
| TalentSkill, UserTalentSkill, SkillProficiency | M03 | M05, M07, M09, M12 (refs) | ✅ none |
| MediaAsset, MediaVariant, upload/processing/storage | M04 | M02, M06–M08, M10, M18, M20 | ✅ none |
| Discovery/feed projections (derived) | M05 | — | ✅ none |
| Conversation, ConversationMember, Message, MessageReceipt, MessageAttachment | M06 | M07 (project link), M17, M18 | ⚠️ CM-03 (Block omitted) |
| CreativeRoom, ProjectMember, ProjectContributionRole, RequiredSkill, ProjectInvitation, ProjectTask, ProjectAsset, FinalOutput, ProjectCredit | M07 | M06, M08, M09, M10, M17 | ✅ none |
| Portfolio, PortfolioItem, PortfolioItemMedia, PortfolioItemContribution | M08 | M02, M05 | ✅ none |
| Competition, Category, Round, EligibilityRule, SubmissionConfig, CompetitionParticipant | M09 | M10–M16, M05, M18 | ⚠️ CM-05/06 (config boundaries) |
| Submission, SubmissionMedia, SubmissionContributor, SubmissionHistory | M10 | M11–M16, M18, M20 | ✅ none |
| Vote, VoteConfig (voting-specific), derived counts | M11 | M14 | ⚠️ CM-05 |
| Judge, JudgeExpertise, JudgeAssignment | M12 | M13, M19, M20 | ✅ none |
| EvaluationTemplate(+Version), EvaluationCriterion, JudgeEvaluation, CriterionScore | M13 | M14, M20 | ✅ none |
| Scoring/TieBreak/Qualification configs, aggregates, FinalScore, Ranking, QualificationResult, ScoreOverride | M14 | M15, M16, M19, M20 | ✅ none |
| ProgressionConfiguration*, ProgressionRecord, ProgressionOverride | M15 | M16 | ⚠️ CM-06 (config ownership open) |
| LeaderboardProjection, LeaderboardPublication, LeaderboardSnapshot | M16 | M19 | ✅ none |
| Notification, Preference, Template, DeliveryAttempt, ReadState, EventReference | M17 | all (consumers emit) | ✅ none |
| ModerationReport/Case/Decision/Action/EvidenceRef/Restriction/PolicyRef | M18 | all (enforcers) | ✅ none |
| Follow, Like, Comment, EngagementCounter | **⚠️ UNASSIGNED** | M05, M17, M04(Followers), M02(counts) | ❌ **CM-01** |
| Platform AuditLog, PlatformConfig, analytics projections | **⚠️ AMBIGUOUS** | all | ❌ **CM-02** |
| Block | baseline → D6/Connect; omitted in M06 entity table | M02, M05, M06, M18 | ⚠️ CM-03 |

## 4. Cross-Module Dependency Matrix (consumption direction)

```text
M01 ─┐ (identity/authz — depended on by ALL, depends on none)
M02 → M01                       M11 → M01 M09 M10
M03 → M01                       M12 → M01 M09 M10 M17*
M04 → M01                       M13 → M09 M10 M12
M05 → M01 M02 M03 M04 (+social,M18)   M14 → M09 M10 M11 M12 M13
M06 → M01 M04 M07 M17*          M15 → M09 M10 M14
M07 → M01 M03 M04 M05 M17*      M16 → M09–M15
M08 → M01 M02 M03 M04 M05 M07   M17 → consumes all (signal sink)
M09 → M01 M03 M05 M07 M17*      M18 → consumes all (decision authority)
M10 → M01 M04 M07 M09           M19 → orchestrates M01–M18
                                M20 → M01 M04 M10 M12 M13 M14–16 M17
    (* = emits signals to M17 — direction is business→M17)
```

**No circular dependencies** — every edge flows toward platform/consuming modules (M17/M18 sink; M19/M20 orchestrate). No ownership-violating dependency detected.

## 5. Identity & Authorization Consistency

✅ Verified across all modules: `SystemRole`=capability; `TalentSkill`/`UserTalentSkill`/`SkillProficiency`=metadata; `JudgeExpertise`=qualification; `JudgeAssignment`=scope; `ProjectContributionRole`=contextual contribution; route groups ≠ authorization. No module violates the rule — **zero authz-conflation findings**.

## 6. Multi-Talent Model Consistency

✅ `User → UserTalentSkill → TalentSkill` preserved end-to-end: multi-skill users (M02/M03), no primary-skill dependency (M08), skills as eligibility refs not permission (M09), contextual contribution roles (M07), contribution-based credits (M08), team=one-entry (M10/M11/M14/M15/M16/M20). **Consistent.**

## 7. Media Ownership Consistency

✅ M04 owns binary/lifecycle; business modules own attachment-link entities: `ProfileAvatar`-equivalent (M02), `PortfolioItemMedia` (M08), `SubmissionMedia` (M10), `ProjectAsset` (M07), `MessageAttachment` (M06), `ModerationEvidenceReference` (M18), judge evidence via M04+M12 scoping. Signed-URL-after-authz and finalization immutability preserved. **Consistent.**

## 8. Creative Room / Project Consistency

✅ M07 membership authoritative; M06 validates via read-contract without duplication; M10 team submission = ONE entry referencing M07 membership/contribution; contributors never scored independently (M14); votes attach to entry (M11); M08 credits consume M07 `ProjectCredit`. **Consistent.**

## 9. Competition Lifecycle Consistency

`Competition(M09) → Category/Round(M09) → Eligibility/Participation(M09) → Submission(M10) → Voting(M11)/Judging(M12/13) → Scoring/Ranking/Qualification(M14) → Progression(M15) → Leaderboard/Publication(M16)` — **exactly one owner per transition; no duplicated state machine.** Config-boundary details flagged in CM-05/06.

## 10. Submission Consistency

✅ Individual vs team distinguished; team=ONE entry; contributors referenced (+optional snapshot — open); server-authoritative deadline; finalized→immutable evidence; separate state machine. **Consistent.**

## 11. Voting Consistency

✅ Vote=authoritative; counts=derived/rebuildable; idempotent; configurable limits; window server-validated; reversal policy open; project votes never split; M14 contract clean. ⚠️ CM-05 (VoteConfig boundary).

## 12. Judge & Rubric Consistency

✅ `JudgeAssignment`=authz boundary; expertise≠authz; published `EvaluationTemplateVersion` immutable; every `JudgeEvaluation` binds `rubricVersionId`; independent judging enforced; revocation enforced; `SystemRole=Judge`+assignment required. **Consistent.**

## 13. Scoring / Ranking / Progression Consistency

✅ M14 aggregates+weights+ranks+qualifies; M15 advances/eliminates; M16 presents. No advance-in-M14, no recalc-in-M15, no compute-in-M16. Three weight layers distinct. `ScoreOverride` ≠ `ProgressionOverride`. Ties: M14 computes, M15 consumes boundary effects. **Consistent.**

## 14. Leaderboard Consistency

✅ Derived/rebuildable; every field traced upstream; publication ≠ finalization; snapshots reference versions; judge confidentiality enforced; cache never authoritative. **Consistent.**

## 15. Notification Consistency

✅ Business modules own truth; M17 delivers; delivery-failure never alters source state; dedup identity defined; stable references; no duplicated state machine; no broker/provider. **Consistent.**

## 16. Moderation Consistency

✅ M18 decides, owning module enforces; never directly changes competition scores/results (M14 independence preserved); evidence referenced not copied; reporter/target privacy; system-level authz only. **Consistent.**

## 17. Admin / Judge Portal Consistency

✅ M19/M20 = presentation/orchestration only; no domain ownership, no DB bypass, no generic-mutate APIs; authz via M01 SystemRole; judge-scope enforced server-side; judge≠admin. **Consistent.**

## 18. State Machine Inventory

| Lifecycle | Owner | States (status) | Conflicting definitions? |
|-----------|-------|-----------------|--------------------------|
| Account status | M01 | Active/Suspended/Blocked/Deactivated `[FRS]` | none |
| Upload/processing/moderation/visibility (4D) | M04 | per ADR-008 design | none |
| Conversation | M06 | active/archived *(proposal)* | none |
| MessageReceipt | M06 | sent/delivered/read `[FRS]` | none |
| CreativeRoom | M07 | Draft→Published→Active→Completing→Completed→Archived *(proposal)* | none |
| ProjectInvitation | M07 | Pending→Accepted/Declined/Withdrawn/Expired *(proposal)* | none |
| Portfolio | M08 | active/restricted *(proposal)* | none |
| Competition (config/participation/round — 3 dims) | M09 | *(proposal)* | none |
| Submission | M10 | Draft→Submitted→UnderReview→Accepted/Rejected→Finalized/Withdrawn *(proposal)* | none |
| Vote | M11 | reversal policy open | none |
| JudgeAssignment | M12 | Pending→Active→Revoked→Completed *(proposal)* | none |
| EvaluationTemplate | M13 | Draft→Validated→Published→Retired *(proposal)* | none |
| JudgeEvaluation | M13 | *(open)* | none |
| Progression | M15 | OPEN→…→FINALIZED *(proposal)* | none |
| LeaderboardPublication | M16 | Hidden→Published→Archived *(proposal)* | none |
| Notification | M17 | Created→Pending→Delivered/Failed→Read/Expired *(proposal)* | none |
| ModerationCase | M18 | Reported→Open→UnderReview→Actioned/Dismissed/Escalated→Closed *(proposal)* | none |

**No two modules define competing states for the same lifecycle.** All non-FRS taxonomies are correctly marked proposal/open.

## 19. Versioning & Immutability Review

✅ `EvaluationTemplateVersion`, `Scoring/TieBreak/Qualification/Progression` configs, finalized `Submission` evidence, `SubmissionMedia` (frozen), `LeaderboardSnapshot`, `Vote` records, published configs — all immutable/versioned with eval bound to `rubricVersionId` and results bound to config versions. **Reproducibility risk points:** `SubmissionContributor` snapshot strategy (open) — if membership changes post-submission, evidence integrity depends on the undecided snapshot policy; `VoteConfig`/`ProgressionConfiguration` boundary (CM-05/06) affects which version is authoritative on recompute.

## 20. Audit & Observability Review

✅ `AuditLog` (append-only, product-level) ≠ business records ≠ telemetry ≠ analytics — consistently separated across all modules; overrides attributable (actor/reason/before-after). ⚠️ **CM-02** — the platform `AuditLog` entity itself has ambiguous ownership (see below).

## 21. Findings & Severity

| ID | Severity | Modules | Issue | Recommended Resolution | PO Decision? |
|----|----------|---------|-------|------------------------|--------------|
| **CM-01** | **HIGH** | baseline-D4, M02, M05, M17, M04 | **`Follow`/`Like`/`Comment`/`EngagementCounter` have no module owner.** Baseline assigns them to domain "D4 Social Engagement" — which has no module in M01–M20. M05 says "Follow ownership lives in User Profile," but M02's boundary table never claims it. Consumed by feed signals, follower visibility (`Followers` media ACL), notification audiences, profile counts, engagement weighting. | Assign an owner: recommend **M02 User Profile** (relationship domain — matches M05's statement; update M02 ownership table to add `Follow`, `Like`, `Comment`, `EngagementCounter`) or create a dedicated Social Engagement module. Then update M02/M05 docs + index. | Yes — owner choice |
| **CM-02** | **HIGH** | baseline-D15, M01, M18, M19, all | **Platform `AuditLog`/`PlatformConfig`/analytics projections ownership is ambiguous.** Baseline's admin domain claims `AuditLog`+`PlatformConfig`+analytics projections; M19 was designed presentation-only and explicitly disowns all entities. M01 writes `AuthenticationAuditEvent` "→ AuditLog" — but no module owns the `AuditLog` table. | Assign a platform owner. Options: (a) **M18 Moderation/Audit** — audit is already moderation-adjacent; (b) a **platform/shared-kernel concern within M01** (identity+audit); (c) keep in baseline admin domain as a non-module platform entity — document explicitly. Recommend (b) or explicit platform-owner documentation. | Yes — owner choice |
| **CM-03** | **MEDIUM** | baseline-D6, M06, M02, M05, M18 | **`Block` entity omitted from M06's ownership table.** Baseline assigns `Block` to Connect; M06's §16 table lists 6 entities without `Block`. Block semantics affect M02 visibility, M05 discovery exclusion, M06 send-rejection. | Add `Block` (or `UserBlock`) to M06's owned entities (blockerId+blockedId), clarify relationship to M18 restriction (user-block vs moderation-restriction — distinct concerns) and M02/M05 consumption. | Partly — confirm M06 owner |
| **CM-04** | **MEDIUM** | baseline-D6, M06 | **Naming drift:** baseline uses `ConversationParticipant`; M06 uses `ConversationMember`. | Pick one (`ConversationMember` — M06's accepted doc) and align baseline/reference docs before schema design. | No |
| **CM-05** | **MEDIUM** | M09, M11 | **Voting-config boundary ambiguity.** M09's `CompetitionRound` carries a "voting-mode reference"; M11 owns `VoteConfig` "where not M09-owned." Which module owns the voting window/limits config needs one explicit rule. | Rule: M09 owns *competition-structure config* (round timing, whether a round has voting); M11 owns *vote-behavior config* (limits, dedup, abuse toggles). Document split in both specs. | No — clarify |
| **CM-06** | **MEDIUM** | M09, M15 | **`ProgressionConfiguration` ownership left open** (M09 vs M15). | Recommend M15-owned (progression is M15's domain; M09 provides round structure). Confirm + document. | Yes — confirm |
| **CM-07** | **LOW** | M12, M20 | **API namespace drift:** M12 uses `/api/v1/judges/me`; M20 uses `/api/v1/judge/me`. Two surfaces for the same logical resource. | Align to one convention (e.g., `/api/v1/judges` admin + `/api/v1/judge/me` judge-scoped, or unify). Resolve during OpenAPI design — document now. | No |
| **CM-08** | **INFO** | baseline-D2, M07, M08 | Naming evolution: baseline `PortfolioCredit` → actual `ProjectCredit`(M07, authoritative) + `PortfolioItemContribution`(M08, link). | Document the mapping; no change needed — consistent design. | No |
| **CM-09** | **INFO** | baseline, all modules | Baseline **D1–D15 domain numbering ≠ M01–M20 module numbering** (e.g., D4 social-engagement unassigned, D2 splits into M02/M03/M08, D15→M19). Also FRS § references in module docs are approximate traces. | Add a baseline-domain → module mapping table (record here); verify FRS § citations during DB/API design. | No |

**D→M mapping (for CM-09):** D1→M01 · D2→M02+M03+M08 · D3→M04 · D4→⚠️unassigned(CM-01) · D5→M05 · D6→M06 · D7→M07 · D8→M09 · D9→M10 · D10→M11 · D11→M12 · D12→M13 · D13→M14+M15+M16 · D14→M18 · D15→M19(+platform, CM-02) · *(new)* M17, M20 have no baseline domain number.

## 22. Required Corrections

1. **CM-01** — assign `Follow`/`Like`/`Comment`/`EngagementCounter` owner (recommend M02); update M02 ownership table + index.
2. **CM-02** — assign `AuditLog`/`PlatformConfig` platform owner; document.
3. **CM-03** — add `Block` to M06 entities; clarify vs M18 restriction.
4. **CM-04** — standardize `ConversationMember`.
5. **CM-05** — document M09-structure vs M11-behavior config split.
6. **CM-06** — confirm `ProgressionConfiguration` owner (recommend M15).
7. **CM-07** — align `/judge` vs `/judges` namespace.

## 23. Open Product Owner Decisions (consolidated)

Engagement model (follow/like/comment scope — CM-01), AuditLog owner (CM-02), block semantics, all module lifecycle taxonomies (proposed), deadline precedence/timezone, submission snapshot policy, voting limits/reversal, judge onboarding/expertise-verification/COI, aggregation+tie-break formulas, progression modes, publication timing, notification channels/preferences, moderator role model/appeals, admin role matrix, blind judging, retention policies, MFA, multi-tenancy.

## 24. Database Readiness Assessment

**Conditionally ready.** The conceptual model is implementation-ready: ownership is unambiguous for ~95% of entities, lifecycle/versioning boundaries are clean, and no redesign is needed. **Before schema design:** resolve CM-01 (tables have no owner), CM-02 (`AuditLog`/`PlatformConfig` owner), CM-03 (`Block`), CM-04 (naming), CM-05/06 (config boundaries). These are **assignments/clarifications, not redesigns.**

## 25. API Readiness Assessment

**Ready** — `/api/v1/{domain}` namespaces are consistent and module-scoped; RFC 9457/DTO/cursor/idempotency patterns uniform. Minor alignment: CM-07 (`judge` vs `judges`), M13 `/evaluations` vs M20 `/judge/me/evaluations` dual surface (document relationship).

## 26. Final Recommendation

**Proceed to Canonical Database Design after resolving CM-01…CM-06** (all are ownership assignments/clarifications achievable without touching ADRs or module architecture). The design foundation is sound: ownership discipline, state-machine separation, and the multi-talent model held up across all 20 modules. No module requires redesign.

---

## A. Blockers Before Database Design
- **CM-01** Follow/Like/Comment/EngagementCounter owner **(PO decision)**
- **CM-02** AuditLog/PlatformConfig owner **(PO/tech decision)**
- **CM-03** Block entity in M06 ownership
- **CM-04** `ConversationMember` vs `ConversationParticipant` naming
- **CM-05** VoteConfig boundary (M09-structure vs M11-behavior)
- **CM-06** ProgressionConfiguration owner **(PO confirm → M15 recommended)**

## B. Blockers Before API Design
- **CM-07** `/judge` vs `/judges` namespace
- Document M13 `/evaluations` vs M20 `/judge/me/evaluations` relationship

## C. Non-Blocking Follow-Ups
- **CM-08** naming-evolution mapping (done here)
- **CM-09** D→M mapping (done here) + FRS § citation verification
- Consolidate per-module "open decisions" into a master PO-decision register

## D. Product Owner Decisions Required
- Social-engagement owner + engagement feature scope (CM-01)
- AuditLog platform owner (CM-02)
- ProgressionConfiguration owner confirm (CM-06)
- All consolidated open decisions (§23)

## E. Already Consistent — No Change Required
- Identity/authorization separation across all 20 modules (zero violations)
- Multi-talent model end-to-end
- Media ownership + attachment-link pattern
- Team/project = one entry; votes never split; contributors never independently scored
- Competition lifecycle — one owner per transition, 6-state separation
- Qualification (M14) vs Progression (M15) vs Publication (M16)
- Three weight-layer separation
- Immutable versioning + evidence integrity
- Audit ≠ telemetry ≠ analytics
- M19/M20 presentation-only boundaries
- No circular dependencies; no cross-entity duplication

---

## Resolution Status (post-review addendum)

All findings CM-01…CM-07 resolved in `CROSS-MODULE-CONSISTENCY-RESOLUTION-ADDENDUM.md`:

| ID | Resolution |
|----|-----------|
| CM-01 | **M21 Social Engagement** assigned as owner of `Follow`/`Like`/`Comment`/`EngagementCounter` — baseline D4 domain restored as a module (not an invention; M02/M05 placements rejected as boundary violations) |
| CM-02 | **Platform Kernel** (cross-cutting, non-business-module concern) owns `AuditLog`/`PlatformConfig`/analytics projections — all modules append via write-contract; M19 views only; M01 emits auth events into it |
| CM-03 | `UserBlock` added to M06 ownership — user-initiated privacy control, distinct from M18 `ModerationRestriction` |
| CM-04 | `ConversationMember` standardized; baseline/reference docs updated |
| CM-05 | M09 = voting *structure* (does a round have voting + `VoteConfig` ref); M11 = voting *behavior* (`VoteConfig` entity) |
| CM-06 | `ProgressionConfiguration` confirmed **M15-owned** |
| CM-07 | `/api/v1/judges/me` standardized; M20 updated |

**Database design is unblocked** — all entity ownership is now assigned.
