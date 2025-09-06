package com.sme.afs.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

public class OtpAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public OtpAuthenticationToken(Object principal, Object credentials, String otpCode) {
        super(principal, credentials);
        setDetails(otpCode);
    }

    public OtpAuthenticationToken(Object principal, Object credentials, 
            Collection<? extends GrantedAuthority> authorities, String otpCode) {
        super(principal, credentials, authorities);
        setDetails(otpCode);
    }

    public String getOtpCode() {
        return getDetails().toString();
    }
}
