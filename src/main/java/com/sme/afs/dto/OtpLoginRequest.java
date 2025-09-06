package com.sme.afs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpLoginRequest {
    @NotBlank
    private String username;
    
    private String password;
    
    @NotBlank
    private String otpCode;
}
