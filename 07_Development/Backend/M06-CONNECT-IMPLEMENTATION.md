# M06 — StarMitra Connect — Implementation

**Slice:** ninth business implementation. **Status: complete.**

## Ownership

`conversations`, `conversation_members`, `messages`, `message_receipts`, `message_attachments`, `user_blocks`, `message_idempotency` — full V1.4 surface.

## Contract surface (9 ops, unchanged)

list/create/get conversations · message history (`since`/cursor) · send (clientMessageId dedup) · markRead · block/unblock.

## Models

- **Conversation**: ONE_TO_ONE/GROUP/PROJECT + `project_id` REF; hidden existence (NOT_FOUND for non-members, no leak).
- **Membership**: M06 rows for 1:1/GROUP; **PROJECT → M07 `ProjectMembershipContract`** — never duplicated (proven: room invite unlocks conv access; non-member can't create/send).
- **Message**: server-assigned per-conv `sequence` (conversation-row `PESSIMISTIC_WRITE` serializes sends; `uq_messages_conv_seq` is final guard); `clientMessageId` → `message_idempotency` PK → replay returns original (at-least-once dedup per ADR-009).
- **Receipts**: DELIVERED written at send for other members; `markRead` upserts READ ≤ `upToSequence` (idempotent native upsert).
- **Blocks**: user-level, either-direction enforcement on 1:1 create+send (`BLOCKED` code); self-block → 422; idempotent.

## Realtime (ADR-009)

Existing STOMP foundation used — no new infra: REST persists → `SimpMessagingTemplate` broadcasts `{type:MESSAGE,message:{...}}` to `/topic/conversations/{id}`. `ConversationSubscriptionInterceptor` enforces the **same membership rule on SUBSCRIBE** (JWT'd `WsPrincipal` → `canAccess`). Recovery = REST `?since=` history. At-least-once; single instance; no broker.

## Cross-module

| Dir | Contract |
|---|---|
| M06 → M07 | `ProjectMembershipContract.isActiveMember` (PROJECT conv access) |
| M06 → M04 | `MediaReferenceContract.isUsableBy` (attachments — no binaries) |
| M06 → M18 | user_blocks are user-initiated, NOT moderation — `ProfileRestrictionContract` seam unchanged |

## Security

Conv/message IDOR → NOT_FOUND · non-member send denied · project conv requires live M07 membership · block → `BLOCKED` · hidden convs.

## Testing

`ConnectFlowIT` 6 real-PG17: 1:1 lifecycle+outsider-IDOR, dedup replay (1 row), `since` recovery + markRead receipts, block create+send enforcement (either direction), project-conv M07 integration end-to-end, M04 attachment validation. Suite: **133**.

## Known follow-ups

- WS end-to-end test (STOMP client) — interceptor + broadcast verified by wiring; no live WS client in suite.
- `left_at`/leave-conversation op — not in contract.
- DELIVERED→READ on WS push vs REST-read semantics — open refinement.
