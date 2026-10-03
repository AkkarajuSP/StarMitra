package com.starmitra.platform.idempotency;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Reusable idempotent-command executor — API-IDEMPOTENCY-MATRIX.md is authoritative.
 * Strategy: database uniqueness is the enforcement layer; this service normalizes
 * the "unique violation → replay existing / conflict" outcome deterministically.
 */
@Service
public class IdempotencyService {

    /**
     * Execute {@code action}; on unique violation, return {@code onDuplicate}
     * instead — replay semantics (return existing) rather than double-effect.
     */
    public <T> T execute(Supplier<T> action, Supplier<T> onDuplicate) {
        try {
            return action.get();
        } catch (DataIntegrityViolationException dup) {
            return onDuplicate.get();
        }
    }

    /**
     * Header-required variant — callers must have already normalized the key.
     * Returns null-safe empty string when absent; modules decide if mandatory.
     */
    public static String keyOrNull(String idempotencyKey) {
        return (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey.trim();
    }
}
