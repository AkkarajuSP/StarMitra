package com.starmitra.modules.portfolio.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "portfolios")
public class PortfolioEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;                          // DB-04: one portfolio per user

    @Column(length = 160)
    private String title;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected PortfolioEntity() {}

    public PortfolioEntity(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
    }

    public void updateTitle(String title) { this.title = title; touch(); }

    private void touch() { this.updatedAt = OffsetDateTime.now(); }

    public String etag() {
        return "\"" + updatedAt.toEpochSecond() + "." + updatedAt.getNano() + "\"";
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
}
