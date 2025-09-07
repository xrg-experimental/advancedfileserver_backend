package com.sme.afs.exception.session;

import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;

public class SessionException extends AfsException {
    public SessionException(ErrorCode errorCode) {
        super(errorCode);
    }
}
