package com.starmitra.modules.voting.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "votes")
public class VoteEntity {

    public enum TargetType { INDIVIDUAL, PROJECT }  // one PROJECT vote = one target

    @Id
    private UUID id;

    @Column(name = "voter_id", nullable = false)
    private UUID voterId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;                    // REF → competitions

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;                       // REF → competition_categories

    @Column(name = "round_id", nullable = false)
    private UUID roundId;                          // REF → competition_rounds

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10)
    private TargetType targetType;

    @Column(name = "cast_at", nullable = false)
    private OffsetDateTime castAt = OffsetDateTime.now();

    @Column(name = "client_msg_id", length = 128)
    private String clientMsgId;

    protected VoteEntity() {}

    public VoteEntity(UUID voterId, UUID submissionId, UUID competitionId, UUID categoryId,
                      UUID roundId, TargetType targetType, String clientMsgId) {
        this.id = UUID.randomUUID();
        this.voterId = voterId;
        this.submissionId = submissionId;
        this.competitionId = competitionId;
        this.categoryId = categoryId;
        this.roundId = roundId;
        this.targetType = targetType;
        this.clientMsgId = clientMsgId;
    }

    public UUID getId() { return id; }
    public UUID getSubmissionId() { return submissionId; }
    public UUID getRoundId() { return roundId; }
    public TargetType getTargetType() { return targetType; }
    public OffsetDateTime getCastAt() { return castAt; }
}
