package com.sme.afs.util;

import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.io.File;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PathResolver {

    /**
     * Convert a logical path to a physical file system path.
     *
     * @param logicalPath The input logical path
     * @return Resolved absolute path
     * @throws InvalidPathException if path is invalid
     */
    public Path resolveLogicalPath(String logicalPath) {
        Assert.hasText(logicalPath, "Logical path must not be empty");

        try {
            // Normalize the path to handle '..' and '.'
            Path resolvedPath = Path.of(logicalPath).toAbsolutePath().normalize();

            // Additional security check to prevent path traversal
            if (!resolvedPath.startsWith(resolvedPath.getRoot())) {
                throw new InvalidPathException(logicalPath, "Path resolution resulted in an invalid location");
            }

            return resolvedPath;
        } catch (InvalidPathException e) {
            log.error("Invalid path resolution: {}", logicalPath, e);
            throw e;
        }
    }

    /**
     * Convert a physical path to a logical representation.
     *
     * @param physicalPath The input physical path
     * @return Logical path representation
     */
    public String convertToLogicalPath(Path physicalPath) {
        Assert.notNull(physicalPath, "Physical path must not be null");

        return physicalPath.toString().replace(File.separatorChar, '/');
    }

    /**
     * Validate if a path is absolute.
     *
     * @param path Path to check
     * @return true if path is absolute, false otherwise
     */
    public boolean isAbsolutePath(String path) {
        try {
            return convertToLogicalPath(Path.of(path)).startsWith("/");
        } catch (InvalidPathException e) {
            log.warn("Invalid path format: {}", path);
            return false;
        }
    }

    /**
     * Resolve a relative path against a base path.
     *
     * @param basePath Base directory path
     * @param relativePath Relative path to resolve
     * @return Resolved absolute path
     */
    public Path resolveRelativePath(Path basePath, String relativePath) {
        Assert.notNull(basePath, "Base path must not be null");
        Assert.hasText(relativePath, "Relative path must not be empty");

        return basePath.resolve(relativePath).normalize();
    }
}