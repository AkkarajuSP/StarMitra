package com.starmitra.modules.media.application;

import com.starmitra.modules.media.persistence.*;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * M04 — media asset lifecycle per ADR-008:
 *   metadata → backend-generated object key → pre-signed upload →
 *   client direct upload → complete (object verified) → process → delivery.
 *
 * 4D state per physical schema: upload / processing / moderation / visibility.
 * M04 never proxies binaries and never owns business attachments (M02/M06/
 * M07/M08/M10 reference media by ID).
 */
@Service
public class MediaService implements MediaReferenceContract {

    private static final Set<String> ALLOWED_TYPES = Set.of("IMAGE", "VIDEO", "AUDIO", "DOCUMENT");
    private static final java.util.Map<String, Set<String>> MIME_PREFIX = java.util.Map.of(
            "IMAGE", Set.of("image/"),
            "VIDEO", Set.of("video/"),
            "AUDIO", Set.of("audio/"),
            "DOCUMENT", Set.of("application/", "text/"));

    private final MediaAssetRepository assets;
    private final MediaVariantRepository variants;
    private final ObjectStorageClient storage;
    private final MediaProcessor processor;
    private final AuditService audit;
    private final long maxBytes;
    private final Duration uploadTtl;
    private final Duration deliveryTtl;
    private final com.starmitra.modules.moderation.application.ModerationContract moderation;

    public MediaService(MediaAssetRepository assets, MediaVariantRepository variants,
                        ObjectStorageClient storage, MediaProcessor processor, AuditService audit,
                        com.starmitra.modules.moderation.application.ModerationContract moderation,
                        @Value("${app.media.max-bytes:52428800}") long maxBytes,
                        @Value("${app.media.upload-ttl:PT15M}") Duration uploadTtl,
                        @Value("${app.media.delivery-ttl:PT1H}") Duration deliveryTtl) {
        this.assets = assets;
        this.variants = variants;
        this.storage = storage;
        this.processor = processor;
        this.audit = audit;
        this.moderation = moderation;
        this.maxBytes = maxBytes;
        this.uploadTtl = uploadTtl;
        this.deliveryTtl = deliveryTtl;
    }

    public record MediaView(UUID id, String objectKey, String mediaType, String uploadState,
                            String processingState, String moderationState, String visibility,
                            String checksum) {}
    public record UrlView(String url, String expiresAt, String method) {}
    public record CreateCommand(String mediaType, String mimeType, Long sizeBytes,
                                String filename, String visibility) {}

    // ---------- lifecycle ----------

