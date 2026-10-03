package com.starmitra.platform.pagination;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Opaque keyset cursor — Base64url("key:value") so clients never construct it.
 * Stable ordering must be a unique, monotonically-ordered key per stream
 * (e.g. (created_at,id), message sequence).
 */
public final class Cursor {

    private Cursor() {}

    public static String encode(String key, String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((key + ":" + value).getBytes(StandardCharsets.UTF_8));
    }

    public static String[] decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return new String[0];
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            return decoded.split(":", 2);
        } catch (IllegalArgumentException e) {
            return new String[0];
        }
    }

    public static int limit(Integer limit) {
        int l = (limit == null || limit <= 0) ? 20 : limit;
        return Math.min(l, 100);
    }
}
