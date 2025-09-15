package com.sme.afs.service;

import com.sme.afs.config.BlobUrlProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service for validating filesystem capabilities required for blob URL functionality.
 * Performs validation on application startup to ensure hard link support.
 */
@Service
@Slf4j
public class FilesystemValidationService {

    private final BlobUrlProperties blobUrlProperties;
    private final HardLinkManager hardLinkManager;

    @Autowired
    public FilesystemValidationService(BlobUrlProperties blobUrlProperties,
                                     HardLinkManager hardLinkManager) {
        this.blobUrlProperties = blobUrlProperties;
        this.hardLinkManager = hardLinkManager;
    }

    /**
     * Validates filesystem support on application startup.
     * Checks hard link capability and directory permissions.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateFilesystemOnStartup() {
        if (!blobUrlProperties.isValidateFilesystemOnStartup()) {
            log.debug("Filesystem validation on startup is disabled");
            return;
        }

        log.info("Starting filesystem validation for blob URL functionality");
        
        try {
            ValidationResult result = validateFilesystem();
            
            if (result.isValid()) {
                log.info("Filesystem validation successful: {}", result.getMessage());
            } else {
                log.error("Filesystem validation failed: {}", result.getMessage());
                if (result.isCritical()) {
                    throw new IllegalStateException("Critical filesystem validation failure: " + result.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error during filesystem validation", e);
            throw new IllegalStateException("Filesystem validation failed", e);
        }
    }

    /**
     * Performs comprehensive filesystem validation.
     *
     * @return ValidationResult with details about the validation
     */
    public ValidationResult validateFilesystem() {
        try {
            Path tempDir = Paths.get(blobUrlProperties.getTempDirectory());
            
            // Validate temporary directory
            ValidationResult dirResult = validateTempDirectory(tempDir);
            if (!dirResult.isValid()) {
                return dirResult;
            }
            
            // Validate hard link support
            ValidationResult hardLinkResult = validateHardLinkSupport(tempDir);
            if (!hardLinkResult.isValid()) {
                return hardLinkResult;
            }
            
            // Validate filesystem permissions
            ValidationResult permissionResult = validatePermissions(tempDir);
            if (!permissionResult.isValid()) {
                return permissionResult;
            }
            
            return ValidationResult.success("Filesystem validation completed successfully");
            
        } catch (Exception e) {
            log.error("Unexpected error during filesystem validation", e);
            return ValidationResult.criticalFailure("Unexpected validation error: " + e.getMessage());
        }
    }

