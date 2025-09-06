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
    
    @NotNull(message = "isTempPath must be specified")
    private Boolean isTempPath;
}
