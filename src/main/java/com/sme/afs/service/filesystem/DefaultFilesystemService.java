package com.sme.afs.service.filesystem;

import com.sme.afs.dto.FileMetadataResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class DefaultFilesystemService implements FilesystemService {

    @Override
    public List<FileMetadataResponse> listDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("Provided path is not a directory: " + directory);
        }

        try (Stream<Path> pathStream = Files.list(directory)) {
            return pathStream
                .map(this::safeGetMetadata)
                .collect(Collectors.toList());
        }
    }

    @Override
    public FileMetadataResponse getMetadata(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        
        return FileMetadataResponse.builder()
            .path(path)
            .name(path.getFileName().toString())
            .isDirectory(attrs.isDirectory())
            .size(attrs.size())
            .createdTime(attrs.creationTime().toInstant())
            .lastModifiedTime(attrs.lastModifiedTime().toInstant())
            .isReadable(Files.isReadable(path))
            .isWritable(Files.isWritable(path))
            .isExecutable(Files.isExecutable(path))
            .build();
    }

    @Override
    public Path resolvePath(Path basePath, String pathToResolve) {
        Path resolvedPath = basePath.resolve(pathToResolve).normalize();
        return resolvedPath.startsWith(basePath) ? resolvedPath : basePath;
    }

    @Override
    public boolean exists(Path path) {
        return Files.exists(path);
    }

    // Helper method to safely get metadata, returning null for unreadable files
    private FileMetadataResponse safeGetMetadata(Path path) {
        try {
            return getMetadata(path);
        } catch (IOException e) {
            log.warn("Could not get metadata for path: {}", path, e);
            return null;
        }
    }
}
