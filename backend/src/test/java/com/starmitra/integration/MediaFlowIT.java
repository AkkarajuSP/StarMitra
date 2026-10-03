package com.starmitra.integration;

import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.media.application.MediaService;
import com.starmitra.modules.media.application.ObjectStorageClient;
import com.starmitra.modules.profile.application.ProfileService;
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

/** M04 slice on real PG17 + filesystem storage — upload, process, deliver. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MediaFlowIT {

    @Autowired OtpService otpService;
    @Autowired MediaService mediaService;
    @Autowired ObjectStorageClient storage;
    @Autowired ProfileService profileService;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID me;

    @BeforeEach
    void register() {
        TestOtpSender.clear();
        String email = "m04+" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        otpService.request("EMAIL", email);
        me = otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
    }

    private byte[] realPng() throws Exception {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        var baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private MediaService.MediaView asset(String type, String mime, String visibility) {
        return mediaService.create(me, new MediaService.CreateCommand(
                type, mime, null, "file.bin", visibility));
    }

    @Test
    void fullUploadProcessDeliverJourney() throws Exception {
        var a = asset("IMAGE", "image/png", "PUBLIC");
        var up = mediaService.uploadUrl(me, a.id());
        assertNotNull(up.url());

        // simulate client direct-to-storage upload (backend never sees bytes over API)
        storage.writeObject(a.objectKey(), realPng(), "image/png");

        var done = mediaService.complete(me, a.id(), null, (long) realPng().length);
        assertEquals("VERIFIED", done.uploadState());
        assertEquals("COMPLETED", done.processingState());

        // thumbnail variant produced by real ImageIO decode
        em.flush();
        Integer variantCount = jdbc.queryForObject(
                "select count(*) from media_variants where media_asset_id=?", Integer.class, a.id());
        assertEquals(1, variantCount);

        var del = mediaService.deliveryUrl(UUID.randomUUID(), a.id());   // public + processed → others ok
        assertNotNull(del.url());
    }

    @Test
    void spoofedImageRejectedAtProcessing() {
        var a = asset("IMAGE", "image/png", "PRIVATE");
        storage.writeObject(a.objectKey(), "definitely not a png".getBytes(), "image/png");

        var done = mediaService.complete(me, a.id(), null, null);
        assertEquals("FAILED", done.processingState());       // content validation caught it
    }

    @Test
    void completionWithoutUploadRejected() {
        var a = asset("IMAGE", "image/jpeg", "PRIVATE");
        var e = assertThrows(ApiException.class, () -> mediaService.complete(me, a.id(), null, null));
        assertEquals(ErrorCode.MEDIA_NOT_READY, e.code());
    }

    @Test
    void crossUserCompletionAndPrivateDeliveryDenied() throws Exception {
        var a = asset("IMAGE", "image/png", "PRIVATE");
        var other = UUID.randomUUID();
        assertThrows(ApiException.class, () -> mediaService.complete(other, a.id(), null, null));
        assertThrows(ApiException.class, () -> mediaService.deliveryUrl(other, a.id()));
        assertThrows(ApiException.class, () -> mediaService.status(other, a.id()));
    }

    @Test
    void avatarReferenceValidationForM02() throws Exception {
        var a = asset("IMAGE", "image/png", "PRIVATE");
        storage.writeObject(a.objectKey(), realPng(), "image/png");
        mediaService.complete(me, a.id(), null, null);

        // usable asset → profile accepts it as avatar
        var p = profileService.updateMyProfile(me, "x",
                new ProfileService.UpdateCommand(null, null, null, a.id(), null), null);
        assertEquals(a.id(), p.avatarMediaId());

        // nonexistent media → rejected (contract, not FK)
        var e = assertThrows(ApiException.class, () -> profileService.updateMyProfile(me, "x",
                new ProfileService.UpdateCommand(null, null, null, UUID.randomUUID(), null), null));
        assertEquals(ErrorCode.VALIDATION_FAILED, e.code());
    }

    @Test
    void documentBypassesTranscoding() {
        var a = asset("DOCUMENT", "application/pdf", "PUBLIC");
        storage.writeObject(a.objectKey(), "pdf-bytes".getBytes(), "application/pdf");
        var done = mediaService.complete(me, a.id(), null, null);
        assertEquals("NOT_REQUIRED", done.processingState());   // honest — no fake processing
    }
}
