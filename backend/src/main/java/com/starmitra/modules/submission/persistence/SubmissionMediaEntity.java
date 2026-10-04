package com.starmitra.modules.submission.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "submission_media")
@IdClass(SubmissionMediaEntity.Pk.class)
public class SubmissionMediaEntity {

    public static class Pk implements Serializable {
        private UUID submissionId;
        private UUID mediaId;
        public Pk() {}
        public Pk(UUID s, UUID m) { this.submissionId = s; this.mediaId = m; }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && submissionId.equals(p.submissionId) && mediaId.equals(p.mediaId);
        }
        @Override public int hashCode() { return Objects.hash(submissionId, mediaId); }
    }

    @Id
    @Column(name = "submission_id")
    private UUID submissionId;

    @Id
    @Column(name = "media_id")
    private UUID mediaId;                          // REF → media_assets (no FK)

    @Column(name = "sort_order")
    private Integer sortOrder;

    protected SubmissionMediaEntity() {}

    public SubmissionMediaEntity(UUID submissionId, UUID mediaId, Integer sortOrder) {
        this.submissionId = submissionId;
        this.mediaId = mediaId;
        this.sortOrder = sortOrder;
    }

    public UUID getMediaId() { return mediaId; }
}
