package com.starmitra.modules.moderation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface ModerationCaseRepository extends JpaRepository<ModerationCaseEntity, UUID> {

    @Query("select c from ModerationCaseEntity c where c.status=:s order by c.createdAt desc")
    List<ModerationCaseEntity> byStatus(ModerationCaseEntity.Status s,
                                        org.springframework.data.domain.Pageable p);
}
