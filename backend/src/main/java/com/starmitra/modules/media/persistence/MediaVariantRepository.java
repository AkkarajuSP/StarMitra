package com.starmitra.modules.media.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MediaVariantRepository extends JpaRepository<MediaVariantEntity, UUID> {
    List<MediaVariantEntity> findByMediaAssetId(UUID mediaAssetId);
}
