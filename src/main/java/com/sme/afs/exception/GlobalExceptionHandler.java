package com.sme.afs.exception;

import com.sme.afs.dto.ErrorResponse;
import com.sme.afs.exception.session.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SessionException.class)
    public ResponseEntity<ErrorResponse> handleSessionException(SessionException ex, HttpServletRequest request) {
        log.debug("Session exception occurred: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getStatus().value(),
            ex.getStatus().getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(MaxSessionsExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSessionsExceededException(
            MaxSessionsExceededException ex, HttpServletRequest request) {
        log.warn("Max sessions exceeded: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getStatus().value(),
            "Too Many Sessions",
            ex.getMessage(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ErrorResponse> handleSessionExpiredException(
            SessionExpiredException ex, HttpServletRequest request) {
        log.debug("Session expired: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getStatus().value(),
            "Session Expired",
            ex.getMessage(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(InvalidSessionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSessionException(
            InvalidSessionException ex, HttpServletRequest request) {
        log.debug("Invalid session: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getStatus().value(),
            "Invalid Session",
            ex.getMessage(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFoundException(
            SessionNotFoundException ex, HttpServletRequest request) {
        log.debug("Session not found: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getStatus().value(),
            "Session Not Found",
            ex.getMessage(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
            .map(this::mapFieldErrorToString)
            .collect(Collectors.joining("; "));

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            errors,
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    private String mapFieldErrorToString(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    @ExceptionHandler(AfsException.class)
    public ResponseEntity<ErrorResponse> handleAfsException(AfsException ex, HttpServletRequest request) {
        log.error("Application exception occurred: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getStatus().value(),
            ex.getStatus().getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex, 
            HttpServletRequest request) {
        log.debug("Access denied: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            HttpStatus.FORBIDDEN.getReasonPhrase(),
            "Access Denied",
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // Handle static resource requests that don't exist (e.g., /api/ or /api/swagger-ui.html when resources are not present)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.debug("Resource not found: {}", request.getRequestURI());
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            "Resource not found",
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        ErrorResponse errorResponse = new ErrorResponse(
            500,
            "Internal Server Error",
            "An unexpected error occurred",
            request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(errorResponse);
    }

    // Inner class for validation errors
    public record ValidationError(String field, String message) {
    }
}
