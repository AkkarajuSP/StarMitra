package com.starmitra.modules.media.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Local/dev storage adapter — filesystem-backed provider-neutral impl for
 * local development and ITs (no cloud credentials). "Signed" URLs are
 * opaque local URIs; the expiry/signature contract is preserved.
 * Production adapters (S3/GCS/R2) implement the same port.
 */
@Component
@Profile({"local", "test", "default"})
public class LocalObjectStorageClient implements ObjectStorageClient {

    private final Path root;

    public LocalObjectStorageClient(@Value("${app.media.store-dir:./media-store}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    @Override
    public PresignedUrl createUploadUrl(String objectKey, String contentType, Duration ttl) {
        return new PresignedUrl("local://media-store/upload/" + objectKey
                + "?token=" + UUID.randomUUID(), "PUT", OffsetDateTime.now().plus(ttl));
    }

    @Override
    public PresignedUrl createDeliveryUrl(String objectKey, Duration ttl) {
        return new PresignedUrl("local://media-store/deliver/" + objectKey
                + "?exp=" + OffsetDateTime.now().plus(ttl).toEpochSecond(),
                "GET", OffsetDateTime.now().plus(ttl));
    }

    @Override
    public boolean objectExists(String objectKey) {
        return Files.isRegularFile(resolve(objectKey));
    }

    @Override
    public Optional<byte[]> readObject(String objectKey) {
        try {
            return objectExists(objectKey) ? Optional.of(Files.readAllBytes(resolve(objectKey))) : Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeObject(String objectKey, byte[] bytes, String contentType) {
        try {
            Path p = resolve(objectKey);
            Files.createDirectories(p.getParent());
            Files.write(p, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Confine keys to the store root — path traversal rejected. */
    private Path resolve(String objectKey) {
        Path p = root.resolve(objectKey).normalize();
        if (!p.startsWith(root)) throw new IllegalArgumentException("key escapes store root");
        return p;
    }
}
