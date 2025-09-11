package com.sme.afs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class    RenameRequest {
    @NotBlank
    private String path;

    @NotBlank
    private String newName;
}
