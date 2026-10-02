# StarMitra — Media Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Draft for review

## 1. Scope

`[FRS §10]` Upload of video, audio, image and supported document formats; metadata (title, description, skill/category, tags, visibility); lifecycle and moderation; engagement counters; ownership/attribution. Scalable storage and delivery `[FRS §36]`.

## 2. Upload & Processing Pipeline

`[Proposed]`

```text
Client                    Backend                    Object Storage        Workers
  │  1. POST /media/upload-init  │                          │                 │
  │ ───────────────────────────►│ create MediaAsset        │                 │
  │                             │ (status=Draft/Uploading) │                 │
  │  2. pre-signed URL          │                          │                 │
  │ ◄───────────────────────────│                          │                 │
  │  3. PUT binary ────────────────────────────────────────►│                 │
  │  4. POST /media/{id}/complete│                          │                 │
  │ ───────────────────────────►│ enqueue processing       │                 │
  │                             │ status=Processing        │                 │
  │                             │ ─────────────────────────────────────────►│
  │                             │                          │  validate type/ │
  │                             │                          │  size, virus    │
  │                             │                          │  scan, transcode│
  │                             │                          │  thumbnails,    │
  │                             │                          │  moderation hook│
  │                             │  MediaProcessed/         │                 │
  │                             │  MediaRejected ◄──────────────────────────│
  │                             │  status=Published/Hidden │                 │
```

Rationale: direct-to-storage upload keeps binary traffic off the API tier; async processing matches FRS status lifecycle (Draft → Processing → Published / Hidden / Rejected / Removed) `[FRS §10]`.

## 3. Storage Layout

`[Proposed]`

```text
media/
  originals/{assetId}.{ext}          # immutable source
  variants/{assetId}/{profile}.{ext} # transcoded renditions
  thumbnails/{assetId}/{n}.jpg
  documents/{assetId}.pdf            # normalized preview where needed
```

`MediaAsset` row = metadata + status + storage keys; `MediaVariant` rows = renditions. Binaries never in DB.

## 4. Delivery & Visibility Enforcement

`[FRS §9][§10]` — visibility enum: **Public / Followers / Collaboration-Only / Private**.

`[Proposed]` Public assets → CDN (long-lived, cacheable). Non-public assets → short-lived signed URLs issued by backend after visibility check (follower graph for Followers; room/project membership for Collaboration-Only; owner + admins for Private). Judge/submission media served under judge-assignment scoping `[FRS §19]`.

## 5. Format Strategy

`[Proposed]` MVP constraints (finalize in media design phase):

| Type | Accepted input | Normalized output |
|------|----------------|-------------------|
| Video | mp4/mov/webm — size/duration caps | HLS or progressive mp4 renditions |
| Audio | mp3/wav/m4a | aac/mp3 streaming + waveform |
| Image | jpg/png/webp | resized variants + thumbnail |
| Document | pdf, txt/docx preview `[Open]` | pdf preview or text extraction |

`[Open OD-6]` Provider choices: object store, CDN, transcode service (managed vs ffmpeg workers).

## 6. Integration Points

| Consumer | Usage |
|----------|-------|
| D2 Portfolio | PortfolioItem ↔ MediaAsset (skill-grouped) `[FRS §9.1]` |
| D7 Rooms | RoomAsset (scripts, lyrics, work files) `[FRS §13]`; versioned assets P1 `[FRS §35]` |
| D9 Submissions | SubmissionMedia; competition entries validate format/size/deadline `[FRS §16]` |
| D6 Messaging | MessageAttachment `[FRS §12]` |
| D14 Moderation | status transitions Hidden/Rejected/Removed `[FRS §26]` |

## 7. Risks & Controls

| Risk | Control |
|------|---------|
| Malicious/oversized uploads | Type + size validation at intake and processing; scanning `[FRS §26]` |
| Hotlinking private media | Signed URLs, no permanent public URL for non-public assets |
| Transcode cost/latency | Async queue, bounded rendition profiles, format caps `[Proposed]` |
| Duplicate uploads | Content-hash dedupe `[Proposed]` |
