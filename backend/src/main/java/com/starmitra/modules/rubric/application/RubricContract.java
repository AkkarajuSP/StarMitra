package com.starmitra.modules.rubric.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** M13-owned rubric-truth contract — M14 scoring consumes immutable views. */
public interface RubricContract {

    record CriterionView(UUID id, String name, BigDecimal weight, BigDecimal maxScore,
                         Integer sortOrder) {}

    record RubricVersionView(UUID id, UUID templateId, int versionNo, String status,
                             List<CriterionView> criteria, Integer scaleMin, Integer scaleMax) {}

    /** The PUBLISHED rubric for a version id (immutable view). Empty if not published. */
    Optional<RubricVersionView> publishedVersion(UUID versionId);

    /** Rubric resolved for a round (M09 round.rubric_version_id → published version). */
    Optional<RubricVersionView> resolveForRound(UUID roundId);
}
