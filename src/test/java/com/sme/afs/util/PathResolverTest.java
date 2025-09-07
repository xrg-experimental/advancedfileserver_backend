package com.sme.afs.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.*;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

class PathResolverTest {

    private PathResolver pathResolver;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger pathResolverLogger;

    @BeforeEach
    void setUp() {
        pathResolver = new PathResolver();
        
        // Set up log capturing
        pathResolverLogger = (Logger) LoggerFactory.getLogger(PathResolver.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        pathResolverLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        // Clean up log capturing
        pathResolverLogger.detachAppender(logAppender);
        logAppender.stop();
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
        
        // Verify no error logs were generated for a valid path
        assertThat(logAppender.list).isEmpty();
    }

    @Test
    void testResolveLogicalPath_InvalidPath() {
        // Given
        String invalidPath = "invalid:path";

        // When/Then - Test that exception is thrown
        assertThatThrownBy(() -> pathResolver.resolveLogicalPath(invalidPath))
            .isInstanceOf(InvalidPathException.class);
            
        // Assert that the error was logged appropriately (captured by our test appender)
        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent logEvent = logAppender.list.get(0);
        assertThat(logEvent.getLevel().toString()).isEqualTo("ERROR");
        assertThat(logEvent.getMessage()).isEqualTo("Invalid path resolution: {}");
        assertThat(logEvent.getArgumentArray()).containsExactly(invalidPath);
        
        // Verify that the exception was logged
        assertThat(logEvent.getThrowableProxy()).isNotNull();
        assertThat(logEvent.getThrowableProxy().getClassName()).isEqualTo(InvalidPathException.class.getName());
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
