package com.starmitra.modules.media.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
public class MediaAssetEntity {

    public enum MediaType { IMAGE, VIDEO, AUDIO, DOCUMENT }
    public enum UploadState { INITIATED, UPLOADED, VERIFIED, FAILED }
    public enum ProcessingState { PENDING, PROCESSING, COMPLETED, FAILED, NOT_REQUIRED }
    public enum ModerationState { PENDING, APPROVED, REJECTED, RESTRICTED }
    public enum Visibility { PUBLIC, FOLLOWERS, COLLABORATION_ONLY, PRIVATE }

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;                    // REF → users (cross-module, no FK)

    @Column(name = "object_key", nullable = false, unique = true, length = 512)
    private String objectKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;

    @Column(name = "mime_type", nullable = false, length = 127)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_state", nullable = false, length = 20)
    private UploadState uploadState = UploadState.INITIATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_state", nullable = false, length = 20)
    private ProcessingState processingState = ProcessingState.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_state", nullable = false, length = 20)
    private ModerationState moderationState = ModerationState.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected MediaAssetEntity() {}

    public MediaAssetEntity(UUID ownerUserId, String objectKey, String originalFilename,
                            MediaType mediaType, String mimeType, Visibility visibility) {
        this.id = UUID.randomUUID();
        this.ownerUserId = ownerUserId;
        this.objectKey = objectKey;
        this.originalFilename = originalFilename;
        this.mediaType = mediaType;
        this.mimeType = mimeType;
        this.visibility = visibility;
    }

    public void markUploaded(Long sizeBytes, String checksum) {
        this.uploadState = UploadState.UPLOADED;
        if (sizeBytes != null) this.sizeBytes = sizeBytes;
        if (checksum != null) this.checksum = checksum;
        touch();
    }

    public void markVerified() { this.uploadState = UploadState.VERIFIED; touch(); }
    public void markUploadFailed() { this.uploadState = UploadState.FAILED; touch(); }
    public void setProcessingState(ProcessingState s) { this.processingState = s; touch(); }

    /** Immutable-evidence flag hook for M10 — once finalized, replacement = new asset. */
    public boolean isFrozen() {
        return uploadState == UploadState.VERIFIED && processingState == ProcessingState.COMPLETED;
    }

    private void touch() { this.updatedAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public String getObjectKey() { return objectKey; }
    public String getOriginalFilename() { return originalFilename; }
    public MediaType getMediaType() { return mediaType; }
    public String getMimeType() { return mimeType; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getChecksum() { return checksum; }
    public UploadState getUploadState() { return uploadState; }
    public ProcessingState getProcessingState() { return processingState; }
    public ModerationState getModerationState() { return moderationState; }
    public Visibility getVisibility() { return visibility; }
}
