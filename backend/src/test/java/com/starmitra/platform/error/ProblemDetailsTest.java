package com.starmitra.platform.error;

import com.starmitra.platform.correlation.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.*;

class ProblemDetailsTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiExceptionRendersRfc9457Shape() {
        var res = handler.handleApi(new ApiException(ErrorCode.CROSS_SCOPE_DENIED, "outside scope"),
                Mockito.mock(HttpServletRequest.class));
        ProblemDetail pd = res.getBody();
        assertNotNull(pd);
        assertEquals(403, res.getStatusCode().value());
        assertEquals("CROSS_SCOPE_DENIED", pd.getProperties().get("code"));
        assertEquals("https://starmitra.app/problems/CROSS_SCOPE_DENIED", pd.getType().toString());
        assertNotNull(pd.getProperties().get("correlationId"));
    }

    @Test
    void unexpectedErrorIsSanitized() {
        var res = handler.handleUnexpected(new RuntimeException("db connection to pg.internal failed"),
                Mockito.mock(HttpServletRequest.class));
        assertEquals(500, res.getStatusCode().value());
        assertEquals("Internal error", res.getBody().getDetail());
        assertFalse(String.valueOf(res.getBody().getDetail()).contains("pg.internal"));
    }
}
