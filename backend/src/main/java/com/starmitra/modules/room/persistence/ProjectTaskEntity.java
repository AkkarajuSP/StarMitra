package com.starmitra.modules.room.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_tasks")
public class ProjectTaskEntity {

    @Id
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "assignee_member_id")
    private UUID assigneeMemberId;                // = member user_id per schema naming

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected ProjectTaskEntity() {}

    public ProjectTaskEntity(UUID roomId, String title, String description,
                             UUID assigneeMemberId, LocalDate dueDate) {
        this.id = UUID.randomUUID();
        this.roomId = roomId;
        this.title = title;
        this.description = description;
        this.assigneeMemberId = assigneeMemberId;
        this.dueDate = dueDate;
    }

    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public UUID getAssigneeMemberId() { return assigneeMemberId; }
}
