package com.starmitra.modules.social.application;

import com.starmitra.modules.media.application.MediaReferenceContract;
import com.starmitra.modules.social.persistence.*;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocialServiceTest {

    private FollowRepository follows;
    private LikeRepository likes;
    private CommentRepository comments;
    private EngagementCounterRepository counters;
    private SocialService service;
    private final UUID me = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        follows = mock(FollowRepository.class);
        likes = mock(LikeRepository.class);
        comments = mock(CommentRepository.class);
        counters = mock(EngagementCounterRepository.class);
        service = new SocialService(follows, likes, comments, counters,
                mediaRef(true), (itemId, caller) -> true, mock(AuditService.class));
    }

    @Test
    void selfFollowRejected() {
        var e = assertThrows(ApiException.class, () -> service.follow(me, me));
        assertEquals(ErrorCode.VALIDATION_FAILED, e.code());
        verify(follows, never()).save(any());
    }

    @Test
    void followIsIdempotent() {
        var other = UUID.randomUUID();
        when(follows.existsById(new FollowEntity.Pk(me, other))).thenReturn(true);
        service.follow(me, other);
        verify(follows, never()).saveAndFlush(any());
    }

    private MediaReferenceContract mediaRef(boolean deliverable) {
        return new MediaReferenceContract() {
            public boolean isUsableBy(UUID m, UUID o) { return true; }
            public boolean isDeliverableTo(UUID m, UUID c) { return deliverable; }
        };
    }

    @Test
    void likeOnNonDeliverableMediaRejected() {
        var svc = new SocialService(follows, likes, comments, counters,
                mediaRef(false), (itemId, caller) -> true, mock(AuditService.class));
        var e = assertThrows(ApiException.class,
                () -> svc.like(me, "MEDIA", UUID.randomUUID()));
        assertEquals(ErrorCode.NOT_FOUND, e.code());
    }

    @Test
    void invalidTargetTypeRejected() {
        assertThrows(ApiException.class,
                () -> service.like(me, "USER", UUID.randomUUID()));
    }

    @Test
    void blankCommentRejected() {
        assertThrows(ApiException.class,
                () -> service.comment(me, "MEDIA", UUID.randomUUID(), "   "));
    }

    @Test
    void foreignCommentDeleteIsNotFound() {
        var c = new CommentEntity(UUID.randomUUID(), "MEDIA", UUID.randomUUID(), "hi");
        when(comments.findById(c.getId())).thenReturn(Optional.of(c));
        assertThrows(ApiException.class, () -> service.deleteComment(me, c.getId()));
        assertEquals(CommentEntity.Status.ACTIVE, c.getStatus());   // untouched
    }

    @Test
    void authorSoftDeletesNotPhysically() {
        var c = new CommentEntity(me, "MEDIA", UUID.randomUUID(), "mine");
        when(comments.findById(c.getId())).thenReturn(Optional.of(c));
        service.deleteComment(me, c.getId());
        assertEquals(CommentEntity.Status.REMOVED, c.getStatus());
        verify(comments).save(c);
        verify(comments, never()).delete(any());
    }

    @Test
    void engagementDefaultsToZero() {
        var c = service.engagement("MEDIA", UUID.randomUUID());
        assertEquals(0, c.likeCount());
        assertTrue(c.commentCount() == 0);
    }
}
