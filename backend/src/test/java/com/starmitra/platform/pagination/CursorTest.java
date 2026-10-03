package com.starmitra.platform.pagination;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CursorTest {

    @Test
    void roundTripsKeyAndValue() {
        String c = Cursor.encode("created_at", "2025-01-01T00:00:00Z|uuid");
        assertArrayEquals(new String[]{"created_at", "2025-01-01T00:00:00Z|uuid"}, Cursor.decode(c));
    }

    @Test
    void invalidCursorReturnsEmpty() {
        assertEquals(0, Cursor.decode("###not-base64###").length);
        assertEquals(0, Cursor.decode(null).length);
    }

    @Test
    void limitIsBounded() {
        assertEquals(20, Cursor.limit(null));
        assertEquals(100, Cursor.limit(10_000));
        assertEquals(5, Cursor.limit(5));
    }
}
