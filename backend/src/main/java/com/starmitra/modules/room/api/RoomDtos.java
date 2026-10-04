package com.starmitra.modules.room.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class RoomDtos {

    private RoomDtos() {}

    public record Room(UUID id, String name, String status, String visibility,
                       UUID ownerId, int version) {}

    public record RoomCreate(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 4000) String description,
            @Pattern(regexp = "PUBLIC|PRIVATE") String visibility) {}

    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}
    public record RoomPage(List<Room> items, PageMeta page) {}

    public record ProjectMember(UUID userId, String status, String joinedAt,
                                List<String> contributionRoles) {}

    public record InviteCreate(@NotNull UUID inviteeId) {}
    public record InviteResponse(@NotNull @Pattern(regexp = "ACCEPT|DECLINE") String action) {}

    public record Invitation(UUID id, UUID roomId, String status, String expiresAt) {}

    public record ContributionAssign(@NotNull UUID memberUserId,
                                     @NotBlank @Size(max = 80) String roleName) {}

    public record Contribution(UUID id, UUID userId, String roleName, boolean verified) {}

    public record TaskCreate(@NotBlank @Size(max = 200) String title,
                             @Size(max = 4000) String description,
                             UUID assigneeMemberId, String dueDate) {}

    public record RoomTask(UUID id, String title, String status, UUID assigneeMemberId) {}

    public record AssetLink(@NotNull UUID mediaId, String role, Integer sortOrder) {}
}
