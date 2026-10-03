package com.starmitra.modules.social.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "follows")
@IdClass(FollowEntity.Pk.class)
public class FollowEntity {

    public static class Pk implements Serializable {
        private UUID followerId;
        private UUID followeeId;
        public Pk() {}
        public Pk(UUID followerId, UUID followeeId) {
            this.followerId = followerId;
            this.followeeId = followeeId;
        }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && followerId.equals(p.followerId) && followeeId.equals(p.followeeId);
        }
        @Override public int hashCode() { return Objects.hash(followerId, followeeId); }
    }

    @Id
    @Column(name = "follower_id")
    private UUID followerId;

    @Id
    @Column(name = "followee_id")
    private UUID followeeId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected FollowEntity() {}

    public FollowEntity(UUID followerId, UUID followeeId) {
        this.followerId = followerId;
        this.followeeId = followeeId;
    }

    public UUID getFollowerId() { return followerId; }
    public UUID getFolloweeId() { return followeeId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
