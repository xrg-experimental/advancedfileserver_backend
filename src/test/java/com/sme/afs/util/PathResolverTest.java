package com.sme.afs.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

class PathResolverTest {

    private PathResolver pathResolver;

    @BeforeEach
    void setUp() {
        pathResolver = new PathResolver();
    }

    @Test
    void testResolveLogicalPath_ValidPath() {
        // Given
        String validPath = "/user/documents/file.txt";

        // When
        Path resolved = pathResolver.resolveLogicalPath(validPath);

        // Then
        assertThat(resolved).isNotNull();
        assertThat(resolved.isAbsolute()).isTrue();
    }

    @Test
    void testResolveLogicalPath_InvalidPath() {
        // Given
        String invalidPath = "invalid:path";

        // When/Then
        assertThatThrownBy(() -> pathResolver.resolveLogicalPath(invalidPath))
            .isInstanceOf(InvalidPathException.class);
    }

    @Test
    void testConvertToLogicalPath() {
        // Given
        Path physicalPath = Path.of("/user/documents/file.txt");

        // When
        String logicalPath = pathResolver.convertToLogicalPath(physicalPath);

        // Then
        assertThat(logicalPath).isEqualTo("/user/documents/file.txt");
    }

    @Test
    void testIsAbsolutePath() {
        // Given
        String absolutePath = "/absolute/path";
        String relativePath = "relative/path";

        // When/Then
        assertThat(pathResolver.isAbsolutePath(absolutePath)).isTrue();
        assertThat(pathResolver.isAbsolutePath(relativePath)).isFalse();
    }

    @Test
    void testResolveRelativePath() {
        // Given
        Path basePath = Path.of("/base/directory");
        String relativePath = "subdirectory/file.txt";

        // When
        Path resolvedPath = pathResolver.resolveRelativePath(basePath, relativePath);
        
        // Then
        assertThat(pathResolver.convertToLogicalPath(resolvedPath))
            .contains("/base/directory/subdirectory/file.txt");
    }

    @Test
    void testResolveLogicalPath_WindowsStyle() {
        // Given
        String windowsPath = "C:\\Users\\Documents\\file.txt";

        // When
        Path resolved = pathResolver.resolveLogicalPath(windowsPath);

        // Then
        assertThat(resolved).isNotNull();
        assertThat(resolved.isAbsolute()).isTrue();
        assertThat(resolved.toString()).contains("Users", "Documents", "file.txt");
    }

    @Test
    void testResolveLogicalPath_UnixStyle() {
        // Given
        String unixPath = "/home/user/documents/file.txt";

        // When
        Path resolved = pathResolver.resolveLogicalPath(unixPath);

        // Then
        assertThat(resolved).isNotNull();
        assertThat(resolved.isAbsolute()).isTrue();
        assertThat(resolved.toString()).contains("home", "user", "documents", "file.txt");
    }

    @Test
    void testResolveLogicalPath_MixedSeparators() {
        // Given
        String mixedPath = "C:/Users\\Documents/file.txt";

        // When
        Path resolved = pathResolver.resolveLogicalPath(mixedPath);

        // Then
        assertThat(resolved).isNotNull();
        assertThat(resolved.isAbsolute()).isTrue();
        assertThat(resolved.toString()).contains("Users", "Documents", "file.txt");
    }
}
