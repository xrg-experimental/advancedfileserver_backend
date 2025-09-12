package com.sme.afs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Service for managing filesystem hard links with cross-platform support.
 * Provides hard link creation, deletion, and validation operations.
 */
@Service
@Slf4j
public class HardLinkManager {

    /**
     * Creates a hard link from the source to the target path.
     * Both paths must be on the same filesystem for hard links to work.
     *
     * @param source Path to the existing file
     * @param target Path where the hard link should be created
     * @throws IOException                   if hard link creation fails
     * @throws UnsupportedOperationException if hard links are not supported
     * @throws IllegalArgumentException      if paths are on different filesystems
     */
    public void createHardLink(Path source, Path target) throws IOException {
        log.debug("Creating hard link from {} to {}", source, target);

        // Resolve real path without following symlinks and validate
        Path sourceReal;
        try {
            sourceReal = source.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new IOException("Source file does not exist or is not accessible: " + source, e);
        }
        if (!Files.isRegularFile(sourceReal, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Source is not a regular file (or is a symlink): " + sourceReal);
        }

        // Ensure target directory exists
        Path targetDir = target.getParent();
        if (targetDir != null && !Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
            log.debug("Created target directory: {}", targetDir);
        }

        // Validate that both paths are on the same filesystem
        if (!isOnSameFilesystem(source.toAbsolutePath().normalize(), target.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException(
                    "Cannot create hard link across different filesystems. Source: " +
                            source + ", Target: " + target);
        }

        // Remove target if it already exists – only regular files (no dirs), do not follow links
        if (Files.exists(target)) {
            if (Files.isDirectory(target)) {
                throw new IOException("Refusing to overwrite a directory: " + target);
            }
            Files.delete(target);
            log.debug("Removed existing target file: {}", target);
        }

        try {
            // Create the hard link
            Files.createLink(target, source);
            log.info("Successfully created hard link from {} to {}", source, target);
        } catch (UnsupportedOperationException e) {
            log.error("Hard links are not supported on this filesystem", e);
            throw new UnsupportedOperationException(
                    "Hard links are not supported on this filesystem", e);
        } catch (IOException e) {
            log.error("Failed to create hard link from {} to {}", source, target, e);
            throw new IOException("Failed to create hard link: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a hard link (not the original file).
     * This only removes the link, the original file remains intact.
     *
     * @param hardLinkPath Path to the hard link to delete
     * @throws IOException if deletion fails
     */
    public void deleteHardLink(Path hardLinkPath) throws IOException {
        log.debug("Deleting hard link: {}", hardLinkPath);

        if (!Files.exists(hardLinkPath)) {
            log.debug("Hard link does not exist, nothing to delete: {}", hardLinkPath);
            return;
        }

        try {
            if (Files.isDirectory(hardLinkPath, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Refusing to delete directory: " + hardLinkPath);
            }
            Files.delete(hardLinkPath);
            log.info("Successfully deleted hard link: {}", hardLinkPath);
        } catch (IOException e) {
            log.error("Failed to delete hard link: {}", hardLinkPath, e);
            throw new IOException("Failed to delete hard link: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the number of hard links for a given file.
     * Useful for debugging and validation.
     *
     * @param filePath Path to the file
     * @return Number of hard links to the file
     * @throws IOException if file attributes cannot be read
     */
    public int getHardLinkCount(Path filePath) throws IOException {
        try {
            // Prefer platform attribute if available
            try {
                Object nlink = Files.getAttribute(filePath, "unix:nlink", LinkOption.NOFOLLOW_LINKS);
                if (nlink instanceof Number) {
                    long count = ((Number) nlink).longValue();
                    return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
                }
            } catch (UnsupportedOperationException | IllegalArgumentException ignored) {
                // Not a UNIX-like filesystem; fall through
            }
            // Conservative default on non-UNIX platforms
            return 1;
        } catch (IOException e) {
            log.error("Failed to get hard link count for: {}", filePath, e);
            throw new IOException("Failed to get hard link count: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if two paths are on the same filesystem.
     * Hard links can only be created between files on the same filesystem.
     *
     * @param path1 First path to check
     * @param path2 Second path to check
     * @return true if both paths are on the same filesystem
     */
    public boolean isOnSameFilesystem(Path path1, Path path2) {
        try {
            // Get the file stores for both paths
            FileStore store1 = Files.getFileStore(path1.getParent() != null ? path1.getParent() : path1);
            FileStore store2 = Files.getFileStore(path2.getParent() != null ? path2.getParent() : path2);

            // Compare file stores
            boolean sameFilesystem = store1.equals(store2);
            log.debug("Filesystem check - Path1: {} ({}), Path2: {} ({}), Same: {}",
                    path1, store1.name(), path2, store2.name(), sameFilesystem);

            return sameFilesystem;
        } catch (IOException e) {
            log.warn("Failed to determine filesystem for paths {} and {}", path1, path2, e);
            // If we can't determine, assume they're on different filesystems for safety
            return false;
        }
    }

    /**
     * Validates that the filesystem supports hard links by attempting to create
     * and delete a test hard link.
     *
     * @param testDirectory Directory to use for testing hard link support
     * @throws IOException                   if filesystem validation fails
     * @throws UnsupportedOperationException if hard links are not supported
     */
    public void validateFilesystemSupport(Path testDirectory) throws IOException {
        log.debug("Validating hard link support in directory: {}", testDirectory);

        // Ensure test directory exists
        if (!Files.exists(testDirectory)) {
            Files.createDirectories(testDirectory);
        }

        // Create temporary test files
        Path testSource = testDirectory.resolve("hardlink_test_source.tmp");
        Path testTarget = testDirectory.resolve("hardlink_test_target.tmp");

        try {
            // Create a test source file
            Files.write(testSource, "test".getBytes());

            // Try to create a hard link
            Files.createLink(testTarget, testSource);

            // Verify the hard link was created successfully
            if (!Files.exists(testTarget)) {
                throw new IOException("Hard link was not created successfully");
            }

            // Verify both files have the same content
            byte[] sourceContent = Files.readAllBytes(testSource);
            byte[] targetContent = Files.readAllBytes(testTarget);
            if (!java.util.Arrays.equals(sourceContent, targetContent)) {
                throw new IOException("Hard link content does not match source");
            }

            log.info("Hard link support validated successfully in: {}", testDirectory);

        } catch (UnsupportedOperationException e) {
            log.error("Hard links are not supported in directory: {}", testDirectory, e);
            throw new UnsupportedOperationException(
                    "Hard links are not supported in directory: " + testDirectory, e);
        } finally {
            // Clean up test files
            try {
                if (Files.exists(testTarget)) {
                    Files.delete(testTarget);
                }
                if (Files.exists(testSource)) {
                    Files.delete(testSource);
                }
            } catch (IOException e) {
                log.warn("Failed to clean up test files", e);
            }
        }
    }
}