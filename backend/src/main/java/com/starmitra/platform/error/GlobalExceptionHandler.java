package com.starmitra.platform.error;

import com.starmitra.platform.correlation.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Single RFC 9457 renderer for the whole monolith — every module throws
 * {@link ApiException} (or bean-validation errors); nothing else escapes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApi(ApiException ex, HttpServletRequest req) {
        return build(ex.code(), ex.detail(), null, req);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            org.springframework.http.HttpHeaders headers, org.springframework.http.HttpStatusCode status,
            org.springframework.web.context.request.WebRequest request) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::fieldError).toList();
        ProblemDetail pd = problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.title());
        pd.setProperty("errors", fieldErrors);
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status()).body(pd);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);           // correlationId in MDC
        return build(ErrorCode.INTERNAL_ERROR, "Internal error", null, req);
    }

    private Map<String, String> fieldError(FieldError fe) {
        return Map.of("field", fe.getField(), "code", String.valueOf(fe.getCode()),
                "message", String.valueOf(fe.getDefaultMessage()));
    }

    private ResponseEntity<ProblemDetail> build(ErrorCode code, String detail,
            List<Map<String, String>> errors, HttpServletRequest req) {
        ProblemDetail pd = problem(code, detail);
        if (errors != null) pd.setProperty("errors", errors);
        return ResponseEntity.status(code.status()).body(pd);
    }

    private ProblemDetail problem(ErrorCode code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.status(), detail);
        pd.setType(URI.create(code.typeUri()));
        pd.setTitle(code.title());
        pd.setProperty("code", code.name());
        pd.setProperty("correlationId", CorrelationIdFilter.current());
        return pd;
    }
}
