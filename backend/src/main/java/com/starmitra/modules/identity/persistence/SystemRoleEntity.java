package com.starmitra.modules.identity.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "system_roles")
public class SystemRoleEntity {

    public static final String USER = "USER";
    public static final String JUDGE = "JUDGE";
    public static final String ADMIN = "ADMIN";
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    @Id
    private UUID id;

    @Column(nullable = false, length = 50)
    private String name;

    private String description;

    protected SystemRoleEntity() {}

    public UUID getId() { return id; }
    public String getName() { return name; }
}
