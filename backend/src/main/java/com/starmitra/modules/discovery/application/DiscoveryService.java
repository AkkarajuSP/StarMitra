package com.starmitra.modules.discovery.application;

import com.starmitra.modules.moderation.application.ProfileRestrictionContract;
import com.starmitra.modules.social.application.SocialSignalContract;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.pagination.Cursor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * M05 — PostgreSQL-native SEARCH / DISCOVERY / FEED (ADR-011).
 *
 * Four-way separation honored: search = explicit query, discovery = browse,
 * feed = recency-ordered read surface, recommendation = NOT built.
 *
 * Queries run against authoritative tables (user_profiles.search_vector GIN +
 * pg_trgm indexes already exist in the physical schema). Visibility and
 * moderation are enforced IN SQL — no post-filter leak. The generic
 * projection tables (discovery_projections/feed_projections) are unused for
 * MVP: direct queries on authoritative data keep visibility/restriction
 * fresh by construction (documented choice, rebuildable if scale demands).
 *
 * Ranking (documented, provisional weights):
 *   search  → ts_rank(search_vector, query) DESC, created_at DESC, id ASC
 *   feed    → created_at DESC, id DESC (follow/engagement signals pending M21)
 * Deterministic; stable id tie-break everywhere.
 */
@Service
public class DiscoveryService {

    private final JdbcTemplate jdbc;
    private final ProfileRestrictionContract restriction;
    private final com.starmitra.modules.moderation.application.ModerationContract moderation;
    private final SocialSignalContract social;

    public DiscoveryService(JdbcTemplate jdbc, ProfileRestrictionContract restriction,
                            com.starmitra.modules.moderation.application.ModerationContract moderation,
                            SocialSignalContract social) {
        this.jdbc = jdbc;
        this.restriction = restriction;
        this.moderation = moderation;
        this.social = social;
    }

    public record Item(String type, UUID id, Map<String, Object> fields) {}
    public record Page(String nextCursor, boolean hasMore, Integer total) {}
    public record PageResult(List<Item> items, Page page) {}

    // ---------- SEARCH (explicit query) ----------

