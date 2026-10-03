# ADR-005 — Mobile: React Native + TypeScript + Expo

| Field | Content |
|-------|---------|
| ADR | ADR-005 |
| Register entry | OD-05 — Mobile Technology |
| Status | **ACCEPTED** |
| Date | 2026-10-03 |
| Baseline refs | §7 Application Architecture, §20 Decisions |
| Related | ADR-004 (React/TS alignment), ADR-006 (mobile auth), ADR-009 (WS client) |

## Decision

**React Native + TypeScript + Expo** is the approved mobile baseline — scoped as a **Creator + Audience app** per `[FRS §5]`.

## Context

`[FRS §5]` names the Mobile App as the primary channel for Audience and Creators: "create, upload, discover, engage, communicate, participate." Admin Web and Judge Web are explicitly separate channels. FRS specifies no mobile implementation details — this is an architecture choice.

## Alternatives Considered

- **Flutter/Dart** — strongest challenger (UI consistency/performance); explicit cost = separate Dart stack (no shared code/toolchain/hiring).
- **Native Kotlin + Swift** — peak performance; ~2× cost; rejected for MVP.
- **Responsive web / PWA only** — fails FRS "Mobile App" channel; iOS PWA limits; no store presence.

## Rationale

Shares TypeScript + React mental model with ADR-004 web; shared TS types/domain logic via OpenAPI codegen (types/logic share, **not** UI); mature camera/media/notifications ecosystem (Expo); one team can cover web + mobile.

## Guardrails / Constraints (binding)

1. React Native + TypeScript + Expo is the approved mobile baseline.
2. **MVP mobile scope = Creator + Audience.**
3. Admin and Judge experiences remain **web-based** for MVP.
4. Mobile does **not** require full feature parity with web.
5. React web UI reuse must **not** be assumed; shared TypeScript contracts/utilities may be evaluated selectively.
6. Android and iOS are the intended platforms; **release sequencing is a separate decision**.
7. Push notification provider is a **separate decision**.
8. OTA/update tooling is a **separate decision**.
9. Native modules/device-specific capabilities require **separate evaluation**.
10. Expo approved as development platform — **individual Expo services/packages are not automatically approved**.
11. Future Admin/Judge mobile experiences are not prohibited but require a future product/architecture decision.

## Consequences

One language across clients; media-edge cases may need native modules (Expo dev-client mitigates); app-store review cycles affect iteration cadence.

## Deferred Items / Future Triggers

Push provider, OTA tooling, native-module needs, platform release sequencing — each separate decisions.

## Open Items

- Android+iOS both at MVP vs sequencing — product input
- Push provider, OTA tooling — implementation-phase decisions
