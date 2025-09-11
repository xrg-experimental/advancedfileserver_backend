package com.sme.afs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PathRequest {
    @Schema(description = "Filesystem path", example = "/home/user/docs")
    private String path;
}
