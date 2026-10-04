package com.starmitra.platform.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    @Query("select a from AuditLogEntity a " +
           "where (:module is null or a.module = :module) " +
           "and (:actorId is null or a.actorId = :actorId) " +
           "order by a.createdAt desc, a.id desc")
    List<AuditLogEntity> search(String module, UUID actorId, Pageable p);
}
