package com.clausify.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions into RFC 9457 {@link ProblemDetail} JSON so every error has the same shape:
 * {@code {"type", "title", "status", "detail", "instance"}} plus extra properties where useful.
 * <p>
 * The parent class already maps Spring MVC's own exceptions (bad JSON, wrong method, and so on);
 * this class adds the domain exceptions and customizes messages users actually see.
 * Responses never include stack traces or internal details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    ProblemDetail handleUnsupportedFileType(UnsupportedFileTypeException ex) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
    }

    @ExceptionHandler(UnreadablePdfException.class)
    ProblemDetail handleUnreadablePdf(UnreadablePdfException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    /** 401: reached through ProblemDetailAuthenticationEntryPoint (missing, expired, or invalid token). */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentication is required.");
    }

    /** 403: authenticated but not allowed. Ownership checks use 404 instead (see NotFoundException). */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.");
    }

    /** Last resort: log the real cause for us, return a generic 500 to the client. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    /** 400 with one message per invalid field, e.g. {@code "errors": {"password": "..."}}. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /** 413 when an upload exceeds spring.servlet.multipart.max-file-size (10 MB). */
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, problem(HttpStatus.CONTENT_TOO_LARGE, "Files must be 10 MB or smaller."),
                headers, status, request);
    }

    /** 415 when the request's Content-Type itself is not accepted (e.g. JSON sent to the upload endpoint). */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported content type."),
                headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
