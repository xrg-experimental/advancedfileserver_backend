package com.sme.afs.exception;

import com.sme.afs.error.ErrorCode;

/**
 * Exception thrown when the filesystem doesn't support hard links
 */
public class FilesystemUnsupportedException extends BlobUrlException {
    
    public FilesystemUnsupportedException(String message) {
        super(ErrorCode.FILESYSTEM_UNSUPPORTED, message);
    }
    
    public FilesystemUnsupportedException(String message, Throwable cause) {
        super(ErrorCode.FILESYSTEM_UNSUPPORTED, message, cause);
    }
}