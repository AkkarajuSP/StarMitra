package com.starmitra.modules.profile.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {

    public enum Visibility { PUBLIC, FOLLOWERS, COLLABORATION_ONLY, PRIVATE }

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column
    private String bio;

    @Column(length = 120)
    private String location;

    @Column(name = "avatar_media_id")
    private UUID avatarMediaId;      // REF → media_assets (M04, logical ref)

    @Column(name = "banner_media_id")
    private UUID bannerMediaId;      // REF → media_assets (M04, logical ref)

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_state", nullable = false, length = 30)
    private Visibility visibilityState = Visibility.PRIVATE;

    @Column(name = "completion_score")
    private Short completionScore;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected UserProfileEntity() {}

    public UserProfileEntity(UUID userId, String displayName) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.displayName = displayName;
    }

    public void applyUpdate(String displayName, String bio, String location,
                            UUID avatarMediaId, Visibility visibility) {
        if (displayName != null) this.displayName = displayName;
        if (bio != null) this.bio = bio;
        if (location != null) this.location = location;
        if (avatarMediaId != null) this.avatarMediaId = avatarMediaId;
        if (visibility != null) this.visibilityState = visibility;
        this.updatedAt = OffsetDateTime.now();
    }

    /** ETag derived from updated_at — the schema carries no version column. */
    public String etag() {
        return "\"" + updatedAt.toEpochSecond() + "-" + updatedAt.getNano() + "\"";
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getBio() { return bio; }
    public String getLocation() { return location; }
    public UUID getAvatarMediaId() { return avatarMediaId; }
    public Visibility getVisibilityState() { return visibilityState; }
}
