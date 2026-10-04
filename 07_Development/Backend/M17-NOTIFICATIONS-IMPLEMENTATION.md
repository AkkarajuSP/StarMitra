# M17 — Notifications — Implementation

**Slice:** eighteenth business implementation. **Status: complete.** M17 owns notification state/delivery — never business truth (source modules stay authoritative; M17 is a sink).

## Ownership (6 V1.15 tables, mapped verbatim)

`notifications` (recipient, type, body, source_ref, deep_link, UNREAD/READ) · `notification_preferences` (PK user+type+channel — delivery toggle only) · `notification_templates` (versioned `{param}` body templates) · `notification_delivery_attempts` (IN_APP ledger) · `notification_read_states` (PK notification+user — idempotent reads) · `notification_event_references` (UQ dedup key).

## Contract surface (5 ops, unchanged)

listNotifications (cursor, unreadOnly) · markNotificationRead (204) · markAllNotificationsRead (204) · getNotificationPrefs · putNotificationPrefs.

## Event architecture

`NotificationContract.notifyEvent(module, eventType, sourceId, eventVersion, type, recipients, params, deepLink)` — internal sink; source module stays authoritative for business facts; recipients are always server-derived (never client input).

- **Dedup**: `uq_ner_dedup` (module+type+source+version) inserted first — retry/concurrent delivery no-ops at DB level
- **Types**: controlled enum (19 values + GENERIC fallback); unknown strings coerced, never arbitrary
- **Templates**: versioned server-side `{param}` render — client can't inject body
- **Prefs**: disabled → notification not created (delivery only); IN_APP channel only, provider-neutral

## Testing

`NotificationFlowIT` 5 real-PG17: dedup replay, unread→read lifecycle + idempotent mark + unreadOnly, cross-user read IDOR → NOT_FOUND, pref disable/enable (delivery not event), server-side template render. Suite: **182**.

## Known follow-ups

- Partial-failure on multi-recipient events: dedup ref is inserted once before recipient fan-out — a mid-fan-out failure + retry skips undelivered recipients (documented; single-recipient events are unaffected).
- Realtime push (ADR-009 WS) — not wired; REST inbox is source of truth per MVP.
- Retention policy — open product decision (no silent deletion).
- Producer wiring (M06/M21/M15/M16 calling `notifyEvent`) — contract ready; producers call it in their own slices per policy.
