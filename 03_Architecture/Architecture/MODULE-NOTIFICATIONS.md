# Module Design — 17: Notifications

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 17)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–16 without modification. Cross-cutting **delivery** capability — never owns business rules.

## 1. Purpose

Own **notification generation + delivery**: consume domain events from business modules, apply notification policy, manage notification lifecycle, deliver via in-app (MVP) + future channels, track state, and maintain auditability — while the *source business truth stays with the originating module.*

## 2. Event → Notification → Delivery separation

```text
Business module:  "Something happened"        (M06/M07/M09-16 — source truth)
        ↓ domain signal/event
M17:             "This may need a notification"  (policy + creation + delivery)
        ↓
Delivery provider: "Deliver it"               (channel — future)
```

**Delivery failure never modifies source state** — a failed notification doesn't cancel the invitation/submission/result.

## 3. FRS Scope `[FRS §31]` — traced

Notifications = MVP capability covering relevant events: auth/account, profile, Creative Room invitations/project events, Connect messages, competition/submission/voting/judging/results/progression/leaderboard events, moderation/admin actions. **Exact trigger catalog = open — not invented.**

## 4. Module Boundary

| Owns | Does NOT own |
|------|--------------|
| `Notification`, `NotificationPreference`, `NotificationTemplate`, `NotificationDeliveryAttempt`, `NotificationReadState`, `NotificationEventReference` | The source business event/entity (message, invitation, submission, result — owned by M06–M16); business rules; notification content's *meaning* |

## 5. Source Event Model — monolith-compatible

Domain signal mechanism: **in-process domain events / application events / direct module invocation / transactional-outbox-like persistence** — all options compatible with ADR-001/012. **No Kafka/RabbitMQ.** Outbox-style durable handoff = design option, not a new infrastructure dependency.

## 6. Notification Lifecycle

`Created → Pending → Delivered | Failed → Read | Expired` *(proposed — open)*. Three distinct dimensions: **creation** (notification exists) ≠ **delivery attempt** (channel dispatched) ≠ **read state** (recipient opened). Delivered ≠ read.

## 7. In-App Notification Model (MVP)

`{recipientId, type, title, body, sourceRef*(deep-link to stable domain resource)*, createdAt, readState, readAt, expiry}`. Lightweight presentation data + stable reference — **no large business payload embedded**; full context resolved via the referenced resource.

## 8. Delivery Channels

| MVP | Future |
|-----|--------|
| **In-app** (durable record + UI surface) | email, push, SMS, messaging — provider **not selected** (Firebase/OneSignal/Twilio/SendGrid/SNS = PO decisions) |

## 9. Notification Preferences

`NotificationPreference`: userId + type + channel + enabled + frequency/digest *(where supported)*. **Mandatory/security notifications have different preference semantics** — preferences can't suppress required business/security notifications without explicit product rule. Granularity = open.

## 10. Templates / Content

`NotificationTemplate`: type + channel + title/body template + localization key *(open)* + version + active. No CMS; no arbitrary scripting; business data stays outside templates where possible. Localization = open.

## 11. Deep Links / Navigation

Notification references a stable domain resource (message / room / competition / submission / result / leaderboard). **Link never grants access** — resource authz is evaluated when the user follows it, per the owning module.

## 12. Read / Unread

Delivered ≠ opened ≠ read — readState per-notification per-recipient, timestamped, idempotent. Unread count = derived from durable read-state, never cached-authoritative.

## 13. Retry / Failure

Provider/transient/permanent failure + timeout + duplicate attempt + restart + partial delivery — **PG-durable delivery state**; retry/backoff conceptual; in-memory never authoritative; retry exhaustion flagged.

## 14. Idempotency

Duplicate source events/retries deduplicated via `{sourceModule, eventType, sourceEntityId, eventVersion}` — **at-least-once processing tolerated; no exactly-once claim; no duplicated notifications.**

## 15. Ordering

Bounded ordering only where product-required (e.g., competition result before publication notification) — **no global ordering, no broker introduced to solve it.**

