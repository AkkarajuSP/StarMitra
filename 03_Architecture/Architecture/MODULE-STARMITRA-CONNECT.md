# Module Design — 06: StarMitra Connect (Messaging)

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 06)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–05 without modification.

## 1. Purpose

Own StarMitra's messaging capability `[FRS §12]` — 1:1, group, and project-linked conversations with text + media attachments, delivery/read status, report/block, and realtime delivery — as the isolated D6 module of the modular monolith.

## 2. FRS Scope `[FRS §12]` — traced

1:1 conversations · group/project conversations · text messages · image/audio/short-video/document attachments · timestamps · delivery + read status · report/block · project-linked conversations · notification triggers for messaging events.

## 3. Preserved Distinctions

`User` ≠ `SystemRole` ≠ `TalentSkill` ≠ `ProjectContributionRole` ≠ `ConversationMembership` ≠ conversation permission. **TalentSkill never grants messaging permission; ProjectContributionRole never grants messaging permission.** Messaging authz = authenticated user + explicit conversation/project membership + domain authorization.

## 4. ADR Compliance

- **ADR-001:** isolated D6 module, named extraction candidate — not a service
- **ADR-006:** WS handshake + message auth via JWT/session; revocation honored
- **ADR-007:** REST = commands/history/recovery; WS = realtime events; RFC 9457; cursor pagination; idempotency
- **ADR-008:** attachments via Media Management — direct upload, async processing; Connect stores references, never binaries
- **ADR-009:** WS in monolith, PG persistence-first, at-least-once + idempotent dedup, per-conversation ordering, REST recovery, no exactly-once claim
- **ADR-010:** no distributed cache; PG authoritative for all message state
- **ADR-012:** in-app mechanism; business-critical state durable in PG; in-memory never authoritative

## 5. Module Boundary

| Owns | Does not own |
|------|--------------|
| `Conversation`, `ConversationMember`, `Message`, `MessageReceipt`, `MessageAttachment` (ref), idempotency/dedup state | MediaAsset/Variant (MODULE 04), UserProfile (02), identity/roles (01), project membership (Rooms), moderation policy, notification delivery, project contribution roles |

## 6. Conversation Types

| Type | Creation | Membership | Access |
|------|----------|-----------|--------|
| **Direct 1:1** | user-initiated | two members | participants only |
| **Group** | user-created, members added | added/leave/remove *(rules open)* | members only |
| **Project-linked** | tied to Creative Room | derived from **room membership** (read from Rooms module — never duplicated) | room members via Rooms authz |

Lifecycle: create → active → archived/left *(product rules for max participants, auto-creation, retention = OPEN — not invented)*.

## 7. Message Model (conceptual)

`Message`: id (server-generated UUID), conversationId, senderId, clientMessageId (idempotency key), type (text/image/audio/video/document), content (text) OR attachment metadata ref, createdAt, conversation sequence number (ordering), state. **Separate:** content ≠ attachment ref ≠ `MessageReceipt` state ≠ moderation state.

*Edit/delete — not FRS-specified → OPEN decision, not invented.*

## 8. Media Attachments (ADR-008)

```text
send-with-attachment → POST /media (initiate) → pre-signed direct upload → complete
  → Media processes (transcode/scan) → Connect stores MessageAttachment{mediaId, role}
  → message carries attachment metadata (never binary) via WS
  → recipients get signed access URL after conversation-membership check
```

Connect holds the **reference**; Media owns the asset + lifecycle. No duplicated logic.

## 9. Realtime Design (ADR-009)

| REST | WebSocket |
|------|-----------|
| create/list conversations, send message (command), history, pagination, reconnect recovery, block/report | `message.new`, `message.delivered`, `message.read`, conversation updates |

Handshake auth (ADR-006); **per-event participant/membership check**; reconnect → REST `?after=<cursor>` resume + WS re-open; duplicate events deduped via message ID + clientMessageId; server ACK after persistence; per-conversation ordering only — **no global ordering, no exactly-once claim**. Protocol/library (STOMP/native/Socket.IO) = implementation spike — not selected.

## 10. Delivery/Read States

`sent → delivered → read` — persisted per-recipient in `MessageReceipt`; transitions idempotent (repeated `delivered`/`read` harmless); single-user shows aggregate state. FRS-required; receipt-table mechanism = design.

## 11. Ordering & Idempotency

Server message ID + client-provided `clientMessageId` dedup (unique constraint → retried send = same message, not duplicate); per-conversation sequence for ordering; send+receipt-write in one transaction; lost ACK → client dedups on `clientMessageId`; reconnect → REST catch-up by sequence cursor.

## 12. Authorization & Security

Authenticated sender; membership checked per send/event/read; group-member + project-linked (via Rooms) scoping; blocked users can't message `[FRS §12]`; removed members lose access to new messages; uniform errors prevent enumeration; attachment access = membership + media visibility; rate limits *(thresholds open)*; correlation IDs + audit.

