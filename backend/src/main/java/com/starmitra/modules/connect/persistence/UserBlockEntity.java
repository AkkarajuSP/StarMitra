package com.starmitra.modules.connect.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_blocks")
@IdClass(UserBlockEntity.Pk.class)
public class UserBlockEntity {

    public static class Pk implements Serializable {
        private UUID blockerId;
        private UUID blockedId;
        public Pk() {}
        public Pk(UUID a, UUID b) { this.blockerId = a; this.blockedId = b; }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && blockerId.equals(p.blockerId) && blockedId.equals(p.blockedId);
        }
        @Override public int hashCode() { return Objects.hash(blockerId, blockedId); }
    }

    @Id
    @Column(name = "blocker_id")
    private UUID blockerId;

    @Id
    @Column(name = "blocked_id")
    private UUID blockedId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected UserBlockEntity() {}

    public UserBlockEntity(UUID blockerId, UUID blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
    }
}
