# StarMitra — Notification Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Draft for review

## 1. Requirements

`[FRS §25]` Notification families: registration/account, submission received/approved/rejected, competition open/close, round progression, voting opened/closed, judge assignment and pending evaluations, results/qualification, messages and Creative Room activity, project invitation/acceptance, system announcements. Reliable delivery required `[FRS §36]`; per-user notification controls `[FRS §12]`.

## 2. Design

`[Proposed]` Event-driven, per-channel rendering:

```text
Domain emits event (e.g. SubmissionApproved, RoundOpened, MessageSent)
        │
        ▼
Notification module
  1. Resolve audience (event-specific: entry owner, followers, room members,
     assigned judges, all users for announcements)
  2. Apply per-user NotificationPreference + mute/block rules
  3. Render NotificationTemplate per channel
  4. Persist Notification (in-app record = source of truth for the bell/inbox)
  5. Dispatch per channel: in-app | push | email | SMS
  6. Track DeliveryAttempt (success/failure/retry)
```

## 3. Channels

| Channel | Use | Status |
|---------|-----|--------|
| In-app inbox | All notification types; primary record | Proposed |
| Push (mobile) | High-priority: results, deadlines, messages, invitations | Proposed `[OD-7]` |
| Email | Account flows, summaries, announcements | Proposed `[OD-7]` |
| SMS | OTP/auth `[FRS §8]`; optional critical alerts | Proposed `[OD-7]` |

## 4. Preferences & Controls

`[FRS §12][§25]` `[Proposed]` Per-user `NotificationPreference` keyed by notification family × channel, with defaults per family. Message-specific mute per conversation; blocked users never generate notifications `[FRS §12 report/block]`.

## 5. Key Entities

`Notification` (in-app record), `NotificationTemplate` (per family × channel, admin-editable `[Proposed]`), `NotificationPreference`, `DeliveryAttempt` (channel, status, retry count, provider ref).

## 6. Reliability

`[Proposed]`

- At-least-once delivery from domain events; dedupe by event ID.
- Retry with backoff on provider failure; dead-letter + alert on exhaustion `[FRS §36]`.
- Judge deadline reminders are scheduled jobs (pending evaluations `[FRS §25][§28]`), not just event reactions.
- Announcements = admin-triggered broadcast `[FRS §25][§27 notification management]`.

## 7. Boundaries

- Notification module never originates business facts — only reacts to events.
- Ordering not guaranteed across channels; in-app record is canonical.
- `[Open]` Real-time in-app delivery (WebSocket push vs poll) — tied to OD-8.
