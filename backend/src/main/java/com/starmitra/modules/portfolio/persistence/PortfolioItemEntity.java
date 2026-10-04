package com.starmitra.modules.portfolio.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "portfolio_items")
public class PortfolioItemEntity {

    public enum Visibility { PUBLIC, PRIVATE, FOLLOWERS, COLLABORATION_ONLY }

    @Id
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "skill_id")
    private UUID skillId;                         // REF → talent_skills (no FK)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected PortfolioItemEntity() {}

    public PortfolioItemEntity(UUID portfolioId, String title, String description,
                               UUID skillId, Visibility visibility) {
        this.id = UUID.randomUUID();
        this.portfolioId = portfolioId;
        this.title = title;
        this.description = description;
        this.skillId = skillId;
        this.visibility = visibility;
    }

    public void update(String title, String description, UUID skillId, Visibility visibility) {
        if (title != null) this.title = title;
        this.description = description;
        this.skillId = skillId;
        if (visibility != null) this.visibility = visibility;
        touch();
    }

    private void touch() { this.updatedAt = OffsetDateTime.now(); }

    public String etag() {
        return "\"" + updatedAt.toEpochSecond() + "." + updatedAt.getNano() + "\"";
    }

    public UUID getId() { return id; }
    public UUID getPortfolioId() { return portfolioId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public UUID getSkillId() { return skillId; }
    public Visibility getVisibility() { return visibility; }
    public String getStatus() { return status; }
}
