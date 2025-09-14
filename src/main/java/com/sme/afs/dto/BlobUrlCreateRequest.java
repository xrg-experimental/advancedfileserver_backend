package com.sme.afs.dto;

import com.sme.afs.validation.SafePath;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlobUrlCreateRequest {

    @NotBlank(message = "File path is required")
    @Size(max = 1000, message = "File path must not exceed 1000 characters")
    @Pattern(regexp = "^[^\\r\\n\\x00]+$", message = "File path must not contain control characters")
    @SafePath(message = "File path is unsafe or contains traversal sequences")
    @Schema(description = "Path to the file (relative to FileService root)",
            example = "documents/report.pdf")
    private String filePath;
}