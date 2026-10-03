package com.starmitra.modules.identity.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    public enum Status { ACTIVE, SUSPENDED, BLOCKED, DEACTIVATED }

    @Id
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected UserEntity() {}

    public UserEntity(String email, String phone) {
        this.id = UUID.randomUUID();      // app-generated (UUIDv7 at service layer vNext)
        this.email = email;
        this.phone = phone;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Status getStatus() { return status; }
}
