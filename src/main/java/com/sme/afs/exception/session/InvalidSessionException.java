package com.sme.afs.exception.session;

import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;
import org.springframework.http.HttpStatus;

public class InvalidSessionException extends AfsException {
    public InvalidSessionException() {
        super(ErrorCode.SESSION_INVALID);
    }

    public InvalidSessionException(String message) {
        super(ErrorCode.SESSION_INVALID, message);
    }
}
