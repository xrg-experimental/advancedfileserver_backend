package com.sme.afs.exception;

import org.springframework.security.core.AuthenticationException;

public class OtpRequiredException extends AuthenticationException {
    public OtpRequiredException(String msg) {
        super(msg);
    }
}
