# ADR-001 — Architecture Style: Modular Monolith

| Field | Content |
|-------|---------|
| ADR | ADR-001 |
| Register entry | OD-01 — Architecture Style |
| Status | **ACCEPTED** |
| Date | 2026-10-03 |
| Baseline refs | §5 Logical Architecture, §6 Domain Architecture, §20 Decisions |
| Related | All ADRs — establishes the boundary model |

## Decision

StarMitra is built as a **modular monolith** for MVP: a single deployable containing explicit bounded domain modules (the 15-domain structure from the Architecture Baseline), designed so individual domains can be extracted into independently deployable services later if actual scale, reliability, organizational, or domain requirements justify it.

## Context

The FRS defines ~15 interdependent domains; the competition scoring chain (submission → vote → evaluation → aggregation → ranking → qualification) demands transactional consistency `[FRS §22][§30][§36]`. A small initial team must maximize delivery speed without sacrificing future structure.

## Alternatives Considered

- **Microservices** — rejected: distributed transactions for the scoring pipeline, network failure modes, infra + ops overhead; no demonstrated scale or team-parallel need for MVP.
- **Hybrid (monolith + satellites)** — preserved as a *future path*, not a current decision: media processing and messaging are designed as extraction candidates.

## Rationale

Single transaction boundary for scoring/voting/audit; fastest MVP; module boundaries preserve the extraction option without paying distributed-systems cost now.

## Guardrails / Constraints (binding)

1. Explicit bounded domain/module boundaries — enforced in code (Spring Modulith or equivalent, per ADR-002).
2. This decision does **NOT** mean: one large unstructured codebase; shared unrestricted database access between modules; no domain boundaries; no asynchronous processing; no future microservices.
3. The 15-domain structure of the Architecture Baseline is maintained.
4. Extraction candidates (media processing, messaging, notifications, search, analytics) are boundaries-ready but are **not** deployed as separate services now.

## Consequences

Single deployable + single DB; boundary discipline relies on enforcement mechanism + review; independent domain scaling deferred until extraction is justified.

## Deferred Items / Future Triggers

Any module extraction requires a separate ADR — triggers: measured load, reliability isolation needs, or team-parallel delivery requirements.

## Open Items

None — fully decided.