## 13. Block / Report

- **Block:** blocker→blocked message-send rejected; historical message visibility + block semantics = **OPEN product decision** (FRS names capability, not policy)
- **Report:** report record passed to Moderation (owns policy); Connect stores reference + audit
- Audit on block/report actions

## 14. Project-Linked Conversations

`Conversation.projectId` → validates membership against **Rooms-owned** membership (read contract) — never a duplicated source of truth; room member → conversation access derived, not stored doubly. Sync behavior not invented (OPEN).

## 15. Notifications

Connect emits domain signals (`MessageReceived`, `MessageRead`*(if product-required)*); Notifications owns delivery/channels. No provider logic in Connect.

## 16. Data Model

| Entity | Authoritative? | Purpose |
|--------|---------------|---------|
| `Conversation` | ✅ | id, type, projectId*nullable*, createdBy, timestamps, state |
| `ConversationMember` | ✅ | conversationId, userId, joinedAt, role *(minimal — not a project role)*, leftAt |
| `Message` | ✅ | id, conversationId, senderId, clientMessageId, type, content, sequence, createdAt, state |
| `MessageReceipt` | ✅ | messageId, userId, deliveredAt, readAt |
| `MessageAttachment` | ✅ | messageId, mediaId (ref→MODULE 04), role |
| Idempotency record | ✅ | clientMessageId→messageId dedup |

All PG-authoritative; no derived projections needed at MVP (receipts computed).

## 17. API Surface (conceptual — ADR-007)

| Endpoint | Purpose |
|----------|---------|
| `POST /api/v1/conversations` | Create (1:1/group/project-linked) |
| `GET /api/v1/conversations` | List (cursor) |
| `GET /api/v1/conversations/{id}` | Detail |
| `POST /api/v1/conversations/{id}/messages` | Send (idempotent via clientMessageId) |
| `GET /api/v1/conversations/{id}/messages` | History (cursor) |
| `POST /api/v1/conversations/{id}/read` | Mark read (idempotent) |
| `GET /api/v1/conversations/recovery` | Post-reconnect catch-up |
| `POST /api/v1/messages/{id}/report` | Report → Moderation |

Errors: RFC 9457 + codes (`CONVERSATION_NOT_FOUND`, `NOT_A_MEMBER`, `MESSAGE_DUPLICATE`*(dedup-succeeds)*, `ATTACHMENT_INVALID`, `USER_BLOCKED`, `CONVERSATION_FORBIDDEN`).

## 18. WebSocket Event Envelope (conceptual)

`{ eventId, eventType, conversationId, messageId?, timestamp, correlationId, sequence, payload }` — type ∈ `message.new|message.delivered|message.read|conversation.updated`. No concrete protocol selected.

## 19. Failure & Recovery

| Failure | Behavior |
|---------|----------|
| WS disconnect | durable state; REST catch-up on reconnect |
| Duplicate send | idempotent → same messageId |
| Persisted but delivery failed | recipient gets it on reconnect (state is source of truth) |
| ACK lost | client dedups via clientMessageId |
| Recipient offline | delivered=0; REST recovery on resume |
| Server restart | WS drops → reconnect+catch-up; PG intact |
| Malformed event | rejected + logged |
| Unauthorized event | rejected |
| Attachment processing failed | attachment marked failed; message still delivers w/ placeholder |
| Moderation rejection | media removed; message per moderation policy |

## 20. Observability

Structured logs (conversationId, messageId, correlationId); metrics (send rate, delivery lag, WS connections, receipt latency, job-queue health); audit events separate; no analytics pipeline.

## 21. Performance / Scalability

Cursor pagination on history/conversations; per-conversation sequence; single-instance WS at MVP; attachments via Media (not WS); PG transactions. **No Redis/Kafka/broker/distributed-fan-out/dedicated-service introduced** — extraction triggers documented (ADR-009).

## 22. Testing (not implemented)

1:1/group/project-linked send+receive; authz (non-member removed/blocked); duplicate send/event; reconnect+catch-up; offline recipient; delivery/read transitions; attachment flow; report→Moderation; notification signals; WS handshake+per-event authz; server-restart recovery.

## 23. Open Decisions (Product Owner)

Max group size, message retention, edit/delete policy, attachment size/type limits, history visibility for new members, block semantics, project-conversation lifecycle sync, notification preferences, typing/presence (not FRS), WS protocol (spike), rate limits, multi-instance fan-out trigger, moderation retention.

## 24. Traceability

- **FRS:** §12 Connect · §13 room linkage · §26 report/block · §30 audit
- **ADRs:** ADR-001 D6 · ADR-003 PG · ADR-006 auth · ADR-007 API · ADR-008 attachments · ADR-009 realtime (primary) · ADR-010 no-cache · ADR-012 jobs
- **MODULE 01:** authz · **04:** media · **Rooms:** membership · **Notifications:** delivery · **Moderation:** policy
