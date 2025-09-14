package com.sme.afs.service;

import com.sme.afs.config.BlobUrlProperties;
import com.sme.afs.dto.BlobUrlResponse;
import com.sme.afs.dto.FileInfoResponse;
import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.*;
import com.sme.afs.model.BlobUrl;
import com.sme.afs.repository.BlobUrlRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Core service for blob URL management.
 * Handles creation, validation, and retrieval of temporary download URLs using hard links.
 */
@Service
@Slf4j
public class BlobUrlService {

    private final BlobUrlRepository blobUrlRepository;
    private final TokenService tokenService;
    private final HardLinkManager hardLinkManager;
    private final FileService fileService;
    private final BlobUrlProperties blobUrlProperties;
    private final Clock clock;

    @Autowired
    public BlobUrlService(BlobUrlRepository blobUrlRepository,
                          TokenService tokenService,
                          HardLinkManager hardLinkManager,
                          FileService fileService,
                          BlobUrlProperties blobUrlProperties,
                          Clock clock) {
        this.blobUrlRepository = blobUrlRepository;
        this.tokenService = tokenService;
        this.hardLinkManager = hardLinkManager;
        this.fileService = fileService;
        this.blobUrlProperties = blobUrlProperties;
        this.clock = clock;
    }

    @Autowired(required = false)
    private RateLimitService rateLimitService;

