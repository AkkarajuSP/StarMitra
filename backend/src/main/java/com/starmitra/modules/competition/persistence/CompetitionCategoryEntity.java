package com.starmitra.modules.competition.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "competition_categories")
public class CompetitionCategoryEntity {

    @Id
    private UUID id;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected CompetitionCategoryEntity() {}

    public CompetitionCategoryEntity(UUID competitionId, String name, String description) {
        this.id = UUID.randomUUID();
        this.competitionId = competitionId;
        this.name = name;
        this.description = description;
    }

    public UUID getId() { return id; }
    public UUID getCompetitionId() { return competitionId; }
    public String getName() { return name; }
}
