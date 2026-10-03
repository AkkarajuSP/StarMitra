package com.starmitra.platform.pagination;

import java.util.List;
import java.util.function.Function;

/**
 * Cursor page envelope matching the OpenAPI PageMeta contract
 * ({nextCursor, hasMore, total?}).
 */
public record PageResult<T>(List<T> items, PageMeta page) {

    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}

    public static <T> PageResult<T> of(List<T> items, int limit, Function<T, String> cursorKey) {
        boolean hasMore = items.size() > limit;
        List<T> pageItems = hasMore ? items.subList(0, limit) : items;
        String next = hasMore && !pageItems.isEmpty() ? cursorKey.apply(pageItems.get(pageItems.size() - 1)) : null;
        return new PageResult<>(pageItems, new PageMeta(next, hasMore, null));
    }
}
