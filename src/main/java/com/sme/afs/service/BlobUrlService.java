package com.sme.afs.service;

import com.sme.afs.config.BlobUrlProperties;
import com.sme.afs.dto.FileInfoResponse;
import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.BlobUrl;
import com.sme.afs.repository.BlobUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Core service for blob URL management.
 * Handles creation, validation, and retrieval of temporary download URLs using hard links.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlobUrlService {

    private final BlobUrlRepository blobUrlRepository;
    private final TokenService tokenService;
    private final HardLinkManager hardLinkManager;
    private final FileService fileService;
    private final BlobUrlProperties blobUrlProperties;

    /**
     * Creates a temporary blob URL for the specified file.
     * Creates a hard link to the original file and returns URL information.
     *
     * @param filePath Path to the file (relative to FileService root)
     * @param createdBy Username of the user creating the blob URL
     * @return BlobUrl entity with download information
     * @throws AfsException if file validation or hard link creation fails
     */
    @Transactional
    public BlobUrl createBlobUrl(String filePath, String createdBy) {
        log.info("Creating blob URL for file: {} by user: {}", filePath, createdBy);

        // Validate file exists and get metadata through FileService
        FileInfoResponse fileInfo = fileService.getFileInfo(filePath);
        if (fileInfo.isDirectory()) {
            throw new AfsException(ErrorCode.VALIDATION_FAILED, "Cannot create blob URL for directory");
        }

        // Check concurrent URL limits
        validateConcurrentLimits();

        // Get the actual file path from FileService
        Path originalPath = getOriginalFilePath(filePath);
        
        // Generate secure token and create hard link path
        String token = tokenService.generateSecureToken();
        Path tempDir = Paths.get(blobUrlProperties.getTempDirectory());
        Path hardLinkPath = tempDir.resolve(token);

        try {
            // Ensure temp directory exists
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
                log.info("Created temporary directory: {}", tempDir);
            }

            // Create hard link
            hardLinkManager.createHardLink(originalPath, hardLinkPath);

            // Create and save blob URL entity
            BlobUrl blobUrl = BlobUrl.builder()
                    .token(token)
                    .originalPath(originalPath.toString())
                    .hardLinkPath(hardLinkPath.toString())
                    .filename(fileInfo.getName())
                    .contentType(fileInfo.getMimeType() != null ? fileInfo.getMimeType() : "application/octet-stream")
                    .fileSize(fileInfo.getSize())
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plus(blobUrlProperties.getDefaultExpiration()))
                    .createdBy(createdBy)
                    .build();

            blobUrl = blobUrlRepository.save(blobUrl);
            log.info("Successfully created blob URL with token: {} for file: {}", token, filePath);
            
            return blobUrl;

        } catch (IOException e) {
            log.error("Failed to create hard link for file: {}", filePath, e);
            // Clean up any partial state
            cleanupFailedCreation(hardLinkPath, token);
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to create temporary download link: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating blob URL for file: {}", filePath, e);
            cleanupFailedCreation(hardLinkPath, token);
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to create blob URL");
        }
    }

    /**
     * Gets the status and metadata of a blob URL by token.
     *
     * @param token The blob URL token
     * @return Optional containing the BlobUrl if found and valid, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<BlobUrl> getBlobUrlStatus(String token) {
        log.debug("Getting blob URL status for token: {}", token);

        if (!tokenService.validateTokenFormat(token)) {
            log.debug("Invalid token format: {}", token);
            return Optional.empty();
        }

        Optional<BlobUrl> blobUrlOpt = blobUrlRepository.findById(token);
        if (blobUrlOpt.isEmpty()) {
            log.debug("Blob URL not found for token: {}", token);
            return Optional.empty();
        }

        BlobUrl blobUrl = blobUrlOpt.get();
        if (tokenService.isTokenExpired(blobUrl)) {
            log.debug("Blob URL expired for token: {}", token);
            return Optional.empty();
        }

        return Optional.of(blobUrl);
    }

    /**
     * Validates a token and returns the file resource for download.
     * This method should be used by the download endpoint.
     *
     * @param token The blob URL token
     * @return Resource for the file download
     * @throws AfsException if token is invalid, expired, or file is not accessible
     */
    @Transactional(readOnly = true)
    public Resource validateAndGetFile(String token) {
        log.debug("Validating token and getting file for download: {}", token);

        Optional<BlobUrl> blobUrlOpt = getBlobUrlStatus(token);
        if (blobUrlOpt.isEmpty()) {
            throw new AfsException(ErrorCode.NOT_FOUND, "Download URL is invalid or expired");
        }

        BlobUrl blobUrl = blobUrlOpt.get();
        Path hardLinkPath = Paths.get(blobUrl.getHardLinkPath());

        // Verify hard link still exists
        if (!Files.exists(hardLinkPath)) {
            log.error("Hard link file not found: {}", hardLinkPath);
            throw new AfsException(ErrorCode.NOT_FOUND, "Download file is no longer available");
        }

        try {
            Resource resource = new UrlResource(hardLinkPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.error("Hard link file is not readable: {}", hardLinkPath);
                throw new AfsException(ErrorCode.INTERNAL_ERROR, "Download file is not accessible");
            }

            log.info("Successfully validated token and prepared file for download: {}", token);
            return resource;

        } catch (MalformedURLException e) {
            log.error("Failed to create resource for hard link: {}", hardLinkPath, e);
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to prepare file for download");
        }
    }

    /**
     * Cleans up expired blob URLs and their associated hard links.
     * This method is called by the cleanup scheduler.
     *
     * @return Number of cleaned up URLs
     */
    @Transactional
    public int cleanupExpiredUrls() {
        log.debug("Starting cleanup of expired blob URLs");

        LocalDateTime now = LocalDateTime.now();
        List<BlobUrl> expiredUrls = blobUrlRepository.findExpiredUrls(now);

        int cleanedCount = 0;
        for (BlobUrl expiredUrl : expiredUrls) {
            try {
                // Delete hard link first
                Path hardLinkPath = Paths.get(expiredUrl.getHardLinkPath());
                if (Files.exists(hardLinkPath)) {
                    hardLinkManager.deleteHardLink(hardLinkPath);
                }

                // Remove from database
                blobUrlRepository.delete(expiredUrl);
                cleanedCount++;
                
                log.debug("Cleaned up expired blob URL: {}", expiredUrl.getToken());

            } catch (Exception e) {
                log.error("Failed to cleanup expired blob URL: {}", expiredUrl.getToken(), e);
                // Continue with other URLs even if one fails
            }
        }

        if (cleanedCount > 0) {
            log.info("Cleaned up {} expired blob URLs", cleanedCount);
        }

        return cleanedCount;
    }

    /**
     * Gets active blob URLs for a specific user.
     *
     * @param username The username to search for
     * @return List of active blob URLs created by the user
     */
    @Transactional(readOnly = true)
    public List<BlobUrl> getActiveUrlsByUser(String username) {
        return blobUrlRepository.findActiveUrlsByUser(username, LocalDateTime.now());
    }

    /**
     * Gets the count of active blob URLs in the system.
     *
     * @return Total number of active blob URLs
     */
    @Transactional(readOnly = true)
    public long getActiveUrlCount() {
        return blobUrlRepository.countActiveUrls(LocalDateTime.now());
    }

    /**
     * Gets the count of active blob URLs for a specific user.
     *
     * @param username The username to count for
     * @return Number of active blob URLs for the user
     */
    @Transactional(readOnly = true)
    public long getActiveUrlCountByUser(String username) {
        return blobUrlRepository.countActiveUrlsByUser(username, LocalDateTime.now());
    }

    /**
     * Validates concurrent URL limits to prevent system overload.
     */
    private void validateConcurrentLimits() {
        long activeCount = getActiveUrlCount();
        if (activeCount >= blobUrlProperties.getMaxConcurrentUrls()) {
            throw new AfsException(ErrorCode.VALIDATION_FAILED, 
                "Maximum concurrent blob URLs limit reached: " + blobUrlProperties.getMaxConcurrentUrls());
        }
    }

    /**
     * Gets the original file path from FileService.
     * This method handles the path resolution logic specific to FileService.
     */
    private Path getOriginalFilePath(String filePath) {
        try {
            // Use FileService to load the resource and get the actual file path
            Resource resource = fileService.loadAsResource(filePath);
            return Paths.get(resource.getURI());
        } catch (Exception e) {
            log.error("Failed to resolve original file path for: {}", filePath, e);
            throw new AfsException(ErrorCode.NOT_FOUND, "File not found or not accessible");
        }
    }

    /**
     * Cleans up any partial state from a failed blob URL creation.
     */
    private void cleanupFailedCreation(Path hardLinkPath, String token) {
        try {
            if (Files.exists(hardLinkPath)) {
                hardLinkManager.deleteHardLink(hardLinkPath);
            }
            // Also try to remove from database if it was saved
            blobUrlRepository.deleteById(token);
        } catch (Exception cleanupError) {
            log.warn("Failed to cleanup after blob URL creation failure", cleanupError);
        }
    }
}