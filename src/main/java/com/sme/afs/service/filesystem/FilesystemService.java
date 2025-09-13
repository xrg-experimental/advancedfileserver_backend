package com.sme.afs.service.filesystem;

import com.sme.afs.dto.FileMetadataResponse;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FilesystemService {
    /**
     * List contents of a directory
     * @param directory Path to the directory
     * @return List of file and directory metadata
     * @throws IOException if directory cannot be read
     */
    List<FileMetadataResponse> listDirectory(Path directory) throws IOException;

    /**
     * Get metadata for a specific file or directory
     * @param path Path to the file or directory
     * @return Metadata for the file or directory
     * @throws IOException if metadata cannot be retrieved
     */
    FileMetadataResponse getMetadata(Path path) throws IOException;

    /**
     * Resolve a path, handling relative and absolute paths
     * @param basePath Base path for resolution
     * @param pathToResolve Path to resolve
     * @return Resolved absolute path
     */
    Path resolvePath(Path basePath, String pathToResolve);

    /**
     * Check if a path exists
     * @param path Path to check
     * @return true if the path exists, false otherwise
     */
    boolean exists(Path path);
}