    /** 1. metadata record — validates type/mime/size; backend mints object key. */
    @Transactional
    public MediaView create(UUID ownerId, CreateCommand cmd) {
        var type = parseType(cmd.mediaType());
        validateMime(type, cmd.mimeType());
        if (cmd.sizeBytes() != null && cmd.sizeBytes() > maxBytes) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "File exceeds maximum size");
        }
        var visibility = cmd.visibility() == null ? MediaAssetEntity.Visibility.PRIVATE
                : MediaAssetEntity.Visibility.valueOf(cmd.visibility());
        // backend-controlled key — client never chooses storage location
        String key = ownerId + "/" + UUID.randomUUID() + extension(cmd.filename());
        var asset = assets.save(new MediaAssetEntity(ownerId, key,
                sanitizeFilename(cmd.filename()), type, cmd.mimeType(), visibility));
        audit.record("M04", "MEDIA_ASSET_CREATED", ownerId, "user", "media_asset", asset.getId().toString(), null);
        return toView(asset);
    }

    /** 2. pre-signed upload URL — owner only, while INITIATED. */
    @Transactional(readOnly = true)
    public UrlView uploadUrl(UUID caller, UUID mediaId) {
        var asset = requireOwned(caller, mediaId);
        if (asset.getUploadState() != MediaAssetEntity.UploadState.INITIATED) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Upload already completed/failed");
        }
        var url = storage.createUploadUrl(asset.getObjectKey(), asset.getMimeType(), uploadTtl);
        return new UrlView(url.url(), url.expiresAt().toString(), url.method());
    }

    /**
     * 3. completion — owner only, requires INITIATED/UPLOADED, verifies the
     * object actually exists in storage, then runs processing. Idempotent:
     * completing an already-processing/completed asset returns its state.
     */
    @Transactional
    public MediaView complete(UUID caller, UUID mediaId, String checksum, Long sizeBytes) {
        var asset = requireOwned(caller, mediaId);
        if (asset.getUploadState() == MediaAssetEntity.UploadState.VERIFIED
                && asset.getProcessingState() != MediaAssetEntity.ProcessingState.PENDING) {
            return toView(asset);                                  // idempotent replay
        }
        if (!storage.objectExists(asset.getObjectKey())) {
            throw new ApiException(ErrorCode.MEDIA_NOT_READY, "Upload not present in storage");
        }
        asset.markUploaded(sizeBytes, checksum);
        asset.markVerified();
        asset.setProcessingState(MediaAssetEntity.ProcessingState.PROCESSING);

        var result = processor.process(asset, storage);
        asset.setProcessingState(result.state());
        for (var v : result.variants()) {
            variants.save(new MediaVariantEntity(asset.getId(), v.variantType(), v.objectKey(),
                    v.width(), v.height(), v.format(), v.sizeBytes()));
        }
        assets.saveAndFlush(asset);
        audit.record("M04", "MEDIA_UPLOAD_COMPLETED", caller, "user", "media_asset",
                mediaId.toString(), result.state().name());
        return toView(asset);
    }

    /** 4. status — owner sees own; others see only deliverable-visible assets. */
    @Transactional(readOnly = true)
    public MediaView status(UUID caller, UUID mediaId) {
        var asset = assets.findById(mediaId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!asset.getOwnerUserId().equals(caller) && !deliverableTo(asset)) {
            throw new ApiException(ErrorCode.NOT_FOUND);           // enumeration-safe
        }
        return toView(asset);
    }

    /** 5. signed delivery — visibility+moderation enforced before issuing URL. */
    @Transactional(readOnly = true)
    public UrlView deliveryUrl(UUID caller, UUID mediaId) {
        var asset = assets.findById(mediaId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        boolean own = asset.getOwnerUserId().equals(caller);
        if (!own && !deliverableTo(asset)) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        var url = storage.createDeliveryUrl(asset.getObjectKey(), deliveryTtl);
        return new UrlView(url.url(), url.expiresAt().toString(), url.method());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<UUID> ownerOf(UUID mediaId) {
        return assets.findById(mediaId).map(MediaAssetEntity::getOwnerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDeliverableTo(UUID mediaId, UUID callerUserId) {
        return assets.findById(mediaId)
                .map(a -> a.getOwnerUserId().equals(callerUserId) || deliverableTo(a))
                .orElse(false);
    }

    // ---------- M02 contract ----------

    @Override
    @Transactional(readOnly = true)
    public boolean isUsableBy(UUID mediaId, UUID ownerUserId) {
        return assets.findById(mediaId)
                .map(a -> a.getOwnerUserId().equals(ownerUserId)
                        && a.getUploadState() == MediaAssetEntity.UploadState.VERIFIED
                        && a.getModerationState() != MediaAssetEntity.ModerationState.REJECTED
                        && a.getModerationState() != MediaAssetEntity.ModerationState.RESTRICTED)
                .orElse(false);
    }

    // ---------- internals ----------

    private MediaAssetEntity requireOwned(UUID caller, UUID mediaId) {
        var asset = assets.findById(mediaId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!asset.getOwnerUserId().equals(caller)) {
            throw new ApiException(ErrorCode.NOT_FOUND);           // IDOR: don't reveal existence
        }
        return asset;
    }

    /**
     * Deliverable to non-owner: PUBLIC + processed + not REJECTED/RESTRICTED,
     * AND no active M18 restriction on the media itself or its owner.
     * M18 stays authoritative — M04 enforces, never decides.
     */
    private boolean deliverableTo(MediaAssetEntity a) {
        if (a.getVisibility() != MediaAssetEntity.Visibility.PUBLIC) return false;
        if (a.getModerationState() == MediaAssetEntity.ModerationState.REJECTED
                || a.getModerationState() == MediaAssetEntity.ModerationState.RESTRICTED) return false;
        if (moderation.isRestricted("MEDIA", a.getId())
                || moderation.isRestricted("USER", a.getOwnerUserId())) return false;
        return a.getUploadState() == MediaAssetEntity.UploadState.VERIFIED
                && (a.getProcessingState() == MediaAssetEntity.ProcessingState.COMPLETED
                    || a.getProcessingState() == MediaAssetEntity.ProcessingState.NOT_REQUIRED);
    }

    private MediaAssetEntity.MediaType parseType(String t) {
        try {
            var type = MediaAssetEntity.MediaType.valueOf(t);
            if (!ALLOWED_TYPES.contains(type.name())) throw new IllegalArgumentException();
            return type;
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unsupported media type");
        }
    }

    private void validateMime(MediaAssetEntity.MediaType type, String mime) {
        if (mime == null || MIME_PREFIX.get(type.name()).stream().noneMatch(mime::startsWith)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "MIME type inconsistent with media type");
        }
    }

    private String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        String ext = dot > 0 ? filename.substring(dot) : "";
        return ext.length() <= 10 && ext.matches("\\.[A-Za-z0-9]+") ? ext.toLowerCase() : "";
    }

    private String sanitizeFilename(String name) {
        if (name == null) return null;
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private MediaView toView(MediaAssetEntity a) {
        return new MediaView(a.getId(), a.getObjectKey(), a.getMediaType().name(),
                a.getUploadState().name(), a.getProcessingState().name(),
                a.getModerationState().name(), a.getVisibility().name(), a.getChecksum());
    }
}
