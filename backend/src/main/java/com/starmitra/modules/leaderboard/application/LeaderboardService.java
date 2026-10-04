package com.starmitra.modules.leaderboard.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starmitra.modules.competition.application.CompetitionStructureContract;
import com.starmitra.modules.leaderboard.persistence.*;
import com.starmitra.modules.progression.application.ProgressionTruthContract;
import com.starmitra.modules.scoring.application.ScoringTruthContract;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.pagination.Cursor;
import com.starmitra.platform.security.SystemRoleGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * M16 — Leaderboards. PURE READ-VIEW over M14 truth (+ M15 outcomes).
 * Never recalculates score/rank/qualification. Publication-gated:
 * a leaderboard is only visible when an admin PUBLISHES it.
 */
@Service
public class LeaderboardService {

    private final LeaderboardProjectionRepository projections;
    private final LeaderboardPublicationRepository publications;
    private final LeaderboardSnapshotRepository snapshots;
    private final ScoringTruthContract scoring;
    private final ProgressionTruthContract progression;
    private final CompetitionStructureContract competition;
    private final AuditService audit;
    private final ObjectMapper json;

    public LeaderboardService(LeaderboardProjectionRepository projections,
                              LeaderboardPublicationRepository publications,
                              LeaderboardSnapshotRepository snapshots,
                              ScoringTruthContract scoring,
                              ProgressionTruthContract progression,
                              CompetitionStructureContract competition,
                              AuditService audit, ObjectMapper json) {
        this.projections = projections;
        this.publications = publications;
        this.snapshots = snapshots;
        this.scoring = scoring;
        this.progression = progression;
        this.competition = competition;
        this.audit = audit;
        this.json = json;
    }

    public record Item(int rank, UUID entryRef, Map<String, Object> display) {}

    // ---------- publication lifecycle (admin) ----------

    @Transactional
    public UUID publish(UUID admin, UUID competitionId, UUID categoryId, UUID roundId,
                        String action) {
        SystemRoleGuard.requireAdmin();
        if (roundId != null && !competition.roundBelongsTo(competitionId, roundId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Round not in competition");
        }
        var status = LeaderboardPublicationEntity.Status.valueOf(switch (action) {
            case "PUBLISH" -> "PUBLISHED";
            case "HIDE" -> "HIDDEN";
            case "ARCHIVE" -> "ARCHIVED";
            default -> throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid action");
        });
        UUID resolvedRound = nz(roundId);
        UUID resolvedCat = nz(categoryId);
        var pub = publications.findByCompetitionIdAndCategoryIdAndRoundId(
                        competitionId, resolvedCat, resolvedRound)
                .orElseGet(() -> new LeaderboardPublicationEntity(competitionId, resolvedCat,
                        resolvedRound, status, admin));
        pub.apply(status, admin);
        publications.save(pub);
        if (status == LeaderboardPublicationEntity.Status.PUBLISHED && roundId != null) {
            refreshProjection(competitionId, roundId);        // rebuildable, non-authoritative
        }
        audit.record("M16", "LEADERBOARD_" + action, admin, "user",
                "leaderboard_publication", pub.getId().toString(), null);
        return pub.getId();
    }

    /** Rebuild projection from M14 truth (+ M15 outcome for display). */
    private void refreshProjection(UUID competitionId, UUID roundId) {
        projections.deleteByCompetitionIdAndRoundId(competitionId, roundId);
        var outcomes = progression.outcomesOf(roundId);
        for (var r : scoring.latestResults(roundId)) {
            var display = Map.of(
                    "finalScore", r.finalScore() == null ? "" : r.finalScore().toPlainString(),
                    "qualified", Boolean.TRUE.equals(r.qualified()),
                    "tied", Boolean.TRUE.equals(r.tied()),
                    "progression", outcomes.getOrDefault(r.submissionId(), "PENDING"));
            try {
                projections.save(new LeaderboardProjectionEntity(competitionId,
                        nz(r.categoryId()),
                        roundId, r.submissionId(), json.writeValueAsString(display), r.rank()));
            } catch (Exception e) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Projection payload failed");
            }
        }
    }

    /** Admin: seal version refs (reproducibility handle — never recalculates). */
    @Transactional
    public void snapshot(UUID admin, UUID publicationId) {
        SystemRoleGuard.requireAdmin();
        publications.findById(publicationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        snapshots.save(new LeaderboardSnapshotEntity(publicationId,
                "{\"capturedBy\":\"" + admin + "\"}"));
        audit.record("M16", "LEADERBOARD_SNAPSHOT", admin, "user",
                "publication", publicationId.toString(), null);
    }

    // ---------- read (publication-gated, cursor paginated) ----------

    @Transactional(readOnly = true)
    public List<Item> leaderboard(UUID competitionId, UUID categoryId, UUID roundId,
                                  String cursor, int limit) {
        var pub = publications.findByCompetitionIdAndCategoryIdAndRoundId(
                        competitionId, nz(categoryId), nz(roundId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (pub.getStatus() != LeaderboardPublicationEntity.Status.PUBLISHED) {
            throw new ApiException(ErrorCode.NOT_FOUND);      // publication-gated (hidden → 404)
        }
        var rows = roundId != null && categoryId == null
                ? projections.findByCompetitionIdAndRoundIdOrderByRankAsc(competitionId, roundId)
                : projections.findByCompetitionIdAndCategoryIdAndRoundIdOrderByRankAsc(
                        competitionId, nz(categoryId), nz(roundId));
        int afterRank = cursorOffset(cursor);
        return rows.stream()
                .filter(p -> afterRank == 0 || p.getRank() > afterRank)
                .limit(limit)
                .map(p -> new Item(p.getRank(), p.getEntryRef(), readMap(p.getDisplayPayload())))
                .toList();
    }

    /** Comp-wide keys normalize null category/round to zero-UUID (NOT NULL cols). */
    private static UUID nz(UUID id) {
        return id != null ? id : UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    private int cursorOffset(String cursor) {
        var parts = Cursor.decode(cursor);                  // opaque "rank:<n>"
        if (parts.length < 2) return 0;
        try {
            return Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Object> readMap(String s) {
        try {
            return json.readValue(s, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
