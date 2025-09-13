package com.sme.afs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlobUrlResponse {
    
    @Schema(description = "Complete download URL for the file", 
            example = "https://api.example.com/api/blob-urls/downloads/abc123def456")
    private String downloadUrl;
    
    @Schema(description = "Unique token for this blob URL", 
            example = "abc123def456")
    private String token;
    
    @Schema(description = "Original filename", 
            example = "report.pdf")
    private String filename;
    
    @Schema(description = "File size in bytes", 
            example = "1048576")
    private Long fileSize;
    
    @Schema(description = "MIME type of the file", 
            example = "application/pdf")
    private String contentType;
    
    @Schema(description = "When the blob URL expires", 
            example = "2024-01-15T14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;
    
    @Schema(description = "Current status of the blob URL", 
            example = "active", 
            allowableValues = {"active", "expired", "invalid"})
    private String status;
}