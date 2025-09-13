package com.sme.afs.exception;

import com.sme.afs.error.ErrorCode;

/**
 * Exception thrown when a requested file is not found or not accessible
 */
public class FileNotFoundException extends BlobUrlException {
    
    public FileNotFoundException(String filePath) {
        super(ErrorCode.FILE_NOT_FOUND, "File not found or not accessible: " + filePath);
    }
    
    public FileNotFoundException(String filePath, Throwable cause) {
        super(ErrorCode.FILE_NOT_FOUND, "File not found or not accessible: " + filePath, cause);
    }
}