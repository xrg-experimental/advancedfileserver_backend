package com.sme.afs.exception.session;

import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;
import org.springframework.http.HttpStatus;

public class SessionException extends AfsException {
    public SessionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SessionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
