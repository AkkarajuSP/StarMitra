# Module Design — 04: Media Management

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Design — implementation-ready | **Phase:** Module Design (MODULE 04)

> Detailed design only — no implementation. Preserves ADR-001…ADR-013 and MODULE 01–03 without modification.

## 1. Purpose

Own the **media lifecycle**: durable metadata, upload orchestration, async processing, delivery access — a cross-domain capability consumed by Profile, Portfolio, Discovery, Creative Rooms, Competitions/Submissions, Connect, and Moderation. Media consumes business *context*; it never owns business *entities*.

## 2. Responsibilities

- `MediaAsset` + `MediaVariant` metadata and lifecycle
- Upload orchestration (initiate → pre-signed → complete → validate)
- Async processing pipeline (transcode/thumbnail/scan/extract)
- **Access authorization before signed delivery** (application-level authz)
- Media visibility/moderation *state* (not policy)
- Storage-object references (provider-neutral)
- Orphan reconciliation + upload-completion verification

## 3. Non-Responsibilities

Does **not** own: Profile/Portfolio/Competition/Submission/CreativeRoom/Message entities, user authorization, TalentSkill, contribution roles, moderation *decisions*, or the business-purpose of any media. Binaries are in object storage; media *binary data* is never in PostgreSQL.

## 4. ADR-008 Compliance (preserved verbatim)

Object storage + direct-to-storage upload + async processing + CDN/signed delivery — provider-neutral; PG=metadata only; authz-before-URL; backend never proxies binaries. **No storage/CDN provider selected** — deferred to deployment/provider decision.

## 5. Media Types & Handling (FRS-traced `[§10]`)

| Type | Validation | Processing (MVP) | Metadata |
|------|-----------|------------------|----------|
| **Video** | MIME+magic+size+container | transcode, thumbnail/poster, duration, resolution | duration, dimensions, codec *(proposal)* |
| **Audio** | MIME+magic+size | transcode, duration; waveform *(optional — not justified for MVP)* | duration |
| **Image** | MIME+magic+size | resize variants, thumbnail, **EXIF strip** | dimensions |
| **Document** | MIME+magic+size | **malware scan**, page count *(proposal)*; preview *(optional)* | page count |

*Fields marked proposal are design suggestions, not mandates.*

## 6. Media Lifecycle — separated states

Don't collapse into one status — **four orthogonal dimensions**:

| Dimension | States | Owner |
|-----------|--------|-------|
| **Upload** | `Requested → Uploading → Uploaded → (Aborted/Failed)` | Media |
| **Processing** | `Pending → Processing → Ready | Failed` | Media |
| **Moderation** | `Clear → UnderReview → Approved | Rejected → Removed` | Moderation decides; Media enforces |
| **Visibility** | `Public / Private / Restricted` | Domain sets; Media stores |

## 7. Upload Flow

```text
Client → POST /media (metadata+intent)
  → authz + validate (type/size) → issue pre-signed URL + server-generated object key
  → client PUT direct to object storage
  → POST /media/{id}/complete → verify existence/size/checksum → mark Uploaded
  → enqueue async processing → Ready
```

- Object key = backend-generated (opaque, never from filename); multipart/resumable supported where justified
- Incomplete uploads → lifecycle-expiry cleanup; orphans → reconciliation job
- Completion is **idempotent** (repeated complete = safe)

## 8. Processing (MVP scope — per type §5)

Async via application-managed job mechanism (ADR-012 §10 — durable PG job state, retry, idempotency). No broker. AI/ML deferred.

## 9. Media Variants

`MediaAsset 1—* MediaVariant` — original + derived renditions (thumbnail, preview, transcode, resized). **Variants are derived/regenerable** — never authoritative records; the asset's metadata + original object are authoritative.

## 10. Storage Object Keys (provider-neutral)

Backend-controlled, opaque, non-PII, versioning-capable, reconciliation-friendly. Format = implementation detail (not locked). Never derived from untrusted input.

## 11. Access Control — two layers

1. **Application authorization** — backend checks domain authz *before* issuing a signed URL (profile owner, room member, judge-assigned, conversation participant, admin)
2. **Storage protection** — ACL/signed-URL expiry as second layer

Media URL alone **never** bypasses domain authz. Access classes: public, authenticated-only, private, competition/submission, room, message-attachment, moderation-restricted — each resolved by owning domain's authz, mediated by Media.

## 12. Media Visibility ≠ Authorization ≠ Moderation

`MediaAsset.visibility=public` **does not** override a private parent's restrictions — a private room asset stays private regardless of media-level flag. Visibility is a delivery hint; domain authz is authoritative.

## 13. Business Attachment Model

```text
MediaAsset ↑── MediaReference/Attachment (ownerModule, ownerEntityId, role) ↑── Business Entity
```

