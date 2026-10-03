package com.starmitra.modules.discovery.api;

import com.starmitra.modules.discovery.application.DiscoveryService;
import com.starmitra.platform.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /api/v1/{search,discovery,feed} — read-only, contract shapes: SearchPage. */
@RestController
@RequestMapping("/api/v1")
public class DiscoveryController {

    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}
    public record SearchPage(List<Map<String, Object>> items, PageMeta page) {}

    private final DiscoveryService discovery;

    public DiscoveryController(DiscoveryService discovery) {
        this.discovery = discovery;
    }

    @GetMapping("/search")
    public ResponseEntity<SearchPage> search(
            @RequestParam String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return toPage(discovery.search(SecurityUtils.currentUserId(), q, type, cursor, limit));
    }

    @GetMapping("/discovery")
    public ResponseEntity<SearchPage> discover(
            @RequestParam(required = false) UUID skillId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        // category param exists in contract; no category field exists in model — ignored, documented
        return toPage(discovery.discover(SecurityUtils.currentUserId(), skillId, cursor, limit));
    }

    @GetMapping("/feed")
    public ResponseEntity<SearchPage> getFeed(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return toPage(discovery.feed(SecurityUtils.currentUserId(), cursor, limit));
    }

    private ResponseEntity<SearchPage> toPage(DiscoveryService.PageResult r) {
        var items = r.items().stream()
                .map(i -> {
                    var m = new java.util.LinkedHashMap<String, Object>();
                    m.put("type", i.type());
                    m.put("id", i.id());
                    m.putAll(i.fields());
                    return (Map<String, Object>) m;
                }).toList();
        return ResponseEntity.ok(new SearchPage(items,
                new PageMeta(r.page().nextCursor(), r.page().hasMore(), r.page().total())));
    }
}
