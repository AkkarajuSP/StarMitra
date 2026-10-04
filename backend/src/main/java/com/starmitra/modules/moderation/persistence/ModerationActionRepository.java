package com.starmitra.modules.moderation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ModerationActionRepository
        extends JpaRepository<ModerationActionEntity, UUID> {}
