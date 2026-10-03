package com.starmitra.modules.identity.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuthenticationAuditEventRepository extends JpaRepository<AuthenticationAuditEventEntity, UUID> {
}
