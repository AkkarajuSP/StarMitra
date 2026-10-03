# ADR-008 — Media Architecture: Object Storage + Direct Upload + Async Processing + CDN

| Field | Content |
|-------|---------|
| ADR | ADR-008 |
| Register entry | OD-08 — Media Storage & Processing |
| Status | **ACCEPTED** |
| Date | 2026-10-03 |
| Baseline refs | §14 Media Architecture, §20 Decisions, MEDIA-ARCHITECTURE.md |
| Related | ADR-003 (metadata in PG), ADR-007 (media API contracts), ADR-012 (provider choice) |

## Decision

**Object storage + authorized direct-to-storage upload + asynchronous processing + CDN delivery** — all behind **provider-neutral interfaces**. PostgreSQL holds durable media metadata; binaries never traverse the backend.

## Context

Video/audio/image/document power portfolio, discovery, rooms, submissions and Connect `[FRS §10]`; moderation `[§26]`; scalable storage `[§36]`. Largest infrastructure surface; FRS names no provider.

## Alternatives Considered

- **DB BLOB storage** — rejected: bloats primary store, no streaming/CDN.
- **All-in-one media platform (Cloudinary/Mux-class)** — rejected: egress pricing risk at video-platform scale + lock-in.
- **Self-managed ffmpeg fleet** — rejected: ops burden.

## Rationale

Purpose-built for binaries: durability, lifecycle rules, signed URLs, CDN pairing; composable + provider-neutral; moderate build, low unit cost.

## Guardrails / Constraints (binding)

1. Object storage is the primary binary media storage architecture.
2. PostgreSQL stores durable media metadata and relationships — **not** large media binaries.
3. Media uploads use authorized direct-to-object-storage transfers.
4. Spring Boot does **not** proxy media binaries.
5. Multipart/resumable uploads supported where media size/network conditions justify them.
6. Upload completion is verified by the backend.
7. Media processing is asynchronous.
8. MVP processing supports video, audio, image and document requirements identified in the review.
9. Media metadata and binary objects remain separate concerns.
10. Media access requires StarMitra authorization before signed delivery access is issued.
11. Signed URLs are appropriately short-lived.
12. CDN delivery is part of the target architecture — **no CDN provider selected**.
13. Storage and CDN providers remain provider-neutral until ADR-012.
14. Competition submission media respects submission finalization and audit requirements.
15. Media integrates with moderation.
16. Media deletion/retention policies remain product decisions where not established by the FRS.
17. Storage object keys are backend-controlled — never derived from untrusted client input.
18. Orphaned-object reconciliation is required.
19. No Redis, Kafka, RabbitMQ or other infrastructure is implied.
20. Media processing tooling/provider selection remains separate from this decision.

## Consequences

Moderate pipeline build effort (transcode/scan/orchestration assembled); provider swap is an adapter change, not a redesign; media module is a clean extraction candidate (ADR-001).

## Deferred Items / Future Triggers

Provider selection (ADR-12-adjacent — OD-12 keeps it open); rendition ladder; doc preview; retention policies (product); dedicated worker infra (load trigger).

## Open Items

- Provider pick — pending OD-12/PO decision on cloud
- Retention/deletion policies — product input
