package com.sme.afs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateGroupRequest {
    @NotBlank(message = "Base path is required")
    @Pattern(regexp = "^/[a-zA-Z0-9/_-]+$", message = "Base path must be a valid Unix-style path")
    private String basePath;
}
