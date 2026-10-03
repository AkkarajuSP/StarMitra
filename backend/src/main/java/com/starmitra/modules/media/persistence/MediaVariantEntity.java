package com.starmitra.modules.media.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_variants")
public class MediaVariantEntity {

    @Id
    private UUID id;

    @Column(name = "media_asset_id", nullable = false)
    private UUID mediaAssetId;

    @Column(name = "variant_type", nullable = false, length = 40)
    private String variantType;                  // THUMBNAIL / PREVIEW / etc (processor-defined)

    @Column(name = "object_key", nullable = false, unique = true, length = 512)
    private String objectKey;

    @Column private Integer width;
    @Column private Integer height;
    @Column(length = 20) private String format;
    @Column(name = "size_bytes") private Long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected MediaVariantEntity() {}

    public MediaVariantEntity(UUID mediaAssetId, String variantType, String objectKey,
                              Integer width, Integer height, String format, Long sizeBytes) {
        this.id = UUID.randomUUID();
        this.mediaAssetId = mediaAssetId;
        this.variantType = variantType;
        this.objectKey = objectKey;
        this.width = width;
        this.height = height;
        this.format = format;
        this.sizeBytes = sizeBytes;
    }

    public UUID getId() { return id; }
    public UUID getMediaAssetId() { return mediaAssetId; }
    public String getVariantType() { return variantType; }
    public String getObjectKey() { return objectKey; }
}
