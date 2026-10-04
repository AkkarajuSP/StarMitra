package com.starmitra.modules.progression.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface ProgressionConfigRepository extends JpaRepository<ProgressionConfigEntity, UUID> {
    @Query("select coalesce(max(c.versionNo),0) from ProgressionConfigEntity c where c.competitionId=:c")
    int maxVersion(UUID c);

    @Query("select c from ProgressionConfigEntity c where c.competitionId=:c and c.status='PUBLISHED' " +
           "order by c.versionNo desc")
    List<ProgressionConfigEntity> latestPublished(UUID c);
}
