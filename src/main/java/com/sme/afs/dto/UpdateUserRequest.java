package com.sme.afs.dto;

import com.sme.afs.model.UserType;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserRequest {
    @Email(message = "Invalid email format")
    private String email;
    
    private UserType userType;
    private Set<String> roles;
    private Set<String> groups;
    private Boolean enabled;
}
