package com.starmitra.modules.judge.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface JudgeRepository extends JpaRepository<JudgeEntity, UUID> {
    Optional<JudgeEntity> findByUserId(UUID userId);
}
