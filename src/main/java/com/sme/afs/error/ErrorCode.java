package com.sme.afs.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SESSION_MAX_EXCEEDED("SESSION_MAX_EXCEEDED", "Too many sessions", HttpStatus.TOO_MANY_REQUESTS),
    SESSION_EXPIRED("SESSION_EXPIRED", "Session expired", HttpStatus.UNAUTHORIZED),
    SESSION_INVALID("SESSION_INVALID", "Invalid session", HttpStatus.UNAUTHORIZED),
    SESSION_NOT_FOUND("SESSION_NOT_FOUND", "Session not found", HttpStatus.NOT_FOUND),
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed", HttpStatus.BAD_REQUEST),
    CONFLICT("CONFLICT", "Conflict", HttpStatus.CONFLICT),
    ACCESS_DENIED("ACCESS_DENIED", "Access denied", HttpStatus.FORBIDDEN),
    NOT_FOUND("NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),
    ENDPOINT_NOT_FOUND("ENDPOINT_NOT_FOUND", "Endpoint not found", HttpStatus.NOT_FOUND),
    TOO_MANY_REQUESTS("TOO_MANY_REQUESTS", "Too many requests", HttpStatus.TOO_MANY_REQUESTS),
    
    // Blob URL specific errors
    FILE_NOT_FOUND("FILE_NOT_FOUND", "File not found or not accessible", HttpStatus.NOT_FOUND),
    FILESYSTEM_UNSUPPORTED("FILESYSTEM_UNSUPPORTED", "Hard links not supported on this filesystem", HttpStatus.INTERNAL_SERVER_ERROR),
    CROSS_FILESYSTEM("CROSS_FILESYSTEM", "Cannot create hard link across filesystems", HttpStatus.BAD_REQUEST),
    TOKEN_INVALID("TOKEN_INVALID", "Download URL is invalid or expired", HttpStatus.NOT_FOUND),
    LINK_CREATION_FAILED("LINK_CREATION_FAILED", "Failed to create temporary download link", HttpStatus.INTERNAL_SERVER_ERROR),
    
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