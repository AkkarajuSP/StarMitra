# ADR-012 — Cloud & Deployment Architecture

| Field | Content |
|-------|---------|
| ADR | ADR-012 |
| Register entry | OD-12 — Cloud / Deployment |
| Status | **ACCEPTED IN PRINCIPLE — PENDING FINAL ADR FORMALIZATION** (final ACCEPTED upon confirmation; provider remains open) |
| Date | 2026-10-03 |
| Baseline refs | §16 Deployment, §20 Decisions, DEPLOYMENT-ARCHITECTURE.md |
| Related | All ADRs — hosts the accepted stack |

## Decision

**Managed container platform + Spring Boot modular monolith + managed PostgreSQL + object storage + CDN** on a single major cloud, single-region MVP — **without Kubernetes**, broker, or other deferred infrastructure. The **cloud provider remains OPEN**.

## Context

Deployment must host the accepted architecture (OD-01…OD-11): one backend deployable, managed PG, provider-neutral media, single-instance WS, no distributed cache, PG-native search. FRS requires backup/recovery + security `[§30][§36]` — no provider named.

## Alternatives Considered

- **PaaS** — rejected beyond alpha: media-bandwidth economics, re-platforming risk.
- **VPS/self-managed** — rejected: ops burden.
- **VM-based** — rejected: needless ops vs managed containers.
- **Kubernetes** — rejected at MVP: orchestration overkill for a monolith.

## Rationale

Every needed managed primitive exists; scales with product; media-capable; startup credits possible; keeps ops proportional to a small team.

## Guardrails / Constraints (binding)

1. Deployment architecture is provider-independent — **provider selection remains OPEN** (no AWS/Azure/GCP lock-in in this decision).
2. **Kubernetes not approved** — deferred to documented triggers.
3. Environment model = **5 logical environments**: Local Development, Ephemeral Test (CI-ephemeral + Test/QA intentionally combined), UAT, Staging, Production.
4. Background jobs: application-managed mechanism inside the monolith; **in-memory-only queues must not be authoritative for business-critical work**; durable job state in PostgreSQL.
5. Single-instance WebSocket preserved (ADR-009); **HTTP replicas do NOT imply WS fan-out** — separate future decision.
6. **Version-controlled Infrastructure as Code** is the principle — Terraform is the current recommended implementation option, not an immutable requirement.
7. **RTO/RPO remain open** — FRS specifies no numerical targets.
8. **MVP infrastructure budget remains an open Product Owner decision.**
9. All prior decisions (OD-01…OD-11) preserved.
10. All deferred technologies retain explicit future triggers.

## Deferred Items / Future Triggers

Kubernetes (multi-service orchestration), Redis (ADR-010 triggers), Kafka/RabbitMQ (distributed eventing need), ES/OS (ADR-011 triggers), multi-instance WS fan-out (connection scale), read replicas (measured contention), DB partitioning (volume), multi-region (latency/DR), CDN expansion, dedicated worker infra.

## Consequences

Cloud-agnostic interface discipline required; single-region accepts regional-outage exposure; provider decision pending.

## Open Items (Product Owner)

- Cloud provider choice (credits/pricing)
- RTO/RPO targets
- MVP infrastructure budget
- Test/QA persistence need