    /**
     * Validates that the temporary directory exists and is accessible.
     */
    private ValidationResult validateTempDirectory(Path tempDir) {
        try {
            // Create directory if it doesn't exist
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
                log.debug("Created temporary directory: {}", tempDir);
            }
            
            // Verify it's a directory
            if (!Files.isDirectory(tempDir)) {
                return ValidationResult.criticalFailure(
                    "Temporary path exists but is not a directory: " + tempDir);
            }
            
            // Check basic permissions
            if (!Files.isReadable(tempDir)) {
                return ValidationResult.criticalFailure(
                    "Temporary directory is not readable: " + tempDir);
            }
            
            if (!Files.isWritable(tempDir)) {
                return ValidationResult.criticalFailure(
                    "Temporary directory is not writable: " + tempDir);
            }
            
            log.debug("Temporary directory validation successful: {}", tempDir);
            return ValidationResult.success("Temporary directory is accessible");
            
        } catch (IOException e) {
            return ValidationResult.criticalFailure(
                "Failed to create or access temporary directory: " + e.getMessage());
        }
    }

    /**
     * Validates that the filesystem supports hard links.
     */
    private ValidationResult validateHardLinkSupport(Path tempDir) {
        Path testFile = null;
        Path testLink = null;
        
        try {
            // Create a test file
            String testId = UUID.randomUUID().toString();
            testFile = tempDir.resolve("hardlink-test-" + testId + ".tmp");
            testLink = tempDir.resolve("hardlink-test-link-" + testId + ".tmp");

            /* TODO: Validation writes test artifacts on every call; consider caching to reduce IO
             *
             * validateFilesystem() creates/deletes files each invocation (also via admin endpoint).
             * Cache a successful result for a short TTL (e.g., 1–5 minutes) to reduce churn.
             */
            // Write some test content
            Files.write(testFile, "hard link test".getBytes());
            
            // Attempt to create a hard link
            hardLinkManager.createHardLink(testFile, testLink);
            
            // Verify the hard link was created successfully
            if (!Files.exists(testLink)) {
                return ValidationResult.criticalFailure("Hard link was not created successfully");
            }
            
            // Verify both files have the same content
            byte[] originalContent = Files.readAllBytes(testFile);
            byte[] linkContent = Files.readAllBytes(testLink);
            
            if (!java.util.Arrays.equals(originalContent, linkContent)) {
                return ValidationResult.criticalFailure("Hard link content does not match original file");
            }
            
            // Verify they are actually hard links (same inode on Unix systems)
            try {
                Object originalKey = Files.getAttribute(testFile, "unix:ino");
                Object linkKey = Files.getAttribute(testLink, "unix:ino");
                
                if (originalKey != null && linkKey != null && !originalKey.equals(linkKey)) {
                    log.warn("Files may not be true hard links (different inodes), but functionality works");
                }
            } catch (Exception e) {
                // Ignore inode check failures on non-Unix systems
                log.debug("Could not verify inode equality (expected on non-Unix systems)");
            }
            
            log.debug("Hard link support validation successful");
            return ValidationResult.success("Hard link support confirmed");
            
        } catch (Exception e) {
            log.error("Hard link validation failed", e);
            return ValidationResult.criticalFailure(
                "Filesystem does not support hard links: " + e.getMessage());
        } finally {
            // Clean up test files
            cleanupTestFiles(testFile, testLink);
        }
    }

    /**
     * Validates filesystem permissions for blob URL operations.
     */
    private ValidationResult validatePermissions(Path tempDir) {
        Path testFile = null;
        
        try {
            // Test file creation and deletion
            String testId = UUID.randomUUID().toString();
            testFile = tempDir.resolve("permission-test-" + testId + ".tmp");
            
            // Create the test file
            Files.write(testFile, "permission test".getBytes());
            
            // Test read permission
            byte[] content = Files.readAllBytes(testFile);
            if (content.length == 0) {
                return ValidationResult.failure("Cannot read files in temporary directory");
            }
            
            // Test modification
            Files.write(testFile, "modified content".getBytes());
            
            // Test deletion
            Files.delete(testFile);
            testFile = null; // Mark as cleaned up
            
            log.debug("Permission validation successful");
            return ValidationResult.success("Filesystem permissions are adequate");
            
        } catch (IOException e) {
            return ValidationResult.failure(
                "Insufficient filesystem permissions: " + e.getMessage());
        } finally {
            // Clean up test file if it still exists
            if (testFile != null) {
                try {
                    Files.deleteIfExists(testFile);
                } catch (IOException e) {
                    log.warn("Failed to cleanup permission test file: {}", testFile, e);
                }
            }
        }
    }

    /**
     * Gets information about the filesystem where the temporary directory resides.
     *
     * @return FilesystemInfo with details about the filesystem
     */
    public FilesystemInfo getFilesystemInfo() {
        try {
            Path tempDir = Paths.get(blobUrlProperties.getTempDirectory());
            
            if (!Files.exists(tempDir)) {
                return FilesystemInfo.builder()
                        .tempDirectoryPath(tempDir.toString())
                        .exists(false)
                        .build();
            }
            
            java.nio.file.FileStore fileStore = Files.getFileStore(tempDir);
            
            return FilesystemInfo.builder()
                    .tempDirectoryPath(tempDir.toString())
                    .exists(true)
                    .fileSystemType(fileStore.type())
                    .totalSpace(fileStore.getTotalSpace())
                    .usableSpace(fileStore.getUsableSpace())
                    .unallocatedSpace(fileStore.getUnallocatedSpace())
                    .readOnly(fileStore.isReadOnly())
                    .supportsFileAttributeView(fileStore.supportsFileAttributeView("basic"))
                    .build();
                    
        } catch (IOException e) {
            log.error("Failed to get filesystem information", e);
            return FilesystemInfo.builder()
                    .tempDirectoryPath(blobUrlProperties.getTempDirectory())
                    .exists(false)
                    .error("Failed to access filesystem: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Cleans up test files, ignoring any errors.
     */
    private void cleanupTestFiles(Path... files) {
        for (Path file : files) {
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    log.debug("Failed to cleanup test file: {}", file, e);
                }
            }
        }
    }

    /**
     * Result of filesystem validation.
     */
    @lombok.Builder
    @lombok.Data
    public static class ValidationResult {
        private final boolean valid;
        private final boolean critical;
        private final String message;

        public static ValidationResult success(String message) {
            return ValidationResult.builder()
                    .valid(true)
                    .critical(false)
                    .message(message)
                    .build();
        }

        public static ValidationResult failure(String message) {
            return ValidationResult.builder()
                    .valid(false)
                    .critical(false)
                    .message(message)
                    .build();
        }

        public static ValidationResult criticalFailure(String message) {
            return ValidationResult.builder()
                    .valid(false)
                    .critical(true)
                    .message(message)
                    .build();
        }
    }

    /**
     * Information about the filesystem.
     */
    @lombok.Builder
    @lombok.Data
    public static class FilesystemInfo {
        private final String tempDirectoryPath;
        private final boolean exists;
        private final String fileSystemType;
        private final Long totalSpace;
        private final Long usableSpace;
        private final Long unallocatedSpace;
        private final Boolean readOnly;
        private final Boolean supportsFileAttributeView;
        private final String error;
    }
}