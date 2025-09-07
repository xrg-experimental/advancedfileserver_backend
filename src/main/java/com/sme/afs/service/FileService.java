package com.sme.afs.service;

import com.sme.afs.config.SharedFolderConfig;
import com.sme.afs.dto.FileInfoResponse;
import com.sme.afs.dto.FileListResponse;
import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class FileService {

    private final Path rootLocation;

    public FileService(SharedFolderConfig sharedFolderConfig) {
        this.rootLocation = Path.of(sharedFolderConfig.getBasePath()).toAbsolutePath().normalize();
    }

    public FileInfoResponse createDirectory(String path) {
        try {
            Path dirPath = getAbsolutePath(path);
            validatePath(dirPath);

            if (Files.exists(dirPath)) {
                throw new AfsException(ErrorCode.VALIDATION_FAILED, "Directory already exists");
            }

            Files.createDirectories(dirPath);
            return createFileInfo(dirPath);
        } catch (IOException e) {
            log.error("Failed to create directory {}: {}", path, e, e);
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to create directory");
        }
    }

    public void delete(String path) {
        try {
            Path filePath = getAbsolutePath(path);
            validatePath(filePath);

            if (Files.isDirectory(filePath)) {
                FileSystemUtils.deleteRecursively(filePath);
            } else {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to delete: " + e.getMessage());
        }
    }

    public FileInfoResponse getFileInfo(String path) {
        Path filePath = getAbsolutePath(path);
        validatePath(filePath);
        if (!Files.exists(filePath, LinkOption.NOFOLLOW_LINKS)) {
            throw new AfsException(ErrorCode.NOT_FOUND, "File not found");
        }
        return createFileInfo(filePath);
    }

    public FileListResponse listDirectory(String path) {
        Path dirPath = getAbsolutePath(path);
        validatePath(dirPath);

        try (Stream<Path> stream = Files.list(dirPath)) {
            List<FileInfoResponse> entries = stream
                    .map(this::createFileInfo)
                    .collect(Collectors.toList());

            FileListResponse response = new FileListResponse();
            response.setPath(path);
            response.setEntries(entries);
            response.setTotalSize(entries.stream().mapToLong(FileInfoResponse::getSize).sum());
            response.setTotalFiles((int) entries.stream().filter(e -> !e.isDirectory()).count());
            response.setTotalDirectories((int) entries.stream().filter(FileInfoResponse::isDirectory).count());

            return response;
        } catch (IOException e) {
            log.error("Failed to list directory at {}: {}", dirPath, e, e);
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to list directory");
        }
    }

    public Resource loadAsResource(String path) {
        try {
            Path filePath = getAbsolutePath(path);
            validatePath(filePath);
            Path real;
            try {
                real = filePath.toRealPath(); // resolves symlinks
            } catch (IOException e) {
                log.error("Failed to read file {}: {}", path, e, e);
                throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to read file");
            }
            if (!real.startsWith(rootLocation)) {
                throw new AfsException(ErrorCode.VALIDATION_FAILED, "Path resolves outside of root directory");
            }
            Resource resource = new UrlResource(real.toUri());
            if (Files.isDirectory(real)) {
                throw new AfsException(ErrorCode.VALIDATION_FAILED, "Cannot download a directory");
            }
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new AfsException(ErrorCode.NOT_FOUND, "File not found");
            }
        } catch (MalformedURLException e) {
            log.error("Failed to read file {}: {}", path, e, e);
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to read file");
        }
    }

    public FileInfoResponse move(String sourcePath, String targetPath) {
        try {
            Path source = getAbsolutePath(sourcePath);
            Path target = getAbsolutePath(targetPath);
            validatePath(source);
            validatePath(target);

            if (Files.exists(target)) {
                throw new AfsException(ErrorCode.VALIDATION_FAILED, "Target already exists");
            }

            Files.move(source, target);
            return createFileInfo(target);
        } catch (IOException e) {
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to move: " + e.getMessage());
        }
    }

    public FileInfoResponse rename(String path, String newName) {
        try {
            Path source = getAbsolutePath(path);
            Path target = source.resolveSibling(newName);
            validatePath(source);
            validatePath(target);

            if (Files.exists(target)) {
                throw new AfsException(ErrorCode.VALIDATION_FAILED, "Target already exists");
            }

            Files.move(source, target);
            return createFileInfo(target);
        } catch (IOException e) {
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to rename: " + e.getMessage());
        }
    }

    public FileInfoResponse store(MultipartFile file, String path) {
        try {
            Path targetPath = getAbsolutePath(path);
            validatePath(targetPath);

            if (Files.exists(targetPath)) {
                throw new AfsException(ErrorCode.VALIDATION_FAILED, "File already exists");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return createFileInfo(targetPath);
        } catch (IOException e) {
            log.error("Failed to store file to {}: {}", path, e, e);
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to store file");
        }
    }

    private FileInfoResponse createFileInfo(Path path) {
        try {
            FileInfoResponse info = new FileInfoResponse();
            info.setName(path.getFileName().toString());
            info.setPath(rootLocation.relativize(path).toString());
            info.setDirectory(Files.isDirectory(path));

            if (!Files.isDirectory(path)) {
                info.setSize(Files.size(path));
                info.setMimeType(Files.probeContentType(path));
            }

            info.setCreatedAt(LocalDateTime.ofInstant(
                    ((FileTime) Files.getAttribute(path, "creationTime")).toInstant(),
                    ZoneId.systemDefault()));
            info.setModifiedAt(LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(path).toInstant(),
                    ZoneId.systemDefault()));

            return info;
        } catch (IOException e) {
            log.error("Failed to read file info {}: {}", path, e, e);
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to read file info");
        }
    }

    private Path getAbsolutePath(String userGivenPath) {
        if (userGivenPath.startsWith("/")) {
            return rootLocation.resolve(userGivenPath.substring(1)).toAbsolutePath();
        } else {
            return rootLocation.resolve(userGivenPath).toAbsolutePath();
        }
    }

    private void validatePath(Path path) {
        if (!path.normalize().startsWith(rootLocation)) {
            throw new AfsException(ErrorCode.VALIDATION_FAILED, "Path is outside of root directory");
        }
    }
}
