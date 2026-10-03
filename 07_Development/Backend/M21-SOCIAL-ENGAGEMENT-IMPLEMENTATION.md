# M21 — Social Engagement — Implementation

**Slice:** sixth business implementation. **Status: complete.** M21 owns social signals; M05 consumes them.

## Ownership

`follows`, `likes`, `comments`, `engagement_counters` — nothing else. Signals are **derived** (counters rebuildable projection); never authoritative for business outcomes.

## Contract surface (9 ops, unchanged)

| Op | Path | Auth | Notes |
|---|---|---|---|
| followUser | POST /social/follows/{userId} | JWT sub = follower | 204 idempotent |
| unfollowUser | DELETE /social/follows/{userId} | follower | 204 idempotent |
| myFollows | GET /social/follows/me | self | FollowPage |
| likeTarget | POST /social/likes | liker | 201, idempotent (uq) |
| unlike | DELETE /social/likes/{likeId} | like owner | 204 |
| createComment | POST /social/comments | author | 201 |
| listComments | GET /social/comments?targetType&targetId | any auth | ACTIVE only |
| deleteComment | DELETE /social/comments/{commentId} | author | 204, soft-delete |
| getEngagement | GET /social/engagement?targetType&targetId | any auth | derived:true |

## Model behavior

- **Follows**: composite PK (follower,followee) = duplicate-proof; `ck_follows_not_self` + service validation → 422 on self-follow. Follow/unfollow both idempotent.
- **Likes**: `uq_likes_unique(user,target)` — repeat like returns same id; `unlike` owner-only (foreign → NOT_FOUND).
- **Comments**: create+delete only (DB-05 — no edit/reply columns exist); soft `status=REMOVED`; author-only (DB-06); body 1–4000.
- **Targets**: polymorphic, no FK by design — `MEDIA` validated via `MediaReferenceContract.isDeliverableTo` (added M04 contract method: owner or PUBLIC+processed+unrestricted); `PORTFOLIO` accepted unchecked — M08 doesn't exist yet (documented seam, schema designed for it).
- **Counters**: transactional `engagement_counters` upsert on like/comment (+/-); `getEngagement` reads the projection, `derived:true` per contract.

## M05 consumption (the one sanctioned touch)

`SocialSignalContract` (M21-owned): `followeeIdsOf`, `isFollowing`. M05 `feed` now applies a **deterministic followed-creator boost** over the fetched window (followed creator media first, then created_at DESC, id DESC) — provisional weight = "boost", not a product-final formula. IT-proven deterministic.

## Boundaries

No foreign repositories: M04 via `MediaReferenceContract` (new `isDeliverableTo`), M05/M02 via `SocialSignalContract`. M21 never: grants permissions, owns profiles/skills/media/moderation, feeds votes (M11 separate).

## Audit

`USER_FOLLOWED`, `USER_UNFOLLOWED`, `TARGET_LIKED`, `COMMENT_CREATED`.

## Testing

`SocialServiceTest` 8 unit (self-follow, idempotent follow, non-deliverable target, bad type, blank comment, foreign-delete→NOT_FOUND, soft-delete, zero counters) · `SocialFlowIT` 7 real-PG17 (follow lifecycle+idempotency, self-follow 422, like/counter/idempotent-like, foreign-unlike IDOR, comment lifecycle+soft-delete+counter, private-media like/comment denied, feed boost determinism). Suite: **113**.

## Known follow-ups

- M08 → real PORTFOLIO target validation (contract slot exists).
- M02 `FOLLOWERS`/`COLLABORATION_ONLY` profile+media visibility — `isFollowing` contract ready.
- M05 engagement-weighted feed ranking — counters exist; weights open product decision.
- Followers list endpoint (contract exposes only myFollows).
