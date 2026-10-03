package com.starmitra.modules.social.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class SocialDtos {

    private SocialDtos() {}

    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}

    public record FollowItem(UUID userId, String followedAt) {}
    public record FollowPage(List<FollowItem> items, PageMeta page) {}

    public record LikeCreate(
            @NotNull @Pattern(regexp = "MEDIA|PORTFOLIO") String targetType,
            @NotNull UUID targetId) {}

    public record CommentCreate(
            @NotNull @Pattern(regexp = "MEDIA|PORTFOLIO") String targetType,
            @NotNull UUID targetId,
            @NotBlank @Size(max = 4000) String body) {}

    public record Comment(UUID id, UUID authorId, String body, String createdAt) {}
    public record CommentPage(List<Comment> items, PageMeta page) {}

    public record EngagementCounts(long followCount, long likeCount, long commentCount, boolean derived) {}
}
