package com.starmitra.modules.social.api;

import com.starmitra.modules.social.application.SocialService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** /api/v1/social — M21 social engagement; actor always JWT sub. */
@RestController
@RequestMapping("/api/v1/social")
public class SocialController {

    private final SocialService social;

    public SocialController(SocialService social) {
        this.social = social;
    }

    @PostMapping("/follows/{userId}")
    public ResponseEntity<Void> followUser(@PathVariable UUID userId) {
        social.follow(SecurityUtils.currentUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/follows/{userId}")
    public ResponseEntity<Void> unfollowUser(@PathVariable UUID userId) {
        social.unfollow(SecurityUtils.currentUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/follows/me")
    public ResponseEntity<SocialDtos.FollowPage> myFollows(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        var p = social.myFollows(SecurityUtils.currentUserId(), cursor, limit);
        return ResponseEntity.ok(new SocialDtos.FollowPage(
                p.items().stream().map(f -> new SocialDtos.FollowItem(f.userId(), f.followedAt())).toList(),
                new SocialDtos.PageMeta(p.page().nextCursor(), p.page().hasMore(), p.page().total())));
    }

    @PostMapping("/likes")
    public ResponseEntity<Void> likeTarget(@Valid @RequestBody SocialDtos.LikeCreate body) {
        social.like(SecurityUtils.currentUserId(), body.targetType(), body.targetId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/likes/{likeId}")
    public ResponseEntity<Void> unlike(@PathVariable UUID likeId) {
        social.unlike(SecurityUtils.currentUserId(), likeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/comments")
    public ResponseEntity<SocialDtos.Comment> createComment(@Valid @RequestBody SocialDtos.CommentCreate body) {
        var c = social.comment(SecurityUtils.currentUserId(), body.targetType(), body.targetId(), body.body());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SocialDtos.Comment(c.id(), c.authorId(), c.body(), c.createdAt()));
    }

    @GetMapping("/comments")
    public ResponseEntity<SocialDtos.CommentPage> listComments(
            @RequestParam String targetType,
            @RequestParam UUID targetId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        var p = social.listComments(targetType, targetId, cursor, limit);
        return ResponseEntity.ok(new SocialDtos.CommentPage(
                p.items().stream().map(c -> new SocialDtos.Comment(c.id(), c.authorId(), c.body(), c.createdAt())).toList(),
                new SocialDtos.PageMeta(p.page().nextCursor(), p.page().hasMore(), p.page().total())));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {
        social.deleteComment(SecurityUtils.currentUserId(), commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/engagement")
    public ResponseEntity<SocialDtos.EngagementCounts> getEngagement(
            @RequestParam String targetType, @RequestParam UUID targetId) {
        var c = social.engagement(targetType, targetId);
        return ResponseEntity.ok(new SocialDtos.EngagementCounts(
                c.followCount(), c.likeCount(), c.commentCount(), true));
    }
}
