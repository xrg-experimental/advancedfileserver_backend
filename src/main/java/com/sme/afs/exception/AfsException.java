package com.sme.afs.exception;

import com.sme.afs.error.ErrorCode;
import com.sme.afs.error.HasErrorCode;
import org.springframework.http.HttpStatus;

public class AfsException extends RuntimeException implements HasErrorCode {
    private final ErrorCode errorCode;

    public AfsException(ErrorCode errorCode) {
        super();
        this.errorCode = errorCode;
    }

    public AfsException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AfsException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return errorCode.status;
    }
}