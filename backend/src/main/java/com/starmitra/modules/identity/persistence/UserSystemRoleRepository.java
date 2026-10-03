package com.starmitra.modules.identity.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSystemRoleRepository extends JpaRepository<UserSystemRoleEntity, UserSystemRoleEntity.Pk> {
    boolean existsByUserIdAndRoleId(java.util.UUID userId, java.util.UUID roleId);
}
