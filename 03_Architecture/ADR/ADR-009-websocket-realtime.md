# ADR-009 — Realtime: WebSocket Inside the Modular Monolith

| Field | Content |
|-------|---------|
| ADR | ADR-009 |
| Register entry | OD-09 — Real-Time Communication |
| Status | **ACCEPTED** |
| Date | 2026-10-03 |
| Baseline refs | §9 API, §20 Decisions, REALTIME-ARCHITECTURE.md |
| Related | ADR-001 (D6 isolation/extraction), ADR-006 (WS auth), ADR-007 (REST/WS boundary), ADR-010 (no-broker) |

## Decision

**WebSocket inside the Spring Boot modular monolith** — isolated messaging module (D6), PostgreSQL persistence-first, at-least-once delivery + idempotent processing — serves StarMitra Connect `[FRS §12]`. Single-instance for MVP.

## Context

FRS §12 requires 1:1 + group/project conversations, attachments, delivery/read status, notifications integration, report/block. Live video/audio/calls/streaming are future/out-of-MVP.

## Alternatives Considered

- **Managed realtime platform** — rejected: per-connection/message pricing at chat scale; lock-in on a core capability.
- **SSE / polling only** — rejected: insufficient for chat UX (delivery/read receipts, bidirectionality).
- **Broker-backed distribution / messaging microservice** — rejected: premature; violates ADR-001.

## Rationale

Zero vendor cost; full control of auth/semantics; Spring WebSocket native; isolated module = clean extraction seam.

## Guardrails / Constraints (binding)

1. WebSocket is the approved realtime transport capability.
2. Realtime messaging remains inside the Spring Boot modular monolith for MVP.
3. StarMitra Connect remains an isolated messaging module/domain.
4. PostgreSQL is the durable source of message state.
5. Message persistence occurs before realtime fan-out.
6. At-least-once delivery semantics with idempotent processing.
7. Exactly-once delivery is not claimed.
8. Server message IDs + clientMessageId support deduplication.
9. Per-conversation ordering where required.
10. REST handles commands/history/recovery; WebSocket handles realtime events.
11. Offline/reconnection recovery uses durable message state and REST recovery.
12. WebSocket authentication follows ADR-006.
13. Per-event authorization and conversation membership checks are required.
14. Token/session revocation must be honored.
15. Talent skills never grant messaging permissions.
16. Media attachments use ADR-008; binaries do not travel through WebSocket.
17. Typing/presence are not mandatory MVP capabilities.
18. Single-instance realtime is sufficient for MVP.
19. Multi-instance fan-out requires a separate architecture decision.
20. Redis/Kafka/RabbitMQ are not approved by ADR-009.
21. STOMP/native WebSocket/Socket.IO protocol selection remains open pending an implementation spike.
22. Live video/audio/calls/streaming remain outside MVP.
23. Connect remains isolated for potential future extraction.

## Consequences

Team owns connection lifecycle/scaling; single-instance constraint at MVP; custom-protocol cost if native WS chosen.

## Deferred Items / Future Triggers

Multi-instance fan-out mechanism (triggered by concurrent-connection scale/availability — ties to ADR-010/ADR-012); presence/typing (product decision); push provider; live media (P2).

## Open Items

- STOMP vs native WS vs Socket.IO — implementation spike
- Presence/typing — product decision
