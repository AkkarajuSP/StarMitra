package com.starmitra.modules.media.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public final class MediaDtos {

    private MediaDtos() {}

    public record MediaCreate(
            @NotBlank @Pattern(regexp = "IMAGE|VIDEO|AUDIO|DOCUMENT") String mediaType,
            @NotBlank String mimeType,
            @Positive Long sizeBytes,
            String filename,
            @Pattern(regexp = "PUBLIC|FOLLOWERS|COLLABORATION_ONLY|PRIVATE") String visibility) {}

    public record MediaAsset(UUID id, String objectKey, String mediaType, String uploadState,
                             String processingState, String moderationState, String visibility,
                             String checksum) {}

    public record MediaComplete(String checksum, Long sizeBytes) {}

    public record UploadUrl(String url, String expiresAt, String method) {}

    public record DeliveryUrl(String url, String expiresAt, String variant) {}
}
