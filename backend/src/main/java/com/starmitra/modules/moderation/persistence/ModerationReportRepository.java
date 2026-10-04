package com.starmitra.modules.moderation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface ModerationReportRepository
        extends JpaRepository<ModerationReportEntity, UUID> {

    @Query("select r from ModerationReportEntity r where r.status=:s order by r.createdAt desc")
    List<ModerationReportEntity> byStatus(ModerationReportEntity.Status s,
                                          org.springframework.data.domain.Pageable p);

    long countByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
            UUID reporterId, ModerationReportEntity.TargetType type, UUID targetId,
            java.util.Collection<ModerationReportEntity.Status> statuses);
}
