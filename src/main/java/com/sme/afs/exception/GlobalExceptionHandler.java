package com.sme.afs.exception;

import com.sme.afs.dto.ProblemResponse;
import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.session.*;
import com.sme.afs.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

    private ProblemResponse createProblem(ErrorCode errorCode, String detail) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID);
        return new ProblemResponse(
                "https://errors.afs/" + errorCode.code,
                errorCode.title,
                errorCode.status.value(),
                detail,
                "urn:uuid:" + correlationId,
                errorCode.code,
                correlationId
        );
    }

    @ExceptionHandler(SessionException.class)
    public ResponseEntity<ProblemResponse> handleSessionException(SessionException ex, HttpServletRequest request) {
        log.debug("Session exception occurred", ex);
        var problem = createProblem(ErrorCode.SESSION_INVALID, "The session is not valid.");
        return ResponseEntity.status(ErrorCode.SESSION_INVALID.status)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(MaxSessionsExceededException.class)
    public ResponseEntity<ProblemResponse> handleMaxSessionsExceededException(
            MaxSessionsExceededException ex, HttpServletRequest request) {
        log.warn("Max sessions exceeded", ex);
        var problem = createProblem(ErrorCode.SESSION_MAX_EXCEEDED, "Maximum number of concurrent sessions exceeded.");
        return ResponseEntity.status(ErrorCode.SESSION_MAX_EXCEEDED.status)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ProblemResponse> handleSessionExpiredException(
            SessionExpiredException ex, HttpServletRequest request) {
        log.debug("Session expired", ex);
        var problem = createProblem(ErrorCode.SESSION_EXPIRED, "The session has expired. Please authenticate again.");
        return ResponseEntity.status(ErrorCode.SESSION_EXPIRED.status)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(InvalidSessionException.class)
    public ResponseEntity<ProblemResponse> handleInvalidSessionException(
            InvalidSessionException ex, HttpServletRequest request) {
        log.debug("Invalid session", ex);
        var problem = createProblem(ErrorCode.SESSION_INVALID, "The session is not valid.");
        return ResponseEntity.status(ErrorCode.SESSION_INVALID.status)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ProblemResponse> handleSessionNotFoundException(
            SessionNotFoundException ex, HttpServletRequest request) {
        log.debug("Session not found", ex);
        var problem = createProblem(ErrorCode.SESSION_NOT_FOUND, "The session was not found.");
        return ResponseEntity.status(ErrorCode.SESSION_NOT_FOUND.status)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.debug("Validation failed", ex);
        var problem = createProblem(ErrorCode.VALIDATION_FAILED, "One or more fields failed validation.");
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(AfsException.class)
    public ResponseEntity<ProblemResponse> handleAfsException(AfsException ex, HttpServletRequest request) {
        log.error("Application exception occurred", ex);
        var problem = createProblem(ex.getErrorCode(), "An application error occurred.");
        return ResponseEntity.status(ex.getErrorCode().status)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ProblemResponse> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request) {
        log.debug("Access denied", ex);
        var problem = createProblem(ErrorCode.ACCESS_DENIED, "Access to the requested resource is denied.");
        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.status)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemResponse> handleNoResourceFoundException(NoResourceFoundException ignore, HttpServletRequest request) {
        log.debug("Resource not found");
        var problem = createProblem(ErrorCode.NOT_FOUND, "The requested resource was not found.");
        return ResponseEntity.status(ErrorCode.NOT_FOUND.status)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemResponse> handleNoHandlerFoundException(NoHandlerFoundException ignore, HttpServletRequest request) {
        log.debug("No handler found");
        var problem = createProblem(ErrorCode.ENDPOINT_NOT_FOUND, "The requested endpoint was not found.");
        return ResponseEntity.status(ErrorCode.ENDPOINT_NOT_FOUND.status)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        var problem = createProblem(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred.");
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status)
                .contentType(PROBLEM_JSON)
                .body(problem);
    }
}