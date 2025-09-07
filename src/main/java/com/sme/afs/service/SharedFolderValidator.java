package com.sme.afs.service;

import com.sme.afs.config.SharedFolderProperties;
import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.SharedFolderValidation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedFolderValidator {
    
    private final SharedFolderProperties properties;

    private void validatePackageOwner() {
        if (!properties.isEnforcePackageOwner()) {
            log.warn("Package owner validation is disabled - this is not recommended for production!");
            return;
        }

        String configuredOwner = properties.getPackageOwner();
        String configuredOwnerFull = properties.getPackageOwnerFull();
        String currentUser = System.getProperty("user.name");
        
        if (configuredOwner != null && !configuredOwner.equals(currentUser)) {
            throw new AfsException(ErrorCode.ACCESS_DENIED,
                String.format("Application must run as configured package owner '%s' but is running as '%s'", 
                    configuredOwner, currentUser));
        }

        // Validate the base path is accessible by package owner
        if (!properties.getBasePath().isEmpty()) {
            String path = properties.getBasePath();
            Path basePath = Path.of(path);
            try {
                UserPrincipal owner = Files.getOwner(basePath);
                
                if (!owner.getName().equals(configuredOwnerFull)) {
                    throw new AfsException(ErrorCode.ACCESS_DENIED,
                        String.format("Path %s is owned by '%s' but must be owned by package owner '%s'",
                            path, owner.getName(), configuredOwnerFull));
                }

                // Verify full access for the owner
                checkFullAccessForOwner(path, basePath, currentUser);
            } catch (IOException e) {
                throw new AfsException(ErrorCode.ACCESS_DENIED,
                    String.format("Cannot verify package owner permissions for path %s: %s",
                        path, e.getMessage()));
            }
        }
    }

    private static void checkFullAccessForOwner(String path, Path basePath, String currentUser) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // For Windows, check basic file access permissions
            boolean canRead = Files.isReadable(basePath);
            boolean canWrite = Files.isWritable(basePath);
            boolean canExecute = Files.isExecutable(basePath);
            if (!canRead || !canWrite || !canExecute) {
                throw new AfsException(ErrorCode.ACCESS_DENIED,
                        String.format("Package owner '%s' must have full access to path: %s (Read: %b, Write: %b, Execute: %b)",
                                currentUser, path, canRead, canWrite, canExecute));
            }
        } else {
            // For UNIX, continue using POSIX permissions
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(basePath);
            boolean hasFullAccess = perms.contains(PosixFilePermission.OWNER_READ) &&
                    perms.contains(PosixFilePermission.OWNER_WRITE) &&
                    perms.contains(PosixFilePermission.OWNER_EXECUTE);
            if (!hasFullAccess) {
                throw new AfsException(ErrorCode.ACCESS_DENIED,
                        String.format("Package owner '%s' must have full access to path: %s",
                                currentUser, path));
            }
        }
    }

    public void validateConfiguration() {
        // Validate package owner first
        validatePackageOwner();

        // Validate the base path
        validatePath(properties.getBasePath());
        if (properties.isCreateMissingDirectories()) {
            validateRequiredStructure(properties.getBasePath());
        }
    }

    private void validateRequiredStructure(String basePath) {
        Path base = Path.of(basePath);
        String[] requiredDirs = {
            "groups",
            "system/acls",
            "system/metadata",
            "system/share_links",
            "logs"
        };

        for (String dir : requiredDirs) {
            Path path = base.resolve(dir);
            try {
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                    log.info("Created required directory: {}", path);
                }
                if (!Files.isDirectory(path)) {
                    throw new AfsException(ErrorCode.VALIDATION_FAILED,
                        "Required path exists but is not a directory: " + path);
                }
                // Verify permissions
                if (!Files.isReadable(path) || !Files.isWritable(path)) {
                    throw new AfsException(ErrorCode.VALIDATION_FAILED,
                        "Required directory lacks proper permissions: " + path);
                }
                log.debug("Validated required directory: {}", path);
            } catch (IOException e) {
                throw new AfsException(ErrorCode.INTERNAL_ERROR,
                    "Failed to create/validate required directory " + path + ": " + e.getMessage());
            }
        }
    }

    public SharedFolderValidation validatePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new AfsException(ErrorCode.VALIDATION_FAILED, "path cannot be empty");
        }

        Path resolvedPath = Path.of(path);
        try {
            // Normalize and get the absolute path
            Path normalizedPath = validateAndNormalizePath(path);

            // Security checks: reject any ".." segment in the raw input
            for (Path seg : resolvedPath) {
                if ("..".equals(seg.toString())) {
                    throw new AfsException(ErrorCode.VALIDATION_FAILED, "Path traversal is not allowed");
                }
            }

            // Existence check
            if (!Files.exists(normalizedPath)) {
                throw new AfsException(ErrorCode.NOT_FOUND, "path does not exist: " + normalizedPath);
            }

            // Directory check
            if (!Files.isDirectory(normalizedPath)) {
                throw new AfsException(ErrorCode.VALIDATION_FAILED, "path must be a directory");
            }

            // Permission checks
            if (!Files.isReadable(normalizedPath)) {
                throw new AfsException(ErrorCode.ACCESS_DENIED, "path is not readable");
            }
            
            if (!Files.isWritable(normalizedPath)) {
                throw new AfsException(ErrorCode.ACCESS_DENIED, "path is not writable");
            }
            
            if (!Files.isExecutable(normalizedPath)) {
                throw new AfsException(ErrorCode.ACCESS_DENIED, "path is not accessible");
            }

            // Verify we can list contents
            try {
                Files.list(normalizedPath).close();
            } catch (SecurityException e) {
                throw new AfsException(ErrorCode.ACCESS_DENIED, "path directory listing denied");
            }
        } catch (SecurityException e) {
            throw new AfsException(ErrorCode.ACCESS_DENIED,
                "Security error accessing path: " + e.getMessage());
        } catch (IOException e) {
            throw new AfsException(ErrorCode.INTERNAL_ERROR,
                "IO error validating: " + e.getMessage());
        }

        try {
            Path normalizedPath = resolvedPath.normalize().toRealPath();
            if (!Files.exists(normalizedPath)) {
                throw new AfsException(ErrorCode.NOT_FOUND, "path does not exist: " + normalizedPath);
            }
        
            SharedFolderValidation validation = new SharedFolderValidation();
            validation.setLastCheckedAt(LocalDateTime.now());
        
            try {
                validation.setCanRead(Files.isReadable(normalizedPath));
                validation.setCanWrite(Files.isWritable(normalizedPath));
                validation.setCanExecute(Files.isExecutable(normalizedPath));
            
                if (!Files.isDirectory(normalizedPath)) {
                    validation.setValid(false);
                    validation.setErrorMessage("path is not a directory");
                    validation.setPermissionCheckError("Path must be a directory");
                    return validation;
                }
            
                if (!validation.getCanRead() || !validation.getCanWrite() || !validation.getCanExecute()) {
                    validation.setValid(false);
                    validation.setErrorMessage("path has insufficient permissions");
                    validation.setPermissionCheckError(
                        String.format("Required permissions missing - Read: %b, Write: %b, Execute: %b",
                            validation.getCanRead(), validation.getCanWrite(), validation.getCanExecute()));
                    return validation;
                }
            
                // Verify we can list contents
                try (var listing = Files.list(normalizedPath)) {
                    //noinspection ResultOfMethodCallIgnored
                    listing.findFirst(); // Try to read at least one entry
                    validation.setValid(true);
                } catch (SecurityException e) {
                    validation.setValid(false);
                    validation.setErrorMessage("directory listing denied");
                    validation.setPermissionCheckError("Cannot list directory contents: " + e.getMessage());
                    return validation;
                }
            
            } catch (SecurityException e) {
                validation.setValid(false);
                validation.setErrorMessage("Security error accessing path");
                validation.setPermissionCheckError(e.getMessage());
            } catch (IOException e) {
                validation.setValid(false);
                validation.setErrorMessage("IO error validating path");
                validation.setPermissionCheckError(e.getMessage());
            }
        
            return validation;
        } catch (IOException e) {
            throw new AfsException(ErrorCode.VALIDATION_FAILED, "Error validating path: " + e.getMessage());
        }
    }

    public static Path validateAndNormalizePath(String pathStr) throws IOException {
        Path path = Path.of(pathStr).normalize().toAbsolutePath();

        // Ensure NOFOLLOW_LINKS option is used when resolving the path
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

        // Optionally add additional file attribute checks here if required
        if (attrs.isSymbolicLink()) {
            throw new IOException("Path traversal via symbolic link is not allowed: " + path);
        }

        return path;
    }
}