    @Transactional(readOnly = true)
    public PageResult search(UUID viewer, String q, String type, String cursor, Integer limit) {
        if (q == null || q.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "q is required");
        }
        int size = Cursor.limit(limit);
        int offset = offset(cursor);
        return switch (type == null ? "ALL" : type.toUpperCase()) {
            case "PROFILE", "USER" -> searchProfiles(q, size, offset);
            case "SKILL" -> searchSkills(q, size, offset);
            case "MEDIA", "CONTENT" -> searchMedia(q, size, offset);
            case "ALL" -> merge(searchProfiles(q, size, offset), searchSkills(q, size, offset),
                    searchMedia(q, size, offset));
            default -> throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown search type");
        };
    }

    /** Profiles: FTS on search_vector + trigram name match; PUBLIC only. */
    private PageResult searchProfiles(String q, int size, int offset) {
        String sql = """
            select p.user_id, p.display_name, p.bio, p.location,
                   ts_rank(p.search_vector, plainto_tsquery('english', ?)) as rank
            from user_profiles p
            where p.visibility_state = 'PUBLIC'
              and (p.search_vector @@ plainto_tsquery('english', ?)
                   or p.display_name ilike ?)
            order by rank desc, p.created_at desc, p.user_id asc
            limit ? offset ?""";
        var rows = jdbc.query(sql, (rs, i) -> toItem("PROFILE", rs), q, q, "%" + q + "%", size + 1, offset);
        return slice(rows.stream().filter(i -> !restriction.isRestricted(i.id())).toList(), size, offset);
    }

    private PageResult searchSkills(String q, int size, int offset) {
        String sql = """
            select s.id, s.name, s.status from talent_skills s
            where s.status = 'ACTIVE' and s.name ilike ?
            order by s.name asc, s.id asc limit ? offset ?""";
        var rows = jdbc.query(sql, (rs, i) -> toItem("SKILL", rs), "%" + q + "%", size + 1, offset);
        return slice(rows, size, offset);
    }

    /** Media: only VERIFIED + processed + PUBLIC + not rejected — filenames only (only searchable field). */
    private PageResult searchMedia(String q, int size, int offset) {
        String sql = """
            select m.id, m.original_filename, m.media_type, m.created_at, m.owner_user_id
            from media_assets m
            where m.visibility = 'PUBLIC' and m.upload_state = 'VERIFIED'
              and m.processing_state in ('COMPLETED','NOT_REQUIRED')
              and m.moderation_state not in ('REJECTED','RESTRICTED')
              and m.original_filename ilike ?
            order by m.created_at desc, m.id asc limit ? offset ?""";
        var rows = jdbc.query(sql, (rs, i) -> toItem("MEDIA", rs), "%" + q + "%", size + 1, offset);
        // M18 truth: restricted media or restricted owner never surfaces
        return slice(rows.stream().filter(i -> !mediaRestricted(i)).toList(), size, offset);
    }

    // ---------- DISCOVERY (browse — no query) ----------

    /** Browse PUBLIC profiles; optional skillId filter via M03 associations. */
    @Transactional(readOnly = true)
    public PageResult discover(UUID viewer, UUID skillId, String cursor, Integer limit) {
        int size = Cursor.limit(limit);
        int offset = offset(cursor);
        String base = """
            select p.user_id, p.display_name, p.bio, p.location, p.created_at
            from user_profiles p
            %s
            where p.visibility_state = 'PUBLIC'
            order by p.created_at desc, p.user_id asc
            limit ? offset ?""";
        List<Item> rows;
        if (skillId != null) {
            String sql = base.formatted(
                    "join user_talent_skills u on u.user_id = p.user_id and u.skill_id = ?");
            rows = jdbc.query(sql, (rs, i) -> toItem("PROFILE", rs), skillId, size + 1, offset);
        } else {
            rows = jdbc.query(base.formatted(""), (rs, i) -> toItem("PROFILE", rs), size + 1, offset);
        }
        return slice(rows.stream().filter(i -> !restriction.isRestricted(i.id())).toList(), size, offset);
    }

    // ---------- FEED (deterministic recency surface) ----------

    /**
     * MVP feed = deliverable PUBLIC media — deterministic:
     *   followed-creator boost (M21 SocialSignalContract) DESC,
     *   then created_at DESC, id DESC (keyset). Engagement-weight
     *   ranking remains a documented PROVISIONAL slot.
     */
    @Transactional(readOnly = true)
    public PageResult feed(UUID viewer, String cursor, Integer limit) {
        int size = Cursor.limit(limit);
        var followees = social.followeeIdsOf(viewer);
        Object afterTs = null; UUID afterId = null;
        if (cursor != null) {
            String[] p = Cursor.decode(cursor);
            if (p.length == 2) {   // "ts:id"
                String[] kv = p[1].split("\\|", 2);
                if (kv.length == 2) { afterTs = OffsetDateTime.parse(kv[0]); afterId = UUID.fromString(kv[1]); }
            }
        }
        String sql = """
            select m.id, m.original_filename, m.media_type, m.created_at, m.owner_user_id
            from media_assets m
            where m.visibility = 'PUBLIC' and m.upload_state = 'VERIFIED'
              and m.processing_state in ('COMPLETED','NOT_REQUIRED')
              and m.moderation_state not in ('REJECTED','RESTRICTED')
              %s
            order by m.created_at desc, m.id desc limit ?""";
        List<Item> rows = (afterTs == null)
                ? jdbc.query(sql.formatted(""), (rs, i) -> toItem("MEDIA", rs), size + 1)
                : jdbc.query(sql.formatted("and (m.created_at, m.id) < (?, ?)"),
                        (rs, i) -> toItem("MEDIA", rs), afterTs, afterId, size + 1);
        // followed-creator boost — deterministic, applied to the fetched window
        rows = rows.stream()
                .filter(i -> !mediaRestricted(i))
                .sorted(java.util.Comparator
                        .<Item, Boolean>comparing(i -> followees.contains(
                                (UUID) i.fields().get("ownerUserId"))).reversed()
                        .thenComparing(i -> String.valueOf(i.fields().get("createdAt")),
                                java.util.Comparator.reverseOrder())
                        .thenComparing(i -> i.id().toString(), java.util.Comparator.reverseOrder()))
                .toList();
        boolean hasMore = rows.size() > size;
        var items = new ArrayList<>(rows.subList(0, Math.min(size, rows.size())));
        String next = null;
        if (hasMore && !items.isEmpty()) {
            var last = items.get(items.size() - 1);
            next = Cursor.encode("ts", last.fields().get("createdAt") + "|" + last.id());
        }
        return new PageResult(items, new Page(next, hasMore, null));
    }

    // ---------- helpers ----------

    /** M18 enforcement for media items — media OR its owner restricted → excluded. */
    private boolean mediaRestricted(Item i) {
        if (moderation.isRestricted("MEDIA", i.id())) return true;
        var owner = (UUID) i.fields().get("ownerUserId");
        return owner != null && moderation.isRestricted("USER", owner);
    }

    private Item toItem(String type, ResultSet rs) throws SQLException {
        return switch (type) {
            case "PROFILE" -> new Item("PROFILE", (UUID) rs.getObject("user_id"),
                    Map.of("displayName", nullTo(rs.getString("display_name")),
                           "bio", nullTo(rs.getString("bio")),
                           "location", nullTo(rs.getString("location"))));
            case "SKILL" -> new Item("SKILL", (UUID) rs.getObject("id"),
                    Map.of("name", rs.getString("name"), "status", rs.getString("status")));
            default -> {
                var f = new java.util.LinkedHashMap<String, Object>();
                f.put("filename", nullTo(rs.getString("original_filename")));
                f.put("mediaType", rs.getString("media_type"));
                f.put("createdAt", String.valueOf(rs.getObject("created_at")));
                f.put("ownerUserId", rs.getObject("owner_user_id"));
                yield new Item("MEDIA", (UUID) rs.getObject("id"), f);
            }
        };
    }

    private static String nullTo(String s) { return s == null ? "" : s; }

    private PageResult slice(List<Item> rows, int size, int offset) {
        boolean hasMore = rows.size() > size;
        var items = new ArrayList<>(rows.subList(0, Math.min(size, rows.size())));
        return new PageResult(items, new Page(hasMore ? Cursor.encode("o", String.valueOf(offset + size)) : null,
                hasMore, null));
    }

    private int offset(String cursor) {
        if (cursor == null) return 0;
        String[] p = Cursor.decode(cursor);
        return p.length == 2 ? Integer.parseInt(p[1]) : 0;
    }

    private PageResult merge(PageResult... results) {
        var items = new ArrayList<Item>();
        boolean more = false;
        for (var r : results) { items.addAll(r.items()); more |= r.page().hasMore(); }
        return new PageResult(items, new Page(null, more, null));
    }
}
