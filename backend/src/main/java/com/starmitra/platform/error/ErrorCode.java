package com.starmitra.platform.error;

import org.springframework.http.HttpStatus;

/**
 * Canonical error codes — mirrors 05_API-Specifications/API-ERROR-CATALOG.md.
 * Add codes here when the catalog grows; never inline ad-hoc error bodies.
 */
public enum ErrorCode {

    // auth/session
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    OTP_INVALID(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP"),
    OTP_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "Too many OTP attempts"),
    SESSION_REVOKED(HttpStatus.UNAUTHORIZED, "Session has been revoked"),
    REFRESH_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "Refresh token reuse detected"),
    CSRF_REQUIRED(HttpStatus.FORBIDDEN, "CSRF token required"),

    // generic
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
    ROLE_REQUIRED(HttpStatus.FORBIDDEN, "Required system role is missing"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed"),
    CONFLICT(HttpStatus.CONFLICT, "Domain conflict"),
    CONFLICT_VERSION(HttpStatus.CONFLICT, "Concurrent modification detected"),
    STATE_TRANSITION_INVALID(HttpStatus.CONFLICT, "Illegal state transition"),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "Idempotency key reused with different payload"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"),

    // media
    MEDIA_NOT_FOUND(HttpStatus.NOT_FOUND, "Media asset not found"),
    MEDIA_NOT_READY(HttpStatus.CONFLICT, "Media is not ready for delivery"),
    MEDIA_FORBIDDEN(HttpStatus.FORBIDDEN, "Media access denied"),

    // competition/submission/voting
    COMPETITION_NOT_FOUND(HttpStatus.NOT_FOUND, "Competition not found"),
    ELIGIBILITY_DENIED(HttpStatus.FORBIDDEN, "Participant is not eligible"),
    SUBMISSION_DEADLINE_PASSED(HttpStatus.CONFLICT, "Submission deadline has passed"),
    SUBMISSION_ALREADY_FINALIZED(HttpStatus.CONFLICT, "Submission is already finalized"),
    VOTING_CLOSED(HttpStatus.CONFLICT, "Voting window is closed"),
    VOTE_DUPLICATE(HttpStatus.CONFLICT, "Vote already cast"),

    // judge/evaluation
    JUDGE_FORBIDDEN(HttpStatus.FORBIDDEN, "Judge role required"),
    ASSIGNMENT_REVOKED(HttpStatus.GONE, "Judge assignment revoked"),
    CROSS_SCOPE_DENIED(HttpStatus.FORBIDDEN, "Submission outside judge assignment scope"),
    EVALUATION_DUPLICATE(HttpStatus.CONFLICT, "Evaluation already submitted for this scope"),
    RUBRIC_NOT_PUBLISHED(HttpStatus.CONFLICT, "Rubric version is not published"),

    // pricing/entitlements (M22)
    PAYMENT_NOT_ENABLED(HttpStatus.PAYMENT_REQUIRED, "Payment integration is not enabled"),

    // messaging/social/moderation
    NOT_A_MEMBER(HttpStatus.FORBIDDEN, "Not a conversation member"),
    BLOCKED(HttpStatus.FORBIDDEN, "User block is active"),
    MESSAGE_DUPLICATE(HttpStatus.CONFLICT, "Duplicate client message"),
    ALREADY_FOLLOWING(HttpStatus.CONFLICT, "Already following"),
    ALREADY_LIKED(HttpStatus.CONFLICT, "Already liked"),
    TARGET_NOT_LIKEABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Target type cannot be liked"),
    TARGET_RESTRICTED(HttpStatus.FORBIDDEN, "Target is restricted"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() { return status; }
    public String title() { return title; }
    public String typeUri() { return "https://starmitra.app/problems/" + name(); }
}
