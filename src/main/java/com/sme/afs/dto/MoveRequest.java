package com.sme.afs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveRequest {
    @Schema(description = "Source path", example = "/home/user/docs/file.txt")
    private String sourcePath;

    @Schema(description = "Target path", example = "/home/user/archive/file.txt")
    private String targetPath;
}
