package com.sme.afs.exception.session;

import org.springframework.http.HttpStatus;

public class SessionNotFoundException extends SessionException {
    public SessionNotFoundException(String sessionId) {
        super(HttpStatus.NOT_FOUND, String.format("Session not found: %s", sessionId));
    }
}
