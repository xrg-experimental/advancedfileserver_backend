package com.sme.afs.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SESSION_MAX_EXCEEDED("SESSION_MAX_EXCEEDED", "Too many sessions", HttpStatus.TOO_MANY_REQUESTS),
    SESSION_EXPIRED("SESSION_EXPIRED", "Session expired", HttpStatus.UNAUTHORIZED),
    SESSION_INVALID("SESSION_INVALID", "Invalid session", HttpStatus.UNAUTHORIZED),
    SESSION_NOT_FOUND("SESSION_NOT_FOUND", "Session not found", HttpStatus.NOT_FOUND),
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed", HttpStatus.BAD_REQUEST),
    ACCESS_DENIED("ACCESS_DENIED", "Access denied", HttpStatus.FORBIDDEN),
    NOT_FOUND("NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),
    ENDPOINT_NOT_FOUND("ENDPOINT_NOT_FOUND", "Endpoint not found", HttpStatus.NOT_FOUND),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    public final String code;
    public final String title;
    public final HttpStatus status;

    ErrorCode(String code, String title, HttpStatus status) {
        this.code = code;
        this.title = title;
        this.status = status;
    }
}