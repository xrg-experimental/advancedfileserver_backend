package com.sme.afs.exception;

import com.sme.afs.error.ErrorCode;

/**
 * Base exception for blob URL operations
 */
public class BlobUrlException extends AfsException {
    
    public BlobUrlException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public BlobUrlException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public BlobUrlException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}