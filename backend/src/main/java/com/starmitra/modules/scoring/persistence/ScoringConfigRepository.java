package com.starmitra.modules.scoring.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface ScoringConfigRepository extends JpaRepository<ScoringConfigEntity, UUID> {
    @Query("select coalesce(max(c.versionNo),0) from ScoringConfigEntity c where c.competitionId=:c")
    int maxVersion(UUID c);

    @Query("select c from ScoringConfigEntity c where c.competitionId=:c order by c.versionNo desc")
    List<ScoringConfigEntity> versionsOf(UUID c);
}
