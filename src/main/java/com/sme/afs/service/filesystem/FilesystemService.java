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
     * <p>
     * Unreadable entries are filtered out and the returned list contains no nulls.
     *
     * @param path Path to the file or directory
     * @return Metadata for the file or directory
     * @throws IOException if metadata cannot be retrieved
     */
    FileMetadataResponse getMetadata(Path path) throws IOException;

    /**
     * Check if a path exists
     * @param path Path to check
     * Notes:
     * <ul>
     *   <li>Subject to TOCTOU: existence can change between check and use; callers must handle failures on use.</li>
     *   <li>Per {@link java.nio.file.Files#exists(Path, java.nio.file.LinkOption...)},
     *             I/O errors may cause this to return {@code false} indistinguishable from non-existence.</li>
     *   <li>Symlink handling is implementation-defined; implementations should document whether links are followed.</li>
     * </ul>
     * @return true if the path exists at check time; false otherwise
     */
    boolean exists(Path path);
}
