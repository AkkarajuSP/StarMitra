package com.starmitra.modules.rubric.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterionEntity, UUID> {

    List<EvaluationCriterionEntity> findByVersionIdOrderBySortOrderAsc(UUID versionId);

    @Modifying
    @Query("delete from EvaluationCriterionEntity c where c.versionId = :versionId")
    void deleteByVersionId(UUID versionId);
}
