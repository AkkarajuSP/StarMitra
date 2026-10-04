package com.starmitra.modules.submission.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SubmissionHistoryRepository extends JpaRepository<SubmissionHistoryEntity, UUID> {
    List<SubmissionHistoryEntity> findBySubmissionIdOrderByCreatedAtAsc(UUID submissionId);
}
