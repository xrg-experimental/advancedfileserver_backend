package com.sme.afs.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateGroupPermissionsRequest {
    @NotNull
    private Boolean canRead;
    @NotNull
    private Boolean canWrite;
    @NotNull
    private Boolean canDelete;
    @NotNull
    private Boolean canShare;
    @NotNull
    private Boolean canUpload;
}
