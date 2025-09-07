package com.sme.afs.error;

public interface HasErrorCode {
    ErrorCode getErrorCode();
    default String getDetail() { return null; }
}