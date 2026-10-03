package com.starmitra.integration;

import com.starmitra.modules.discovery.application.DiscoveryService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.media.application.MediaService;
import com.starmitra.modules.media.application.ObjectStorageClient;
import com.starmitra.modules.social.application.SocialService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M21 on real PG17 — follows, likes, comments, counters + M05 consumption. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SocialFlowIT {

    @Autowired OtpService otpService;
    @Autowired SocialService social;
    @Autowired MediaService mediaService;
    @Autowired ObjectStorageClient storage;
    @Autowired DiscoveryService discovery;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID me, other, creator;

    private UUID register(String tag) {
        String email = tag + UUID.randomUUID().toString().substring(0, 6) + "@t.dev";
        otpService.request("EMAIL", email);
        return otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        me = register("m21a");
        other = register("m21b");
        creator = register("m21c");
    }

    private MediaService.MediaView publicMedia(UUID owner) throws Exception {
        var a = mediaService.create(owner, new MediaService.CreateCommand(
                "IMAGE", "image/png", null, "art.png", "PUBLIC"));
        var img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        var baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        storage.writeObject(a.objectKey(), baos.toByteArray(), "image/png");
        mediaService.complete(owner, a.id(), null, null);
        return a;
    }

    @Test
    void followUnfollowIdempotent() {
        social.follow(me, other);
        social.follow(me, other);                       // repeat — still one row
        em.flush();
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from follows where follower_id=? and followee_id=?",
                Integer.class, me, other));
        var page = social.myFollows(me, null, 10);
        assertEquals(1, page.items().size());
        assertEquals(other, page.items().get(0).userId());

        social.unfollow(me, other);
        social.unfollow(me, other);                     // repeat — no-op
        em.flush();
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from follows where follower_id=?", Integer.class, me));
    }

    @Test
    void selfFollowRejected() {
        var e = assertThrows(ApiException.class, () -> social.follow(me, me));
        assertEquals(ErrorCode.VALIDATION_FAILED, e.code());
    }

    @Test
    void likeUnlikeWithCounters() throws Exception {
        var media = publicMedia(creator);
        var likeId = social.like(me, "MEDIA", media.id());
        var likeId2 = social.like(me, "MEDIA", media.id());    // idempotent → same id
        assertEquals(likeId, likeId2);

        var counts = social.engagement("MEDIA", media.id());
        assertEquals(1, counts.likeCount());

        social.unlike(me, likeId);
        assertEquals(0, social.engagement("MEDIA", media.id()).likeCount());
    }

    @Test
    void cannotUnlikeForeignLike() throws Exception {
        var media = publicMedia(creator);
        var likeId = social.like(creator, "MEDIA", media.id());
        assertThrows(ApiException.class, () -> social.unlike(me, likeId));   // NOT_FOUND — no leak
    }

    @Test
    void commentsLifecycleAuthorOnly() throws Exception {
        var media = publicMedia(creator);
        var c = social.comment(me, "MEDIA", media.id(), "great work");
        var c2 = social.comment(other, "MEDIA", media.id(), "nice");

        var page = social.listComments("MEDIA", media.id(), null, 10);
        assertEquals(2, page.items().size());

        // foreign author cannot delete
        assertThrows(ApiException.class, () -> social.deleteComment(me, c2.id()));

        social.deleteComment(other, c2.id());                    // author deletes — soft
        var after = social.listComments("MEDIA", media.id(), null, 10);
        assertEquals(1, after.items().size());
        em.flush();
        assertEquals("REMOVED", jdbc.queryForObject(
                "select status from comments where id=?", String.class, c2.id()));
        assertEquals(1, social.engagement("MEDIA", media.id()).commentCount());
    }

    @Test
    void privateMediaCannotBeLikedOrCommented() {
        var a = mediaService.create(creator, new MediaService.CreateCommand(
                "IMAGE", "image/png", null, "secret.png", "PRIVATE"));
        assertThrows(ApiException.class, () -> social.like(me, "MEDIA", a.id()));
        assertThrows(ApiException.class, () -> social.comment(me, "MEDIA", a.id(), "x"));
    }

    @Test
    void feedBoostsFollowedCreatorsDeterministically() throws Exception {
        var m1 = publicMedia(other);      // not followed
        var m2 = publicMedia(creator);    // will be followed
        social.follow(me, creator);

        var feed = discovery.feed(me, null, 10);
        assertEquals(m2.id(), feed.items().get(0).id());   // followed creator first

        var feed2 = discovery.feed(me, null, 10);
        assertEquals(feed.items().stream().map(i -> i.id()).toList(),
                feed2.items().stream().map(i -> i.id()).toList());   // deterministic
    }
}
