package com.sme.afs.exception.session;

import org.springframework.http.HttpStatus;

public class InvalidSessionException extends SessionException {
    public InvalidSessionException(String sessionId) {
        super(HttpStatus.UNAUTHORIZED, String.format("Invalid session: %s", sessionId));
    }
}
