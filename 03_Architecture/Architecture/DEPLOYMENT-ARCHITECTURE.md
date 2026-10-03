# StarMitra — Deployment & Environment Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Accepted in principle | **Decision status:** **OD-12 ACCEPTED IN PRINCIPLE** — ADR-012 formalization pending; cloud provider remains OPEN ([Decision Register](../ADR/ARCHITECTURE-DECISION-REGISTER.md))

## 1. MVP Topology (conceptual)

```text
Clients (Web SPA / RN mobile / Judge / Admin)
        │  HTTPS / WSS
        ▼
   CDN (static frontend + public media)
        │
   Load balancer ──► Spring Boot monolith (container, managed platform)
        │              ├── REST /api/v1/{domain}
        │              ├── WebSocket (messaging, single-instance MVP)
        │              └── Internal job queue / scheduled jobs
        ▼
   Managed PostgreSQL ◄── Flyway migrations
   Object storage ◄── pre-signed direct uploads; async processing
   CDN/signed delivery ◄── media
```

**Principles:** single deployable for the backend; stateless API tier; PostgreSQL managed; binaries never traverse the app (OD-08); single-region MVP.

## 2. Environments — 5 logical environments

| # | Logical environment | Form | Purpose |
|---|--------------------|------|---------|
| 1 | **Local Development** | Local Docker | Dev machines |
| 2 | **Ephemeral Test** *(combines CI-ephemeral + Test/QA)* | CI-spun disposable envs | Automated + QA validation — dedicated always-on Test/QA not justified at MVP; becomes a 6th env if dedicated QA needs demand |
| 3 | **UAT** | Small persistent | `[FRS §37]` validation |
| 4 | **Staging** | Prod-shaped minimal | Pre-release |
| 5 | **Production** | Managed services, single region | Live |

## 3. What MVP does NOT include

Kubernetes, service mesh, Redis, Kafka/RabbitMQ, Elasticsearch/OpenSearch, multi-instance WS fan-out, read replicas (by default), multi-region, dedicated worker infrastructure — each has a documented **trigger** in the register OD-12 §18 and requires a separate decision to adopt.

**WebSocket note:** HTTP backend replicas for availability do NOT automatically solve WS fan-out — multi-instance realtime coordination is a separate future decision (OD-09 preserved).

**Background jobs (no broker):** application-managed job mechanism inside the monolith with: durable job state for business-critical work (PostgreSQL — **in-memory-only queues are never authoritative**), retry + idempotency + duplicate-execution protection, failure handling + job status, graceful shutdown + restart recovery, transaction boundaries, scheduled/watchdog execution. No job framework selected — requirements only.

## 4. CI/CD Principles (not implemented)

Git feature branches → PR; build → container image + static web bundle; automated tests in CI; immutable artifacts promoted across environments; Flyway migrations at deploy; rollback = redeploy prior image; secrets via managed secrets store; per-environment config isolation. **Version-controlled Infrastructure as Code** is the principle — Terraform is the current recommended implementation option, not an immutable architecture requirement.

## 5. Security/Networking

TLS everywhere; DB in private subnet (no public access); backend behind LB only; signed-URL storage access; least-privilege roles; per-env isolation; audited admin access.

## 6. Backup/DR

Managed PG backups + PITR; object-store durability; scheduled restore testing; **RTO/RPO remain OPEN** — FRS specifies no numerical targets; product-owner decision. **MVP infrastructure budget is an open Product Owner decision** — no ceiling invented.

## 7. Provider Neutrality

This document defines *deployment architecture*, not vendor. AWS/GCP/Azure selection requires Product Owner input (credits/pricing) and remains an open sub-decision of OD-12.
