package com.starmitra.modules.identity.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Query("select t from RefreshTokenEntity t where t.familyId = :familyId and t.revokedAt is null")
    List<RefreshTokenEntity> findActiveByFamilyId(UUID familyId);

    @Query("select t from RefreshTokenEntity t where t.userId = :userId and t.revokedAt is null")
    List<RefreshTokenEntity> findActiveByUserId(UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("update RefreshTokenEntity t set t.revokedAt = CURRENT_TIMESTAMP where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(UUID familyId);

    @Modifying(clearAutomatically = true)
    @Query("update RefreshTokenEntity t set t.revokedAt = CURRENT_TIMESTAMP where t.userId = :userId and t.revokedAt is null")
    int revokeAllForUser(UUID userId);
}
