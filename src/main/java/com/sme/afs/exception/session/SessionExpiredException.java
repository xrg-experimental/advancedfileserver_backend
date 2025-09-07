package com.sme.afs.exception.session;

import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;

public class SessionExpiredException extends AfsException {
    public SessionExpiredException() {
        super(ErrorCode.SESSION_EXPIRED);
    }

    public SessionExpiredException(String message) {
        super(ErrorCode.SESSION_EXPIRED, message);
    }
}