package com.starmitra.modules.social.application;

import com.starmitra.modules.media.application.MediaReferenceContract;
import com.starmitra.modules.portfolio.application.PortfolioTargetContract;
import com.starmitra.modules.social.persistence.*;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.pagination.Cursor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * M21 — Social Engagement. Owns follows, likes, comments, engagement_counters.
 *
 * Signals are DERIVED — never authoritative for business outcomes
 * (engagement_counters is a rebuildable projection per V1.17).
 * Targets are polymorphic typed refs (no FK by design):
 *   MEDIA     → validated via M04 MediaReferenceContract.isDeliverableTo
 *   PORTFOLIO → accepted unchecked (M08 not implemented — documented seam)
 * Comments: create+delete only (DB-05), soft-deleted, author-only (DB-06).
 */
@Service
public class SocialService implements SocialSignalContract {

    private final FollowRepository follows;
    private final LikeRepository likes;
    private final CommentRepository comments;
    private final EngagementCounterRepository counters;
    private final MediaReferenceContract media;
    private final PortfolioTargetContract portfolioTargets;
    private final AuditService audit;

    public SocialService(FollowRepository follows, LikeRepository likes, CommentRepository comments,
                         EngagementCounterRepository counters, MediaReferenceContract media,
                         PortfolioTargetContract portfolioTargets, AuditService audit) {
        this.follows = follows;
        this.likes = likes;
        this.comments = comments;
        this.counters = counters;
        this.media = media;
        this.portfolioTargets = portfolioTargets;
        this.audit = audit;
    }

    public record FollowView(UUID userId, String followedAt) {}
    public record CommentView(UUID id, UUID authorId, String body, String createdAt) {}
    public record Counts(long followCount, long likeCount, long commentCount) {}
    public record Page(String nextCursor, boolean hasMore, Integer total) {}
    public record FollowPage(List<FollowView> items, Page page) {}
    public record CommentPage(List<CommentView> items, Page page) {}

    // ---------- follows ----------

    /** Idempotent follow — composite PK is the duplicate guard; self-follow → 422. */
    @Transactional
    public void follow(UUID follower, UUID followee) {
        if (follower.equals(followee)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Cannot follow yourself");
        }
        if (!follows.existsById(new FollowEntity.Pk(follower, followee))) {
            try {
                follows.saveAndFlush(new FollowEntity(follower, followee));
                audit.record("M21", "USER_FOLLOWED", follower, "user", "user", followee.toString(), null);
            } catch (DataIntegrityViolationException dup) {
                // concurrent insert — already following, still 204
            }
        }
    }

    @Transactional
    public void unfollow(UUID follower, UUID followee) {
        var key = new FollowEntity.Pk(follower, followee);
        if (follows.existsById(key)) {
            follows.deleteById(key);
            audit.record("M21", "USER_UNFOLLOWED", follower, "user", "user", followee.toString(), null);
        }
    }

    @Transactional(readOnly = true)
    public FollowPage myFollows(UUID me, String cursor, Integer limit) {
        int size = Cursor.limit(limit);
        int offset = offset(cursor);
        var rows = follows.findByFollowerIdOrderByCreatedAtDesc(me, PageRequest.of(0, size + 1 + offset));
        var window = rows.size() > offset ? rows.subList(offset, rows.size()) : List.<FollowEntity>of();
        boolean hasMore = window.size() > size;
        var items = window.stream().limit(size)
                .map(f -> new FollowView(f.getFolloweeId(), f.getCreatedAt().toString())).toList();
        return new FollowPage(items,
                new Page(hasMore ? Cursor.encode("o", String.valueOf(offset + size)) : null, hasMore, null));
    }

    // ---------- likes ----------

    /** Idempotent like — uq_likes_unique guards; returns like id (new or existing). */
    @Transactional
    public UUID like(UUID user, String targetType, UUID targetId) {
        requireDeliverableTarget(user, targetType, targetId);
        var existing = likes.findByUserIdAndTargetTypeAndTargetId(user, targetType, targetId);
        if (existing.isPresent()) return existing.get().getId();
        try {
            var like = likes.saveAndFlush(new LikeEntity(user, targetType, targetId));
            bumpCounter(targetType, targetId, 1, 0, 0);
            audit.record("M21", "TARGET_LIKED", user, "user", targetType.toLowerCase(), targetId.toString(), null);
            return like.getId();
        } catch (DataIntegrityViolationException dup) {
            return likes.findByUserIdAndTargetTypeAndTargetId(user, targetType, targetId)
                    .map(LikeEntity::getId).orElseThrow();
        }
    }

