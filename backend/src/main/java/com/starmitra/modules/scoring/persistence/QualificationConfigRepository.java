package com.starmitra.modules.scoring.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface QualificationConfigRepository extends JpaRepository<QualificationConfigEntity, UUID> {
    @Query("select coalesce(max(c.versionNo),0) from QualificationConfigEntity c where c.competitionId=:c")
    int maxVersion(UUID c);

    @Query("select c from QualificationConfigEntity c where c.competitionId=:c and c.status='PUBLISHED' order by c.versionNo desc")
    List<QualificationConfigEntity> latestPublished(UUID c);
}