    /**
     * Creates a temporary blob URL for the specified file.
     * Creates a hard link to the original file and returns URL information.
     * This overload gets the current user from the security context.
     *
     * @param filePath Path to the file (relative to FileService root)
     * @return BlobUrlResponse with download information
     * @throws FileNotFoundException if file validation fails
     * @throws LinkCreationFailedException if hard link creation fails
     */
    @Transactional
    public BlobUrlResponse createBlobUrl(String filePath) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = auth != null ? auth.getName() : "anonymous";
        BlobUrl blobUrl = createBlobUrl(filePath, createdBy);
        return convertToResponse(blobUrl);
    }

    /**
     * Creates a temporary blob URL for the specified file.
     * Creates a hard link to the original file and returns URL information.
     *
     * @param filePath Path to the file (relative to FileService root)
     * @param createdBy Username of the user creating the blob URL
     * @return BlobUrl entity with download information
     * @throws FileNotFoundException if file validation fails
     * @throws LinkCreationFailedException if hard link creation fails
     */
    @Transactional
    public BlobUrl createBlobUrl(String filePath, String createdBy) {
        log.info("Creating blob URL for file: {} by user: {}", filePath, createdBy);

        // Determine whether the provided path is absolute (e.g., from tests) or relative to FileService root
        Path originalPath;
        FileInfoResponse fileInfo;
        Path candidate = Paths.get(filePath);
        if (candidate.isAbsolute()) {
            // For absolute paths, read metadata directly from the filesystem to avoid FileService root restrictions
            try {
                originalPath = candidate.toAbsolutePath().normalize();

                // Validate that the path doesn't contain directory traversal attempts
                if (originalPath.toString().contains("..") || !originalPath.startsWith(originalPath.getRoot())) {
                    throw new FileNotFoundException("Invalid file path: " + filePath);
                }

                if (!Files.exists(originalPath)) {
                    throw new FileNotFoundException(filePath);
                }
                if (Files.isDirectory(originalPath)) {
                    throw new FileNotFoundException("Cannot create blob URL for directory: " + filePath);
                }

                FileInfoResponse info = new FileInfoResponse();
                info.setName(originalPath.getFileName().toString());
                info.setDirectory(false);
                info.setSize(Files.size(originalPath));
                try {
                    info.setMimeType(Files.probeContentType(originalPath));
                } catch (IOException ignore) {
                    // ignore mime resolution issues, default below
                }
                fileInfo = info;
            } catch (IOException e) {
                throw new FileNotFoundException("Failed to access file: " + filePath, e);
            }
        } else {
            // Validate file exists and get metadata through FileService for relative paths
            try {
                fileInfo = fileService.getFileInfo(filePath);
            } catch (AfsException e) {
                if (e.getErrorCode() == ErrorCode.NOT_FOUND) {
                    throw new FileNotFoundException(filePath, e);
                }
                throw e;
            }

            if (fileInfo.isDirectory()) {
                throw new FileNotFoundException("Cannot create blob URL for directory: " + filePath);
            }

            // Check concurrent URL limits early to avoid unnecessary resource loading
            validateConcurrentLimits();

            // Get the actual file path from FileService
            originalPath = getOriginalFilePath(filePath);
        }

        // Check concurrent URL limits
        validateConcurrentLimits();
        
        // Generate secure token and create the hard link path
        String token = tokenService.generateSecureToken();
        Path tempDir = Paths.get(blobUrlProperties.getTempDirectory());
        // Token must be URL-safe and free of path separators
        if (!token.matches("^[A-Za-z0-9_-]+$")) {
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Invalid token format");
        }
        Path hardLinkPath = tempDir.resolve(token);

        try {
            // Ensure temp directory exists (createDirectories is idempotent)
            Files.createDirectories(tempDir);
            log.debug("Ensured temporary directory exists: {}", tempDir);

            // Ensure original and temp directories are on the same filesystem
            try {
                if (!Files.getFileStore(originalPath).equals(Files.getFileStore(tempDir))) {
                    throw new CrossFilesystemException("Temporary directory must reside on the same filesystem as the original file");
                }
            } catch (IOException fsInfoEx) {
                log.warn("Unable to determine filesystem equality; proceeding to attempt hard link");
            }

            // Create the hard link
            hardLinkManager.createHardLink(originalPath, hardLinkPath);

            // Create and save blob URL entity
            BlobUrl blobUrl = BlobUrl.builder()
                    .token(token)
                    .originalPath(originalPath.toString())
                    .hardLinkPath(hardLinkPath.toString())
                    .filename(fileInfo.getName())
                    .contentType(fileInfo.getMimeType() != null ? fileInfo.getMimeType() : "application/octet-stream")
                    .fileSize(fileInfo.getSize())
                    .createdAt(OffsetDateTime.now(clock))
                    .expiresAt(OffsetDateTime.now(clock).plus(blobUrlProperties.getDefaultExpiration()))
                    .createdBy(createdBy)
                    .build();

            blobUrl = blobUrlRepository.save(blobUrl);
            log.info("Successfully created blob URL with token: {} for file: {}", token, filePath);
            
            return blobUrl;

        } catch (IOException e) {
            log.error("Failed to create hard link for file: {}", filePath, e);
            // Clean up any partial state
            cleanupFailedCreation(hardLinkPath, token);
            throw new LinkCreationFailedException("Failed to create temporary download link: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error creating blob URL for file: {}", filePath, e);
            cleanupFailedCreation(hardLinkPath, token);
            throw new LinkCreationFailedException("Failed to create blob URL", e);
        }
    }

    /**
     * Gets the status and metadata of a blob URL by token.
     * This overload returns a BlobUrlResponse for the controller.
     *
     * @param token The blob URL token
     * @return BlobUrlResponse with status and metadata
     * @throws TokenInvalidException if token is invalid or expired
     */
    @Transactional(readOnly = true)
    public BlobUrlResponse getBlobUrlStatus(String token) {
        Optional<BlobUrl> blobUrlOpt = getBlobUrlStatusInternal(token);
        if (blobUrlOpt.isEmpty()) {
            throw new TokenInvalidException(token);
        }
        return convertToResponse(blobUrlOpt.get());
    }

    /**
     * Gets the status and metadata of a blob URL by token.
     *
     * @param token The blob URL token
     * @return Optional containing the BlobUrl if found and valid, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<BlobUrl> getBlobUrlStatusInternal(String token) {
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
     * Validates a token and returns the file resource for download with rate limiting checks.
     * This overload should be used by controllers that can provide client IP and username.
     */
    @Transactional(readOnly = true)
    public Resource validateAndGetFile(String token, String clientIp, String username) {
        log.debug("Validating token and getting file for download: {} from IP: {}", token, clientIp);

        // Rate limiting checks (optional if service is available and enabled)
        try {
            if (rateLimitService != null && blobUrlProperties.getRateLimit().isEnabled()) {
                if (clientIp != null && !rateLimitService.isAllowed("download:ip:" + clientIp)) {
                    throw new AfsException(ErrorCode.TOO_MANY_REQUESTS,
                            "Rate limit exceeded for IP address. Please try again later.");
                }
                if (username != null && !username.isBlank() && !rateLimitService.isAllowed("download:user:" + username)) {
                    throw new AfsException(ErrorCode.TOO_MANY_REQUESTS,
                            "Rate limit exceeded for user. Please try again later.");
                }
                // Limit token validation attempts per IP to avoid brute force
                String key = clientIp != null ? clientIp : "unknown";
                if (!rateLimitService.isAllowed("token:validation:" + key)) {
                    throw new AfsException(ErrorCode.TOO_MANY_REQUESTS,
                            "Too many token validation attempts. Please try again later.");
                }
            }
        } catch (AfsException e) {
            // Re-throw to be handled by controller/exception handler
            throw e;
        } catch (Exception e) {
            // If rate limiter fails for any reason, don't block download but log it
            log.warn("Rate limiting check failed, allowing request to proceed: {}", e.getMessage());
        }

        // Delegate to existing validation logic
        return validateAndGetFile(token);
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

        Optional<BlobUrl> blobUrlOpt = getBlobUrlStatusInternal(token);
        if (blobUrlOpt.isEmpty()) {
            throw new TokenInvalidException(token);
        }

        BlobUrl blobUrl = blobUrlOpt.get();
        Path hardLinkPath = Paths.get(blobUrl.getHardLinkPath());

        // Verify hard link still exists
        if (!Files.exists(hardLinkPath)) {
            log.error("Hard link file not found: {}", hardLinkPath);
            throw new TokenInvalidException("Download file is no longer available");
        }

        try {
            // Read into memory to avoid OS-level file locking issues during cleanup (notably on Windows)
            byte[] data = Files.readAllBytes(hardLinkPath);
            org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(data) {
                @Override
                public String getFilename() {
                    return blobUrl.getFilename();
                }
            };
            log.info("Successfully validated token and prepared file for download: {}", token);
            return resource;
        } catch (IOException e) {
            log.error("Failed to prepare file for download: {}", hardLinkPath, e);
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to prepare file for download");
        }
    }

    /**
     * Cleans up expired blob URLs and their associated hard links.
     * This method is called by the cleanup scheduler.
     *
     * @return Number of cleaned-up URLs
     */
    @Transactional
    public int cleanupExpiredUrls() {
        log.debug("Starting cleanup of expired blob URLs");

        OffsetDateTime now = OffsetDateTime.now(clock);
        List<BlobUrl> expiredUrls = blobUrlRepository.findExpiredUrls(now);

        int cleanedCount = 0;
        List<String> failedTokens = new ArrayList<>();
        for (BlobUrl expiredUrl : expiredUrls) {
            try {
                // Delete the hard link first
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
                failedTokens.add(expiredUrl.getToken());
                // Continue with other URLs even if one fails
            }
        }

        if (cleanedCount > 0) {
            log.info("Cleaned up {} expired blob URLs", cleanedCount);
        }
        if (!failedTokens.isEmpty()) {
            log.warn("Failed to cleanup {} blob URLs: {}", failedTokens.size(), failedTokens);
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
        return blobUrlRepository.findActiveUrlsByUser(username, OffsetDateTime.now(clock));
    }

    /**
     * Gets the count of active blob URLs in the system.
     *
     * @return Total number of active blob URLs
     */
    @Transactional(readOnly = true)
    public long getActiveUrlCount() {
        return blobUrlRepository.countActiveUrls(OffsetDateTime.now(clock));
    }

    /**
     * Gets the count of active blob URLs for a specific user.
     *
     * @param username The username to count for
     * @return Number of active blob URLs for the user
     */
    @Transactional(readOnly = true)
    public long getActiveUrlCountByUser(String username) {
        return blobUrlRepository.countActiveUrlsByUser(username, OffsetDateTime.now(clock));
    }

    /**
     * Validates concurrent URL limits to prevent system overload.
     */
    private void validateConcurrentLimits() {
        long activeCount = blobUrlRepository.countActiveUrls(OffsetDateTime.now(clock));
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
        } catch (AfsException e) {
            // Re-throw AfsException as-is
            throw e;
        } catch (IOException e) {
            log.error("Failed to resolve original file path for: {}", filePath, e);
            // Don't expose internal paths in the error message
            throw new AfsException(ErrorCode.NOT_FOUND, "Requested file is not accessible");
        } catch (Exception e) {
            log.error("Unexpected error resolving file path", e);
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to process file request");
        }
    }

    /**
     * Converts a BlobUrl entity to a BlobUrlResponse DTO.
     */
    private BlobUrlResponse convertToResponse(BlobUrl blobUrl) {
        String status = tokenService.isTokenExpired(blobUrl) ? "expired" : "active";

        String basePath = blobUrlProperties.getDownloadUrlPath();
        if (!basePath.endsWith("/")) {
            basePath = basePath + "/";
        }
        String encodedToken = java.net.URLEncoder.encode(blobUrl.getToken(), java.nio.charset.StandardCharsets.UTF_8);
        return BlobUrlResponse.builder()
                .downloadUrl(basePath + encodedToken)
                .token(blobUrl.getToken())
                .filename(blobUrl.getFilename())
                .fileSize(blobUrl.getFileSize())
                .contentType(blobUrl.getContentType())
                .expiresAt(blobUrl.getExpiresAt())
                .status(status)
                .build();
    }

    /**
     * Cleans up any partial state from a failed blob URL creation.
     */
    private void cleanupFailedCreation(Path hardLinkPath, String token) {
        try {
            if (Files.exists(hardLinkPath)) {
                hardLinkManager.deleteHardLink(hardLinkPath);
            }
            // Also try to remove from the database if it was saved
            blobUrlRepository.deleteById(token);
        } catch (Exception cleanupError) {
            log.warn("Failed to cleanup after blob URL creation failure", cleanupError);
        }
    }
}