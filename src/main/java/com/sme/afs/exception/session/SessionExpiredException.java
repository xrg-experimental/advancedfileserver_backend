package com.sme.afs.exception.session;

import org.springframework.http.HttpStatus;

public class SessionExpiredException extends SessionException {
    public SessionExpiredException(String sessionId) {
        super(HttpStatus.UNAUTHORIZED, String.format("Session has expired: %s", sessionId));
    }
}
