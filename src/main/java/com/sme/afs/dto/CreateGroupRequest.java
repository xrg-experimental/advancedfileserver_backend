package com.sme.afs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateGroupRequest {
    @NotBlank(message = "Group name is required")
    @Size(min = 3, max = 50, message = "Group name must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Group name can only contain letters, numbers, underscores and hyphens")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotBlank(message = "Base path is required")
    @Pattern(regexp = "^/[a-zA-Z0-9/_-]+$", message = "Base path must be a valid Unix-style path")
    private String basePath;
}
