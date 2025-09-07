package com.sme.afs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SharedFolderConfigRequest {
    @NotBlank(message = "Path cannot be empty")
    private String path;
}
