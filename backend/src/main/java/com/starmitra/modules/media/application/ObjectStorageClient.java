package com.starmitra.modules.media.application;

import java.time.Duration;
import java.util.UUID;

/**
 * Provider-neutral object-storage contract (ADR-008).
 * Implemented by an adapter (S3/GCS/R2...) — never exposed directly via API.
 * Clients upload/download directly via the URLs this produces; Spring Boot
 * never proxies media bytes.
 */
public interface ObjectStorageClient {

    /** Pre-signed PUT URL for direct upload. */
    PresignedUrl createUploadUrl(String objectKey, String contentType, Duration ttl);

    /** Signed GET/delivery URL honoring expiry. */
    PresignedUrl createDeliveryUrl(String objectKey, Duration ttl);

    void delete(String objectKey);

    record PresignedUrl(String url, String method, java.time.OffsetDateTime expiresAt) {}
}