## 16. Bulk / Broadcast

Participant/judge/member/admin broadcasts — conceptual batching; never bypasses authz or privacy; **not a marketing platform.**

## 17. Competition Notifications — boundary

Competition published/deadline/submission status/voting/judging/results/qualification/leaderboard events — consumed as M09–M16 outcomes; **no competition state machine here.**

## 18. Creative Room / Connect Boundaries

- **M07:** invitation sent/accepted/declined, member changes, task/project events
- **M06:** new message, conversation events

M17 delivers; never duplicates Conversation/Message/CreativeRoom/ProjectMember.

## 19. Judge / Admin Boundaries

Judge: assignment/evaluation/deadline/result events per M12/13 scope. Admin: moderation/action/workflow alerts. No judge/admin workflows here.

## 20. Security / Privacy

Content leakage, unauthorized recipient, private-resource disclosure, stale links, deleted/restricted/blocked users, moderated content — **notification existence never bypasses authz**; sensitive content kept out of bodies unless approved.

## 21. Authorization

SystemRole authorizes admin capabilities; `TalentSkill`/`ProjectContributionRole`/`JudgeExpertise` never do; `JudgeAssignment` determines judge-recipient scope (consumed, not recreated). Recipients explicitly determined by source domain.

## 22. API Boundary (conceptual — ADR-007)

`GET /api/v1/notifications` (cursor) · `GET /{id}` · `POST /{id}/read` · `POST /read-all` · `GET /unread-count` · `GET/PUT /preferences` · admin `GET /deliveries/{id}` — `/api/v1/notifications`; DTOs; Problem Details; no internal event APIs exposed.

## 23. Observability

Creation, delivery success/failure, retries, provider latency, unread-query latency, backlog, failed deliveries, duplicate suppression — telemetry ≠ audit ≠ analytics.

## 24. Auditability

Audited: preference changes, admin actions, required delivery-state transitions, suppression, retry exhaustion, security-sensitive notifications — source event + correlation refs; established AuditLog, not duplicated.

## 25. Data Integrity

PG invariants: recipient, notification identity, source-event dedup, read state, delivery consistency, preference consistency, template-version refs. **Notification data never business-outcome-authoritative.**

## 26. Failure / Recovery

Creation/event-processing/provider-outage/restart/retry-exhaustion/duplicate-events/stale-refs/deleted-recipients — all recoverable; **business transactions never depend on external delivery succeeding** unless an explicit product rule requires it.

## 27. Future Channels / Extensibility

Provider-neutral channel abstraction — push/email/SMS/WhatsApp/digest possible futures; **no provider selected now, no notification microservice, modular monolith stays.**

## 28. Open Product Owner Decisions

MVP channels, push/email/SMS providers, trigger catalog, mandatory-vs-optional, preference granularity, digest/frequency, template ownership, localization, retention, expiry, retry policy, delivery SLA, unread semantics, broadcast policy, security-notification policy.

## 29. FRS Traceability

`[FRS §31]` notifications · auth/account · messaging §12 · Creative Rooms §13 · competitions §14–17 · voting §18 · judging §19–21 · results §22–25 · moderation §26 · audit §30.

## 30. ADR Validation

ADR-003 PG ✓ · ADR-006 auth ✓ · ADR-007 API ✓ · ADR-009 (WS = delivery mechanism, not business authority) ✓ · ADR-010 no-cache-authority ✓ · ADR-012 no-broker/in-monolith ✓ · ADR-013 analytics boundary ✓

## 31. Acceptance Validation

Delivery-capability ✓ · source-truth stays with business modules ✓ · delivery-failure ≠ business-failure ✓ · in-app MVP ✓ · channels provider-neutral ✓ · preferences configurable + mandatory protected ✓ · templates versioned ✓ · delivered≠read ✓ · idempotent + durable ✓ · bounded ordering ✓ · no broker/service ✓ · skills/roles never authorize ✓ · PG authoritative ✓ · no infra ✓ · no impl ✓
