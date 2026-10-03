# StarMitra — Realtime Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Draft for review | **Decision status:** under review as **OD-09** in the [Decision Register](../ADR/ARCHITECTURE-DECISION-REGISTER.md)

## 1. Scope

**MVP `[FRS §12]` (StarMitra Connect):** 1:1 conversations, group/project conversations, text messages, image/audio/short-video/document attachments, timestamps, delivery status, read status, project-linked conversations, in-app notification events, report/block.

**Explicitly out of scope (future):** live video/audio, voice/video calls, live streaming, realtime collaborative editing `[FRS §4.2][§35]`. Presence/typing indicators are not FRS-mandated — separate product decision.

## 2. Architecture

`[Proposed]` **WebSocket inside the Spring Boot modular monolith**, in the isolated messaging module (D6), per OD-09 review:

- **Transport:** WebSocket. Protocol detail (STOMP-over-WS vs native WS frames) deferred to implementation spike — `[Open]`.
- **Persistence-first:** messages durably stored in PostgreSQL (`Conversation`, `ConversationParticipant`, `Message`, `MessageAttachment`, `MessageReceipt`) **before** fan-out. WebSocket is transport, not storage.
- **Delivery semantics:** at-least-once transport + idempotent processing; client-generated `clientMessageId` dedup (unique constraint per OD-07); server ACK confirms persistence; **no exactly-once claim**.
- **Ordering:** per-conversation sequence only (minimum necessary).
- **Reconnect:** client resumes via REST history (`?after=<cursor>`) + re-opened WS; missed-message recovery is a read-path.

## 3. REST vs WebSocket Boundary (per OD-07)

| REST — commands & history | WebSocket — realtime events |
|---------------------------|-----------------------------|
| `/api/v1/conversations` CRUD, history, pagination, attachment initiation, block/report, admin | `message.new`, `message.delivered`, `message.read`, conversation updates, in-app notifications, live counters where enabled `[FRS §18]` |

## 4. AuthN/AuthZ (OD-06)

Handshake authenticates via token/session; per-connection principal; every event authorized against conversation-participant + room-membership rules; suspended/blocked users refused/closed; connections honor revocation. **TalentSkill grants no messaging permission `[BR-2]`.**

## 5. Attachments (OD-08)

Attachment = `MessageAttachment` → `MediaAsset` reference; upload via OD-08 direct-to-storage flow; WS carries metadata only — never binaries; access via signed URLs after membership check.

## 6. Scaling Path

Single-instance WS suffices for MVP. Multi-instance fan-out (sticky sessions or pub/sub adapter) is a **future dependency requiring separate decision** — no Redis/Kafka/RabbitMQ approved. Module isolation means swapping in a distributed adapter is configuration-level, not a rewrite.

## 7. Failure & Security

- Failures: reconnect+REST catch-up; duplicate suppression; persistence-failure = send error (no ghost messages); expired credentials → re-auth challenge.
- Security: connection limits, message size caps, send rate limits, block/report enforcement `[FRS §12]`, attachment validation per OD-08, idle timeouts, audit `[FRS §30]`.

## 8. Extraction

D6 is the named extraction candidate: isolated tables, namespaced endpoints (`/ws/messaging`, `/api/v1/conversations`), internal contracts for cross-domain reads. Extraction = lift module + endpoint registration; no domain redesign.
