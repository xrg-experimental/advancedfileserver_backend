package com.sme.afs.model;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Entity representing a temporary blob URL for file downloads.
 * Uses hard links to provide secure, temporary access to files without copying.
 */
@Entity
@Table(
        name = "blob_urls",
        indexes = {
                @Index(name = "idx_blob_urls_expires_at", columnList = "expires_at"),
                @Index(name = "idx_blob_urls_created_by", columnList = "created_by")
        }
)
@lombok.Getter
@lombok.Setter
@lombok.ToString(exclude = {"token", "originalPath", "hardLinkPath", "contentType"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlobUrl {

    /**
     * Primary key - cryptographically secure random token
     */
    @Id
    @Column(name = "token", length = 64, nullable = false)
    private String token;

    /**
     * Path to the original file in the filesystem
     */
    @Column(name = "original_path", nullable = false, length = 1000)
    private String originalPath;

    /**
     * Path to the hard link file in the temporary directory
     */
    @Column(name = "hard_link_path", nullable = false, length = 1000)
    private String hardLinkPath;

    /**
     * Original filename for proper Content-Disposition headers
     */
    @jakarta.validation.constraints.Size(max = 255)
    @jakarta.validation.constraints.Pattern(regexp = "^[^\\r\\n\\\\/]+$", message = "filename must not contain path separators or control characters")
    @Column(name = "filename", nullable = false)
    private String filename;

    /**
     * MIME type for proper Content-Type headers
     */
    @jakarta.validation.constraints.Pattern(regexp = "^[\\w.+-]+/[\\w.+-]+$", message = "contentType must be a valid MIME type")
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /**
     * File size in bytes for Content-Length headers
     */
    @jakarta.validation.constraints.PositiveOrZero
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * Timestamp when the blob URL was created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the blob URL expires
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Username of the user who created this blob URL
     */
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    /**
     * Check if this blob URL has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Set creation timestamp to current time
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}