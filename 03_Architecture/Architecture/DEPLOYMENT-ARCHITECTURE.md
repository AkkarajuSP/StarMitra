# StarMitra — Deployment & Environment Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Draft for review | **Decision status:** under review as **OD-12** in the [Decision Register](../ADR/ARCHITECTURE-DECISION-REGISTER.md)

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

## 2. Environments

| Env | Form | Purpose |
|-----|------|---------|
| Development | Local Docker | Dev machines |
| Test/QA | CI-ephemeral / small shared | Automated tests |
| UAT | Small persistent | `[FRS §37]` validation |
| Staging | Prod-shaped minimal | Pre-release |
| Production | Managed services, single region | Live |

## 3. What MVP does NOT include

Kubernetes, service mesh, Redis, Kafka/RabbitMQ, Elasticsearch/OpenSearch, multi-instance WS fan-out, read replicas (by default), multi-region — each has a documented **trigger** in the register OD-12 §18 and requires a separate decision to adopt.

## 4. CI/CD Principles (not implemented)

Git feature branches → PR; build → container image + static web bundle; automated tests in CI; immutable artifacts promoted across environments; Flyway migrations at deploy; rollback = redeploy prior image; secrets via managed secrets store; per-environment config isolation.

## 5. Security/Networking

TLS everywhere; DB in private subnet (no public access); backend behind LB only; signed-URL storage access; least-privilege roles; per-env isolation; audited admin access.

## 6. Backup/DR

Managed PG backups + PITR; object-store durability; scheduled restore testing; RTO/RPO **not invented** — product-owner decision flagged.

## 7. Provider Neutrality

This document defines *deployment architecture*, not vendor. AWS/GCP/Azure selection requires Product Owner input (credits/pricing) and remains an open sub-decision of OD-12.
