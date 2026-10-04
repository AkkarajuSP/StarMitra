package com.starmitra.modules.rubric.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationTemplateVersionRepository
        extends JpaRepository<EvaluationTemplateVersionEntity, UUID> {

    @Query("select coalesce(max(v.versionNo), 0) from EvaluationTemplateVersionEntity v " +
           "where v.templateId = :templateId")
    int maxVersion(UUID templateId);

    @Query("select v from EvaluationTemplateVersionEntity v where v.templateId = :templateId " +
           "order by v.versionNo desc")
    List<EvaluationTemplateVersionEntity> versionsOf(UUID templateId);

    /** Latest DRAFT for a template (editable head). */
    @Query("select v from EvaluationTemplateVersionEntity v where v.templateId = :templateId " +
           "and v.status = 'DRAFT' order by v.versionNo desc")
    List<EvaluationTemplateVersionEntity> draftsOf(UUID templateId);
}
