package com.starmitra.modules.progression.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProgressionOverrideRepository extends JpaRepository<ProgressionOverrideEntity, UUID> {}
