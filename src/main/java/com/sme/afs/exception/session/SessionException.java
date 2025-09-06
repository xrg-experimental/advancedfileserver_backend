package com.sme.afs.exception.session;

import com.sme.afs.exception.AfsException;
import org.springframework.http.HttpStatus;

public class SessionException extends AfsException {
    public SessionException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public SessionException(HttpStatus status, String message) {
        super(status, message);
    }
}
