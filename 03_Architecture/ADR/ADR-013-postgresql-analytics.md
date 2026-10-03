# ADR-013 — Analytics: PostgreSQL Operational Reporting

| Field | Content |
|-------|---------|
| ADR | ADR-013 |
| Register entry | OD-13 — Analytics |
| Status | **ACCEPTED IN PRINCIPLE — PENDING FINAL ADR FORMALIZATION** (final ACCEPTED upon review confirmation) |
| Date | 2026-10-03 |
| Baseline refs | §20 Decisions, ANALYTICS-ARCHITECTURE.md |
| Related | ADR-003 (PG), ADR-001 (D15 module), ADR-007 (reporting APIs), ADR-010 (no-cache), ADR-012 (replica trigger) |

## Decision

**PostgreSQL-based operational reporting** for MVP: direct indexed queries + read-model/materialized rollup tables inside the D15 module, scheduled aggregation jobs for heavy aggregates — covering all `[FRS §29]` metrics. No warehouse, event streaming, or third-party analytics platform.

## Context

`[FRS §29]` requires admin analytics/reporting dashboards; `[FRS §30]` audit is separate. No FRS requirement mandates a warehouse or third-party analytics platform.

## Alternatives Considered

- **Third-party analytics (PostHog/Mixpanel/GA-class)** — deferred: product decision + privacy review required; not FRS-mandated.
- **Warehouse/data platform** — rejected: premature.
- **Event streaming/CDC pipeline** — rejected: no high-volume event requirement.

## Rationale

All §29 metrics are operational aggregates of system-of-record data — derivable without new infrastructure; consistent with PG-first architecture; analytics never produces or overrides authoritative competition results.

## Guardrails / Constraints (binding)

1. PostgreSQL is authoritative for all analytics inputs and competition results.
2. Reporting/read models are **derived** — refreshable, never authoritative.
3. AuditLog remains **separate** from analytics telemetry `[FRS §30]`.
4. Analytics must not become authoritative for competition results.
5. Analytics cannot bypass authorization, privacy, moderation, or competition-access controls — admin-role-scoped surfaces.
6. Product-usage analytics (funnels/retention) is **inferred**, not FRS — deferred to product decision.
7. First-party event capture, if adopted later, is DB-persisted with documented event principles — not implemented now.
8. No warehouse, third-party analytics platform, event streaming, CDC, or dedicated pipeline — all deferred with explicit triggers.
9. No Redis/Kafka/RabbitMQ is implied by this decision.

## Deferred Items / Future Triggers

Separate analytics datastore (§29 load degrades OLTP despite rollups), warehouse (cross-source BI questions), event streaming/CDC (high-volume first-party capture), BI platform (self-serve reporting need), real-time analytics infra (live dashboards product requirement), any third-party tool (product + privacy review).

## Consequences

§29 delivered without new infra; product-level insight (funnels/retention) deferred — accepted gap; rollup jobs add scheduled-work complexity (covered by ADR-012 job mechanism).

## Open Items (Product Owner)

- Product-usage analytics appetite at MVP (first-party vs tool vs none)
- Reporting freshness SLA
- Analytics retention/anonymization policy
- Consent/legal review for future event tracking
