package com.sme.afs.config;

import com.sme.afs.service.HardLinkManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates Blob URL filesystem invariants at application startup.
 * <p>
 * - Optionally validates that the temp directory's filesystem supports hard links
 *   (controlled by afs.blob-urls.validateFilesystemOnStartup).
 * - Always ensures that shared-folder.base-path and afs.blob-urls.temp-directory
 *   are located on the same filesystem because hard links cannot cross filesystems.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlobUrlStartupValidator implements ApplicationRunner {

    private final BlobUrlProperties blobUrlProperties;
    private final SharedFolderProperties sharedFolderProperties;
    private final HardLinkManager hardLinkManager;

    @Override
    public void run(ApplicationArguments args) {
        final String tempDirStr = blobUrlProperties.getTempDirectory();
        final String basePathStr = sharedFolderProperties.getBasePath();
        if (tempDirStr == null || tempDirStr.isBlank()) {
            throw new IllegalStateException("afs.blob-urls.temp-directory is not configured");
        }

        final Path tempDir = Path.of(tempDirStr).normalize().toAbsolutePath();
        if (Files.exists(tempDir) && Files.isSymbolicLink(tempDir)) {
            throw new IllegalStateException("afs.blob-urls.temp-directory must not be a symbolic link: " + tempDir);
        }
        final Path basePath = (basePathStr != null && !basePathStr.isBlank())
                ? Path.of(basePathStr).normalize().toAbsolutePath()
                : null;
        if (basePath != null && Files.exists(basePath) && Files.isSymbolicLink(basePath)) {
            throw new IllegalStateException("shared-folder.base-path must not be a symbolic link: " + basePath);
        }

        // Validate hard link support in the temp directory if enabled
        if (blobUrlProperties.isValidateFilesystemOnStartup()) {
            try {
                hardLinkManager.validateFilesystemSupport(tempDir);
                log.info("Validated hard link support in blob temp directory: {}", tempDir);
            } catch (UnsupportedOperationException | IOException e) {
                throw new IllegalStateException("Filesystem does not support required hard link operations in temp directory: " + tempDir, e);
            }
        }

        // Always check that temp directory and shared folder base path are on the same filesystem
        if (basePath == null) {
            // If the base path is not configured, we cannot validate cross-filesystem constraint here.
            // SharedFolderValidator/Config will handle its own validations.
            log.warn("shared-folder.base-path is not configured; skipping filesystem sameness check with blob temp directory.");
            return;
        }

        boolean sameFs = hardLinkManager.isOnSameFilesystem(basePath, tempDir);
        if (!sameFs) {
            throw new IllegalStateException(
                "Configuration error: shared-folder.base-path (" + basePath + ") and afs.blob-urls.temp-directory (" + tempDir + ") are on different filesystems. " +
                "Hard links cannot be created across filesystems. Configure both paths on the same filesystem.");
        }
        log.info("Blob URL temp directory and shared folder base path are on the same filesystem: basePath={}, tempDir={}", basePath, tempDir);
    }
}
