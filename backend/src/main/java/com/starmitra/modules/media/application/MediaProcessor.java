package com.starmitra.modules.media.application;

import com.starmitra.modules.media.persistence.MediaAssetEntity;

import java.util.List;

/**
 * MVP processing abstraction — durable in-process processor (no Kafka/
 * workers per ADRs). Business state lives in media_assets/media_variants.
 * Swap for an external processor later without touching API or schema.
 */
public interface MediaProcessor {

    /** Validate content + produce variants. Returns resulting state + variants. */
    Result process(MediaAssetEntity asset, ObjectStorageClient storage);

    record Result(MediaAssetEntity.ProcessingState state, List<VariantSpec> variants) {}
    record VariantSpec(String variantType, String objectKey, Integer width, Integer height,
                       String format, Long sizeBytes) {}
}