Explicit per-module attachment entities preferred over loose polymorphism: `ProfileAvatar`, `PortfolioItemMedia`, `SubmissionMedia`, `RoomMedia`, `MessageAttachment` — each owned by its business module, holding a `MediaAsset` reference. **Media owns the asset; business modules own the attachment.**

## 14. Competition Media

- `SubmissionMedia` association locked at submission finalization `[§16]` — immutable evidence
- No replacement post-finalization (new media = new submission/attachment)
- Audit trail on evidence changes; judge access = assigned-scope only
- Retention per audit policy — **durations not invented** (PO decision)

## 15. Moderation Boundary

Moderation *decides* (UnderReview/Approved/Rejected/Removed); Media *enforces* delivery restriction + state. Policy never duplicated here.

## 16. Profile / Portfolio / Connect Consumption

- **Profile/Portfolio:** avatar/cover/item-media references via business attachment; upload through this module's flow
- **Connect:** attachment upload → media ID carried in message metadata (WS carries metadata only, per ADR-009); access = conversation-membership + media visibility; `MessageAttachment` owned by Connect

## 17. API Surface (conceptual — ADR-007)

| Endpoint | Purpose | Auth | Authz |
|----------|---------|------|-------|
| `POST /api/v1/media` | Initiate upload | auth'd | uploader + context |
| `POST /api/v1/media/{id}/complete` | Mark uploaded | auth'd | uploader |
| `GET /api/v1/media/{id}` | Metadata + status | auth'd | per access class |
| `GET /api/v1/media/{id}/access` | Signed delivery URL | auth'd | domain authz |
| `GET /api/v1/media/{id}/variants` | List variants | auth'd | per access class |
| `POST /api/v1/media/{id}/retry` | Re-process failed | auth'd | uploader/admin |
| `DELETE /api/v1/media/{id}` | Remove | auth'd | owner/moderation |

Errors: RFC 9457 + codes (`MEDIA_NOT_FOUND`, `MEDIA_TYPE_UNSUPPORTED`, `MEDIA_TOO_LARGE`, `MEDIA_UPLOAD_INCOMPLETE`, `MEDIA_PROCESSING_FAILED`, `MEDIA_FORBIDDEN`, `MEDIA_REMOVED`) + correlation ID. Idempotent initiate/complete.

## 18. Data Ownership

**Owns:** `MediaAsset` (id, owner, type, MIME, size, storageRef, checksum, duration/dims, upload/processing/moderation/visibility states, timestamps), `MediaVariant`. **References:** owner user, business attachments (owned elsewhere). Deletion = media-level removal; retention per policy.

## 19. Failure / Recovery

Upload interrupted → abandoned → TTL expiry; complete-fails → client retries idempotently; processing fails → retry + status=Failed + alert; worker crash → job recovery (durable state); storage down → health + retry; orphans (object w/o asset, asset w/o object) → scheduled reconciliation; duplicate complete → dedup; PG metadata authoritative throughout.

## 20. Security Threats & Mitigations

MIME spoofing (magic-byte sniff), oversize (limits+throttle), malware (scan), key manipulation (server-generated), unauthorized access (authz-before-URL + storage ACL), leaked signed URL (short expiry), cross-user access (participant/owner scoping), enumeration (uniform errors + rate limits), EXIF privacy (strip), executable uploads (type allowlist).

## 21. Privacy / Retention

Personal/private media scope-gated; competition evidence retained per audit; user-deletion → object delete + metadata tombstone; moderation removal → delivery revoke + retention per policy. **All durations = open PO decisions — none invented.**

## 22. Observability vs Audit

**Audit:** authz-sensitive actions, deletions, moderation interactions, competition-evidence changes. **Observe:** processing/upload failures, latency, storage errors, job-queue health. Never conflated (ADR-013).

## 23. Testing (not implemented)

Upload round-trip, authz-matrix (owner/participant/outsider), signed-URL issuance+expiry, MIME/size/checksum validation, processing lifecycle, retry/idempotency, orphan reconciliation, moderation enforcement, competition-evidence immutability, threat-matrix tests.

## 24. Open Questions (Product Owner)

1. Storage provider, CDN provider, transcoding/scanner tool — provider decisions
2. Exact size limits, signed-URL expiry, retention durations, resumable-upload implementation, streaming format
3. Deletion semantics (hard vs soft) — policy
4. Moderation evidence retention

## 25. Traceability

- **FRS:** §10 media lifecycle · §9 portfolio · §12 Connect attachments · §13 room media · §16 submission evidence · §26 moderation · §30 audit · §36 scalable storage
- **ADRs:** ADR-003 metadata-only-in-PG · ADR-007 API/errors · ADR-008 media (primary) · ADR-009 WS-metadata-only · ADR-012 async jobs
- **MODULE 01:** authz principal · **MODULE 02:** profile refs · **MODULE 03:** skill-tagged media context
