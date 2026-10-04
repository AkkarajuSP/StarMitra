package com.starmitra.modules.rubric.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EvaluationTemplateRepository extends JpaRepository<EvaluationTemplateEntity, UUID> {}
