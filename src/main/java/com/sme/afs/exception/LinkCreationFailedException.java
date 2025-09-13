package com.sme.afs.exception;

import com.sme.afs.error.ErrorCode;

/**
 * Exception thrown when hard link creation fails
 */
public class LinkCreationFailedException extends BlobUrlException {
    
    public LinkCreationFailedException(String message) {
        super(ErrorCode.LINK_CREATION_FAILED, message);
    }
    
    public LinkCreationFailedException(String message, Throwable cause) {
        super(ErrorCode.LINK_CREATION_FAILED, message, cause);
    }
}