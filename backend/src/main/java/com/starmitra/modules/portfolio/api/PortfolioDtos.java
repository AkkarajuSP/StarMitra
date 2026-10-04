package com.starmitra.modules.portfolio.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class PortfolioDtos {

    private PortfolioDtos() {}

    public record Portfolio(UUID id, UUID userId, String title, String status,
                            List<PortfolioItem> items) {}

    public record PortfolioUpdate(@Size(max = 160) String title) {}

    public record PortfolioItem(UUID id, String title, String description, UUID skillId,
                                String visibility, List<UUID> mediaIds) {}

    public record PortfolioItemCreate(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 4000) String description,
            UUID skillId,
            @Pattern(regexp = "PUBLIC|FOLLOWERS|COLLABORATION_ONLY|PRIVATE") String visibility) {}

    public record AssetLink(@NotNull UUID mediaId, String role, Integer sortOrder) {}

    public record CreditLink(@NotNull UUID projectCreditId) {}
}
