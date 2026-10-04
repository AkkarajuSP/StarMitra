package com.starmitra.modules.room.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProjectAssetRepository extends JpaRepository<ProjectAssetEntity, UUID> {
}
