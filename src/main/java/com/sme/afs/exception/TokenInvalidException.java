package com.sme.afs.exception;

import com.sme.afs.error.ErrorCode;

/**
 * Exception thrown when a blob URL token is invalid or expired
 */
public class TokenInvalidException extends BlobUrlException {
    
    public TokenInvalidException(String token) {
        super(ErrorCode.TOKEN_INVALID, "Token is invalid or expired: " + token);
    }
    
    public TokenInvalidException(String message, Throwable cause) {
        super(ErrorCode.TOKEN_INVALID, message, cause);
    }
}