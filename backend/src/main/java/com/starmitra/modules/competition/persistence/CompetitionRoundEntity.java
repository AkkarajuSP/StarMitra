package com.starmitra.modules.competition.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "competition_rounds")
public class CompetitionRoundEntity {

    @Id
    private UUID id;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(nullable = false)
    private int sequence;

    @Column(length = 120)
    private String name;

    @Column(name = "start_at")
    private OffsetDateTime startAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;

    @Column(name = "vote_config_id")
    private UUID voteConfigId;                     // REF → vote_configs (M11)

    @Column(name = "rubric_version_id")
    private UUID rubricVersionId;                  // REF → evaluation_template_versions (M13)

    @Column(name = "progression_config_id")
    private UUID progressionConfigId;              // REF → progression_configurations (M15)

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected CompetitionRoundEntity() {}

    public CompetitionRoundEntity(UUID competitionId, int sequence, String name,
                                  OffsetDateTime startAt, OffsetDateTime endAt,
                                  UUID voteConfigId, UUID rubricVersionId,
                                  UUID progressionConfigId) {
        this.id = UUID.randomUUID();
        this.competitionId = competitionId;
        this.sequence = sequence;
        this.name = name;
        this.startAt = startAt;
        this.endAt = endAt;
        this.voteConfigId = voteConfigId;
        this.rubricVersionId = rubricVersionId;
        this.progressionConfigId = progressionConfigId;
    }

    public UUID getId() { return id; }
    public UUID getCompetitionId() { return competitionId; }
    public int getSequence() { return sequence; }
    public String getName() { return name; }
    public OffsetDateTime getStartAt() { return startAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public UUID getVoteConfigId() { return voteConfigId; }
    public UUID getRubricVersionId() { return rubricVersionId; }
    public UUID getProgressionConfigId() { return progressionConfigId; }
}
