package com.sme.afs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SharedFolderConfigRequest {
    @NotBlank(message = "Path cannot be empty")
    private String path;
    
    @NotNull(message = "isBasePath must be specified")
    private Boolean isBasePath;
    
    // Note: isTempPath is no longer supported but kept for backward compatibility
    // Any request with isTempPath=true will be rejected
    private Boolean isTempPath = false;
}
