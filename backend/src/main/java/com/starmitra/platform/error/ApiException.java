package com.starmitra.platform.error;

import org.springframework.http.HttpStatus;

/**
 * Domain/application exception carrying a catalog error code.
 * Rendered as RFC 9457 Problem Details by {@link GlobalExceptionHandler}.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final String detail;

    public ApiException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
        this.detail = detail;
    }

    public ApiException(ErrorCode code) {
        this(code, code.title());
    }

    public ErrorCode code() { return code; }
    public String detail() { return detail; }
    public HttpStatus status() { return code.status(); }
}
