package com.starmitra.modules.leaderboard.api;

import com.starmitra.modules.leaderboard.application.LeaderboardService;
import com.starmitra.platform.pagination.Cursor;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /api/v1/leaderboards — M16 (publication-gated read + admin lifecycle). */
@RestController
public class LeaderboardController {

    private final LeaderboardService leaderboards;

    public LeaderboardController(LeaderboardService leaderboards) {
        this.leaderboards = leaderboards;
    }

    public record PublicationCreate(@NotNull UUID competitionId, UUID categoryId, UUID roundId,
                                    @NotNull String action) {}
    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}
    public record Entry(int rank, UUID entryRef, Map<String, Object> display) {}
    public record LeaderboardPage(List<Entry> items, PageMeta page) {}

    @GetMapping("/api/v1/leaderboards")
    public ResponseEntity<LeaderboardPage> getLeaderboard(
            @RequestParam UUID competitionId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID roundId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        int lim = Cursor.limit(limit);
        var items = leaderboards.leaderboard(competitionId, categoryId, roundId, cursor, lim + 1);
        boolean more = items.size() > lim;
        var page = items.stream().limit(lim)
                .map(i -> new Entry(i.rank(), i.entryRef(), i.display())).toList();
        String next = more && !page.isEmpty()
                ? Cursor.encode("rank", String.valueOf(page.get(page.size() - 1).rank())) : null;
        return ResponseEntity.ok(new LeaderboardPage(page,
                new PageMeta(next, more, page.size())));
    }

    @PostMapping("/api/v1/leaderboards/publications")
    public ResponseEntity<Map<String, String>> publishLeaderboard(
            @Valid @RequestBody PublicationCreate body) {
        var id = leaderboards.publish(SecurityUtils.currentUserId(), body.competitionId(),
                body.categoryId(), body.roundId(), body.action());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id.toString()));
    }

    @PostMapping("/api/v1/leaderboards/{publicationId}/snapshot")
    public ResponseEntity<Void> snapshotLeaderboard(@PathVariable UUID publicationId) {
        leaderboards.snapshot(SecurityUtils.currentUserId(), publicationId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
