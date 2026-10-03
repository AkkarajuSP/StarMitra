package com.starmitra.modules.social.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "engagement_counters")
@IdClass(EngagementCounterEntity.Pk.class)
public class EngagementCounterEntity {

    public static class Pk implements Serializable {
        private String targetType;
        private UUID targetId;
        public Pk() {}
        public Pk(String targetType, UUID targetId) {
            this.targetType = targetType;
            this.targetId = targetId;
        }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && targetType.equals(p.targetType) && targetId.equals(p.targetId);
        }
        @Override public int hashCode() { return Objects.hash(targetType, targetId); }
    }

    @Id
    @Column(name = "target_type")
    private String targetType;

    @Id
    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "follow_count", nullable = false)
    private long followCount = 0;

    @Column(name = "like_count", nullable = false)
    private long likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private long commentCount = 0;

    @Column(name = "refreshed_at", nullable = false)
    private OffsetDateTime refreshedAt = OffsetDateTime.now();

    protected EngagementCounterEntity() {}

    public EngagementCounterEntity(String targetType, UUID targetId) {
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public void bump(long likes, long comments, long follows) {
        this.likeCount += likes;
        this.commentCount += comments;
        this.followCount += follows;
        this.refreshedAt = OffsetDateTime.now();
    }

    public long getFollowCount() { return followCount; }
    public long getLikeCount() { return likeCount; }
    public long getCommentCount() { return commentCount; }
}
