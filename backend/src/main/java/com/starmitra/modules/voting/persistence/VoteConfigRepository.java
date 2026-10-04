package com.starmitra.modules.voting.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface VoteConfigRepository extends JpaRepository<VoteConfigEntity, UUID> {

    @Query("select coalesce(max(v.versionNo), 0) from VoteConfigEntity v where v.seriesKey = :key")
    int maxVersion(String key);
}
