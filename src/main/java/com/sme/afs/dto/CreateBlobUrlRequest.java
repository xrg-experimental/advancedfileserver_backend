package com.sme.afs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating blob URLs.
 * Contains the file path for which a temporary download URL should be created.
 */
@Data
public class CreateBlobUrlRequest {
    
    /**
     * Path to the file for which to create a blob URL.
     * Should be relative to the FileService root directory.
     */
    @NotBlank(message = "File path is required")
    @Size(max = 1000, message = "File path must not exceed 1000 characters")
    @Pattern(regexp = "^[^\\r\\n\\x00]+$", message = "File path must not contain control characters")
    private String filePath;
}