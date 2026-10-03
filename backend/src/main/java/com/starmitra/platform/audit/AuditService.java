package com.starmitra.platform.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Kernel audit writer — insert-only, REQUIRES_NEW so audit survives caller
 * rollback where the event itself must be recorded (e.g. failed auth).
 * Distinct from telemetry; never logs secrets.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String module, String action, UUID actorId, String actorContext,
                       String targetType, String targetId, String reason) {
        repository.save(new AuditLogEntity(module, action, actorId, actorContext, targetType, targetId, reason));
        log.debug("audit {}.{} target={}:{}", module, action, targetType, targetId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLogEntity entry) {
        repository.save(entry);
    }
}
