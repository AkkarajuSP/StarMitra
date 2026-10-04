package com.starmitra.platform.audit;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Kernel audit READ seam — M19 admin audit search consumes this; append-only model preserved. */
@Service
public class AuditQueryService {

    private final AuditLogRepository repository;

    public AuditQueryService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public record AuditView(String module, String action, String actorId, String targetType,
                            String targetId, String createdAt) {}

    @Transactional(readOnly = true)
    public List<AuditView> search(String module, UUID actorId, int limit) {
        return repository.search(module, actorId, PageRequest.of(0, Math.max(1,
                        Math.min(limit, 100)))).stream()
                .map(e -> new AuditView(e.getModule(), e.getAction(),
                        e.getActorId() == null ? null : e.getActorId().toString(),
                        e.getTargetType(), e.getTargetId(), e.getCreatedAt().toString()))
                .toList();
    }
}
