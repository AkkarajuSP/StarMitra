package com.starmitra.modules.competition.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** DB-03 typed single table — USER xor PROJECT (ck_cp_xor + UQ nulls-not-distinct). */
@Entity
@Table(name = "competition_participants")
public class CompetitionParticipantEntity {

    public enum Type { USER, PROJECT }
    public enum Status { ACTIVE, WITHDRAWN }

    @Id
    private UUID id;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 10)
    private Type participantType;

    @Column(name = "user_id")
    private UUID userId;                           // REF → users

    @Column(name = "project_id")
    private UUID projectId;                        // REF → creative_rooms

    @Column(name = "registered_at", nullable = false)
    private OffsetDateTime registeredAt = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected CompetitionParticipantEntity() {}

    public static CompetitionParticipantEntity forUser(UUID compId, UUID categoryId, UUID userId) {
        var p = new CompetitionParticipantEntity();
        p.id = UUID.randomUUID();
        p.competitionId = compId;
        p.categoryId = categoryId;
        p.participantType = Type.USER;
        p.userId = userId;
        return p;
    }

    public static CompetitionParticipantEntity forProject(UUID compId, UUID categoryId, UUID projectId) {
        var p = new CompetitionParticipantEntity();
        p.id = UUID.randomUUID();
        p.competitionId = compId;
        p.categoryId = categoryId;
        p.participantType = Type.PROJECT;
        p.projectId = projectId;
        return p;
    }

    public UUID getId() { return id; }
    public UUID getCompetitionId() { return competitionId; }
    public UUID getCategoryId() { return categoryId; }
    public Type getParticipantType() { return participantType; }
    public UUID getUserId() { return userId; }
    public UUID getProjectId() { return projectId; }
    public Status getStatus() { return status; }
}
