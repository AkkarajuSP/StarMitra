package com.starmitra.modules.room.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "creative_rooms")
public class CreativeRoomEntity {

    public enum Visibility { PUBLIC, PRIVATE }
    public enum Status { OPEN, CLOSED, ARCHIVED }

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility = Visibility.PRIVATE;

    /** Real optimistic-lock column per physical schema — If-Match = version. */
    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected CreativeRoomEntity() {}

    public CreativeRoomEntity(UUID ownerId, String name, String description, Visibility visibility) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.visibility = visibility;
    }

    public void update(String name, String description, Visibility visibility) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (visibility != null) this.visibility = visibility;
        touch();
    }

    private void touch() { this.updatedAt = OffsetDateTime.now(); }

    public boolean isOwner(UUID userId) { return ownerId.equals(userId); }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public Visibility getVisibility() { return visibility; }
    public int getVersion() { return version; }
}
