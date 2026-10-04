package com.starmitra.modules.competition.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "competitions")
public class CompetitionEntity {

    public enum ConfigStatus { DRAFT, CONFIGURED, FROZEN }
    public enum ParticipationStatus { OPEN, CLOSED }
    public enum RoundState { NOT_STARTED, ACTIVE, COMPLETE }

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "config_status", nullable = false, length = 20)
    private ConfigStatus configStatus = ConfigStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "participation_status", nullable = false, length = 20)
    private ParticipationStatus participationStatus = ParticipationStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "round_state", nullable = false, length = 20)
    private RoundState roundState = RoundState.NOT_STARTED;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;                       // REF → users

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected CompetitionEntity() {}

    public CompetitionEntity(String title, String description, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
    }

    public void update(String title, String description) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        touch();
    }

    public void markConfigured() {
        if (configStatus == ConfigStatus.DRAFT) this.configStatus = ConfigStatus.CONFIGURED;
        touch();
    }

    private void touch() { this.updatedAt = OffsetDateTime.now(); }

    public boolean isOwner(UUID userId) { return createdBy.equals(userId); }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public ConfigStatus getConfigStatus() { return configStatus; }
    public ParticipationStatus getParticipationStatus() { return participationStatus; }
    public RoundState getRoundState() { return roundState; }
    public UUID getCreatedBy() { return createdBy; }
    public int getVersion() { return version; }
}
