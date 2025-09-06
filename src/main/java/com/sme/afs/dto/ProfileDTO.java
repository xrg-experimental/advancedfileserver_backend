package com.sme.afs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileDTO {
    
    @NotBlank(message = "Display name is required")
    @Size(min = 3, max = 50, message = "Display name must be between 3 and 50 characters")
    private String displayName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    private String username;
}
