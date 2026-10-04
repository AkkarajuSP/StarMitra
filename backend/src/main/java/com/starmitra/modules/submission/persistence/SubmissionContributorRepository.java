package com.starmitra.modules.submission.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SubmissionContributorRepository extends JpaRepository<SubmissionContributorEntity, UUID> {
    List<SubmissionContributorEntity> findBySubmissionId(UUID submissionId);
}
