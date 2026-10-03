package com.starmitra.modules.identity.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_system_roles")
@IdClass(UserSystemRoleEntity.Pk.class)
public class UserSystemRoleEntity {

    public static class Pk implements Serializable {
        private UUID userId;
        private UUID roleId;
        public Pk() {}
        public Pk(UUID u, UUID r) { userId = u; roleId = r; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Pk p)) return false;
            return Objects.equals(userId, p.userId) && Objects.equals(roleId, p.roleId);
        }
        @Override public int hashCode() { return Objects.hash(userId, roleId); }
    }

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt = OffsetDateTime.now();

    protected UserSystemRoleEntity() {}

    public UserSystemRoleEntity(UUID userId, UUID roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }
}
