package com.sme.afs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for blob URL operations.
 * Contains all information needed by clients to download files via temporary URLs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlobUrlResponse {
    
    /**
     * Full URL for downloading the file
     */
    private String downloadUrl;
    
    /**
     * Token for status checking and identification
     */
    private String token;
    
    /**
     * Original filename for proper download handling
     */
    private String filename;
    
    /**
     * File size in bytes for progress tracking
     */
    private Long fileSize;
    
    /**
     * MIME type for proper content handling
     */
    private String contentType;
    
    /**
     * When the URL expires
     */
    private LocalDateTime expiresAt;
    
    /**
     * Current status of the URL (active, expired, invalid)
     */
    private String status;
    
    /**
     * When the URL was created
     */
    private LocalDateTime createdAt;
    
    /**
     * User who created the URL
     */
    private String createdBy;
}