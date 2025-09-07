package com.sme.afs.exception.session;

import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;
import org.springframework.http.HttpStatus;

public class MaxSessionsExceededException extends AfsException {
    public MaxSessionsExceededException() {
        super(ErrorCode.SESSION_MAX_EXCEEDED);
    }

    public MaxSessionsExceededException(String message) {
        super(ErrorCode.SESSION_MAX_EXCEEDED, message);
    }
}
