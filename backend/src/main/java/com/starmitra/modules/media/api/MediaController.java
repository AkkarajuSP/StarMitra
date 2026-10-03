package com.starmitra.modules.media.api;

import com.starmitra.modules.media.application.MediaService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** /api/v1/media — ADR-008 direct-upload flow; binaries never cross the API. */
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaService media;

    public MediaController(MediaService media) {
        this.media = media;
    }

    @PostMapping
    public ResponseEntity<MediaDtos.MediaAsset> createMediaAsset(
            @Valid @RequestBody MediaDtos.MediaCreate body) {
        var cmd = new MediaService.CreateCommand(body.mediaType(), body.mimeType(),
                body.sizeBytes(), body.filename(), body.visibility());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(media.create(SecurityUtils.currentUserId(), cmd)));
    }

    @PostMapping("/{mediaId}/upload-url")
    public ResponseEntity<MediaDtos.UploadUrl> getUploadUrl(@PathVariable UUID mediaId) {
        var u = media.uploadUrl(SecurityUtils.currentUserId(), mediaId);
        return ResponseEntity.ok(new MediaDtos.UploadUrl(u.url(), u.expiresAt(), u.method()));
    }

    @PostMapping("/{mediaId}/complete")
    public ResponseEntity<MediaDtos.MediaAsset> completeMediaUpload(
            @PathVariable UUID mediaId,
            @Valid @RequestBody(required = false) MediaDtos.MediaComplete body) {
        var v = media.complete(SecurityUtils.currentUserId(), mediaId,
                body == null ? null : body.checksum(),
                body == null ? null : body.sizeBytes());
        return ResponseEntity.ok(toDto(v));
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaDtos.MediaAsset> getMediaStatus(@PathVariable UUID mediaId) {
        return ResponseEntity.ok(toDto(media.status(SecurityUtils.currentUserId(), mediaId)));
    }

    @GetMapping("/{mediaId}/delivery-url")
    public ResponseEntity<MediaDtos.DeliveryUrl> getDeliveryUrl(@PathVariable UUID mediaId) {
        var u = media.deliveryUrl(SecurityUtils.currentUserId(), mediaId);
        return ResponseEntity.ok(new MediaDtos.DeliveryUrl(u.url(), u.expiresAt(), "original"));
    }

    private MediaDtos.MediaAsset toDto(MediaService.MediaView v) {
        return new MediaDtos.MediaAsset(v.id(), v.objectKey(), v.mediaType(), v.uploadState(),
                v.processingState(), v.moderationState(), v.visibility(), v.checksum());
    }
}
