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
        this.rootLocation = Path.of(sharedFolderConfig.getBasePath());
    }

    public FileListResponse listDirectory(String path) {
        Path dirPath = rootLocation.resolve(path).normalize();
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
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to list directory: " + e.getMessage());
        }
    }

    public FileInfoResponse getFileInfo(String path) {
        Path filePath = rootLocation.resolve(path).normalize();
        validatePath(filePath);
        return createFileInfo(filePath);
    }

    public FileInfoResponse createDirectory(String path) {
        try {
            Path dirPath = rootLocation.resolve(path).normalize();
            validatePath(dirPath);

            if (Files.exists(dirPath)) {
                throw new AfsException(ErrorCode.VALIDATION_FAILED, "Directory already exists");
            }

            Files.createDirectories(dirPath);
            return createFileInfo(dirPath);
        } catch (IOException e) {
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to create directory: " + e.getMessage());
        }
    }

    public void delete(String path) {
        try {
            Path filePath = rootLocation.resolve(path).normalize();
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

    public FileInfoResponse rename(String path, String newName) {
        try {
            Path source = rootLocation.resolve(path).normalize();
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

    public FileInfoResponse move(String sourcePath, String targetPath) {
        try {
            Path source = rootLocation.resolve(sourcePath).normalize();
            Path target = rootLocation.resolve(targetPath).normalize();
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

    public Resource loadAsResource(String path) {
        try {
            Path filePath = rootLocation.resolve(path).normalize();
            validatePath(filePath);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new AfsException(ErrorCode.NOT_FOUND, "File not found: " + path);
            }
        } catch (MalformedURLException e) {
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to read file: " + e.getMessage());
        }
    }

    public FileInfoResponse store(MultipartFile file, String path) {
        try {
            Path targetPath = rootLocation.resolve(path).normalize();
            validatePath(targetPath);

            if (Files.exists(targetPath)) {
                throw new AfsException(ErrorCode.VALIDATION_FAILED, "File already exists");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return createFileInfo(targetPath);
        } catch (IOException e) {
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to store file: " + e.getMessage());
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
            throw new AfsException(ErrorCode.INTERNAL_ERROR, "Failed to read file info: " + e.getMessage());
        }
    }

    private void validatePath(Path path) {
        if (!path.normalize().startsWith(rootLocation)) {
            throw new AfsException(ErrorCode.VALIDATION_FAILED, "Path is outside of root directory");
        }
    }
}
