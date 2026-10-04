package com.starmitra.modules.submission.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface SubmissionMediaRepository
        extends JpaRepository<SubmissionMediaEntity, SubmissionMediaEntity.Pk> {

    @Query("select m.mediaId from SubmissionMediaEntity m where m.submissionId = :id order by m.sortOrder")
    List<UUID> findMediaIds(UUID id);
}
