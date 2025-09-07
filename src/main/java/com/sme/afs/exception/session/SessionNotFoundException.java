package com.sme.afs.exception.session;

import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;
import org.springframework.http.HttpStatus;

public class SessionNotFoundException extends AfsException {
    public SessionNotFoundException() {
        super(ErrorCode.SESSION_NOT_FOUND);
    }

    public SessionNotFoundException(String message) {
        super(ErrorCode.SESSION_NOT_FOUND, message);
    }
}
