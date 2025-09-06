package com.sme.afs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String userType;
    private long refreshWindowStart;
    private long refreshWindowEnd;
    private boolean otpRequired;
}