    /** Unlike by like id — owner only; foreign id → NOT_FOUND (IDOR-safe). */
    @Transactional
    public void unlike(UUID user, UUID likeId) {
        var like = likes.findById(likeId)
                .filter(l -> l.getUserId().equals(user))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        likes.delete(like);
        bumpCounter(like.getTargetType(), like.getTargetId(), -1, 0, 0);
    }

    // ---------- comments ----------

    @Transactional
    public CommentView comment(UUID author, String targetType, UUID targetId, String body) {
        if (body == null || body.isBlank() || body.length() > 4000) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Comment body invalid");
        }
        requireDeliverableTarget(author, targetType, targetId);
        var c = comments.save(new CommentEntity(author, targetType, targetId, body.trim()));
        bumpCounter(targetType, targetId, 0, 1, 0);
        audit.record("M21", "COMMENT_CREATED", author, "user", targetType.toLowerCase(), targetId.toString(), null);
        return toView(c);
    }

    @Transactional(readOnly = true)
    public CommentPage listComments(String targetType, UUID targetId, String cursor, Integer limit) {
        int size = Cursor.limit(limit);
        int offset = offset(cursor);
        var rows = comments.findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtAsc(
                targetType, targetId, CommentEntity.Status.ACTIVE, PageRequest.of(0, size + 1 + offset));
        var window = rows.size() > offset ? rows.subList(offset, rows.size()) : List.<CommentEntity>of();
        boolean hasMore = window.size() > size;
        var items = window.stream().limit(size).map(this::toView).toList();
        return new CommentPage(items,
                new Page(hasMore ? Cursor.encode("o", String.valueOf(offset + size)) : null, hasMore, null));
    }

    /** Soft delete (DB-05) — author only (DB-06); foreign comment → NOT_FOUND. */
    @Transactional
    public void deleteComment(UUID caller, UUID commentId) {
        var c = comments.findById(commentId)
                .filter(x -> x.getAuthorId().equals(caller))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (c.getStatus() == CommentEntity.Status.ACTIVE) {
            c.softDelete();
            comments.save(c);
            bumpCounter(c.getTargetType(), c.getTargetId(), 0, -1, 0);
        }
    }

    // ---------- engagement counters (derived) ----------

    @Transactional(readOnly = true)
    public Counts engagement(String targetType, UUID targetId) {
        return counters.findById(new EngagementCounterEntity.Pk(targetType, targetId))
                .map(c -> new Counts(c.getFollowCount(), c.getLikeCount(), c.getCommentCount()))
                .orElse(new Counts(0, 0, 0));
    }

    // ---------- M05/M02 signal contract ----------

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> followeeIdsOf(UUID followerId) {
        return follows.findFolloweeIds(followerId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID followeeId) {
        return follows.existsById(new FollowEntity.Pk(followerId, followeeId));
    }

    // ---------- internals ----------

    /** MEDIA must be deliverable to the caller (M04 rule); PORTFOLIO unchecked — M08 not implemented. */
    private void requireDeliverableTarget(UUID caller, String targetType, UUID targetId) {
        switch (targetType) {
            case "MEDIA" -> {
                if (!media.isDeliverableTo(targetId, caller)) {
                    throw new ApiException(ErrorCode.NOT_FOUND, "Target not found or not accessible");
                }
            }
            case "PORTFOLIO" -> {
                if (!portfolioTargets.isEngageableItem(targetId, caller)) {
                    throw new ApiException(ErrorCode.NOT_FOUND, "Target not found or not accessible");
                }
            }
            default -> throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown target type");
        }
    }

    private void bumpCounter(String targetType, UUID targetId, long likes, long comments, long follows) {
        var key = new EngagementCounterEntity.Pk(targetType, targetId);
        var c = counters.findById(key).orElseGet(() -> new EngagementCounterEntity(targetType, targetId));
        c.bump(likes, comments, follows);
        counters.save(c);
    }

    private CommentView toView(CommentEntity c) {
        return new CommentView(c.getId(), c.getAuthorId(), c.getBody(), c.getCreatedAt().toString());
    }

    private int offset(String cursor) {
        if (cursor == null) return 0;
        String[] p = Cursor.decode(cursor);
        return p.length == 2 ? Integer.parseInt(p[1]) : 0;
    }
}
