package com.sme.afs.exception.session;

import org.springframework.http.HttpStatus;

public class MaxSessionsExceededException extends SessionException {
    public MaxSessionsExceededException(String username, int maxSessions) {
        super(HttpStatus.TOO_MANY_REQUESTS, 
            String.format("User '%s' has exceeded maximum allowed sessions (%d)", username, maxSessions));
    }
}
