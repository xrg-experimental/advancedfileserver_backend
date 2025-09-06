package com.sme.afs.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AfsException extends RuntimeException {
    private final HttpStatus status;
    private final String message;

    public AfsException(String message) {
        this(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public AfsException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.message = message;
    }
}
