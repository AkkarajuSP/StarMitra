package com.starmitra.modules.identity.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByPhone(String phone);

    @Query("select r.name from SystemRoleEntity r join UserSystemRoleEntity ur " +
           "on ur.roleId = r.id where ur.userId = :userId")
    List<String> findRoleNamesByUserId(UUID userId);
}
