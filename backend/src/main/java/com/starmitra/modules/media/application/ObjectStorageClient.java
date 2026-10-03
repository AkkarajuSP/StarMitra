package com.starmitra.modules.media.application;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Provider-neutral object-storage contract (ADR-008).
 * Implemented by an adapter (S3/GCS/R2/local) — never exposed via API.
 * Clients upload/download directly via the URLs this produces; Spring Boot
 * never proxies media bytes.
 */
public interface ObjectStorageClient {

    /** Pre-signed PUT URL for direct upload — backend controls the key. */
    PresignedUrl createUploadUrl(String objectKey, String contentType, Duration ttl);

    /** Signed GET/delivery URL honoring expiry. */
    PresignedUrl createDeliveryUrl(String objectKey, Duration ttl);

    /** Does the object exist? Authoritative completion verification. */
    boolean objectExists(String objectKey);

    /** Object bytes for trusted server-side processing only. */
    Optional<byte[]> readObject(String objectKey);

    /** Write bytes (processing output — variants). Server-side only. */
    void writeObject(String objectKey, byte[] bytes, String contentType);

    void delete(String objectKey);

    record PresignedUrl(String url, String method, OffsetDateTime expiresAt) {}
}
