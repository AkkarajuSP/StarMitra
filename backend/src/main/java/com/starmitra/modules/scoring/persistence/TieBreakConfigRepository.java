package com.starmitra.modules.scoring.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface TieBreakConfigRepository extends JpaRepository<TieBreakConfigEntity, UUID> {
    @Query("select coalesce(max(c.versionNo),0) from TieBreakConfigEntity c where c.competitionId=:c")
    int maxVersion(UUID c);

    @Query("select c from TieBreakConfigEntity c where c.competitionId=:c and c.status='PUBLISHED' order by c.versionNo desc")
    List<TieBreakConfigEntity> latestPublished(UUID c);
}
