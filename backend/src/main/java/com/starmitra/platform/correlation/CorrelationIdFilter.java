package com.starmitra.platform.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Establishes a correlation ID per request: echoed via X-Correlation-Id,
 * propagated into MDC for logs and into Problem Details responses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = req.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank() || correlationId.length() > 64) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        res.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    public static String current() {
        String id = MDC.get(MDC_KEY);
        return id != null ? id : UUID.randomUUID().toString();
    }
}
