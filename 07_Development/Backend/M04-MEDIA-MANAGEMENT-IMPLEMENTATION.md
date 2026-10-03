# M04 — Media Management — Implementation

**Slice:** fourth business implementation. **Status: complete.**

## Ownership

M04 owns `media_assets` + `media_variants` — the platform media capability. Business attachments (M02 avatar, M06 attachments, M08 portfolio media, M10 evidence) are **references by UUID only** — those modules own the attachment rows; M04 owns the asset.

## Contract surface (5 ops, unchanged)

| Op | Path | Auth |
|---|---|---|
| createMediaAsset | POST /media | authenticated |
| getUploadUrl | POST /media/{id}/upload-url | owner |
| completeMediaUpload | POST /media/{id}/complete | owner |
| getMediaStatus | GET /media/{id} | owner or publicly-deliverable |
| getDeliveryUrl | GET /media/{id}/delivery-url | owner or PUBLIC+processed+unrestricted |

## 4D lifecycle (schema-verbatim)

`upload_state` INITIATED→UPLOADED→VERIFIED (FAILED) · `processing_state` PENDING→PROCESSING→COMPLETED/FAILED/NOT_REQUIRED · `moderation_state` PENDING/APPROVED/REJECTED/RESTRICTED (M18's decision — M04 stores, never decides) · `visibility` PUBLIC/FOLLOWERS/COLLABORATION_ONLY/PRIVATE.

## Upload sequence (ADR-008)

`create` validates type∈{IMAGE,VIDEO,AUDIO,DOCUMENT} + MIME-prefix consistency + size ≤ 50MB (config) + filename sanitization → backend mints `objectKey = {owner}/{uuid}.{ext}` (client never chooses) → `upload-url` returns presigned PUT (owner, INITIATED only) → client uploads **direct to storage** → `complete` verifies `objectExists` (real check, not client claim) → VERIFIED → `LocalMediaProcessor` runs synchronously (durable in-process; swappable) → variants persisted.

## Content validation (real, not claimed)

IMAGE: `ImageIO.read` decodes actual bytes — spoofed/corrupt images → processing FAILED (IT-proven with a fake PNG). VIDEO/AUDIO/DOCUMENT: `NOT_REQUIRED` — honest: no transcoding platform exists yet. THUMBNAIL variant records width/height/format from the decoded image. Malware scanning is an integration seam, not implemented — documented, not faked.

## Storage abstraction

`ObjectStorageClient` port: `createUploadUrl / createDeliveryUrl / objectExists / readObject / writeObject / delete`. `LocalObjectStorageClient` (filesystem, profile local/test/default) — path-traversal-safe key resolution, opaque `local://` URIs with expiry. Production adapter plugs the same port — no provider decision forced.

## Delivery

Signed URL only after authorization: owner always; others need PUBLIC + VERIFIED + COMPLETED/NOT_REQUIRED + not REJECTED/RESTRICTED. Non-viewable → NOT_FOUND (existence not leaked). No permanent URLs; TTL 1h (config).

## M02 integration

`MediaReferenceContract.isUsableBy(mediaId, owner)` — `PUT /profiles/me` now validates `avatarMediaId`: must exist, be owner-owned, VERIFIED, not REJECTED/RESTRICTED. M02 keeps profile semantics; M04 keeps the asset. Verified end-to-end in `MediaFlowIT.avatarReferenceValidationForM02`.

## Boundaries held

M18: moderation_state stored but M04 never writes decisions (no API mutates it). M10 evidence immutability: `isFrozen()` hook — VERIFIED+COMPLETED assets marked; replacement = new asset ID (the reference model makes silent mutation impossible: new upload ⇒ new objectKey ⇒ new row). No attachment entities created — M06/M07/M08/M10 own theirs.

## Audit

`MEDIA_ASSET_CREATED`, `MEDIA_UPLOAD_COMPLETED` (with resulting processing state). Signed URLs/keys never logged.

## Testing

`MediaServiceTest` 9 unit (MIME-type mismatch, oversize, key control, completion-verifies-object, variant persistence, idempotent complete, cross-user denial, private/public delivery) · `MediaFlowIT` 6 real-PG+filesystem (full journey incl. real PNG decode→variant, spoofed-image rejection, unuploaded-complete rejection, IDOR on all ops, M02 avatar wiring, document no-transcode). Suite: 91.

## Known follow-ups

- Real provider adapter (S3/GCS/R2) — port is ready; provider selection is an open decision.
- Video/audio/doc processing (transcode thumbnails, duration probe) — `MediaProcessor` seam.
- Malware scanning — hook inside `complete` before PROCESSING; vendor undecided.
- CDN delivery + cache headers when deployment lands (ADR-012).
- EXIF stripping — inside processor when a resize lib is adopted (documented, not claimed).
