package com.sme.afs.exception;

import com.sme.afs.error.ErrorCode;

/**
 * Exception thrown when attempting to create hard links across different filesystems
 */
public class CrossFilesystemException extends BlobUrlException {
    
    public CrossFilesystemException(String message) {
        super(ErrorCode.CROSS_FILESYSTEM, message);
    }
    
    public CrossFilesystemException(String message, Throwable cause) {
        super(ErrorCode.CROSS_FILESYSTEM, message, cause);
    }
}