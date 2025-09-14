package com.sme.afs.service;

import com.sme.afs.config.BlobUrlProperties;
import com.sme.afs.model.BlobUrl;
import com.sme.afs.repository.BlobUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CleanupScheduler.
 */
@ExtendWith(MockitoExtension.class)
class CleanupSchedulerTest {

    @Mock
    private BlobUrlService blobUrlService;

    @Mock
    private BlobUrlRepository blobUrlRepository;

    @Mock
    private HardLinkManager hardLinkManager;

    @Mock
    private BlobUrlProperties blobUrlProperties;

    @Mock
    private ApplicationReadyEvent applicationReadyEvent;

    @TempDir
    Path tempDir;

    private CleanupScheduler cleanupScheduler;

    @BeforeEach
    void setUp() {
        cleanupScheduler = new CleanupScheduler(
                blobUrlService,
                blobUrlRepository,
                hardLinkManager,
                blobUrlProperties
        );
    }

    @Test
    void scheduledCleanup_WhenCleanupEnabled_ShouldCallBlobUrlService() {
        // Arrange
        when(blobUrlProperties.isEnableAutomaticCleanup()).thenReturn(true);
        when(blobUrlService.cleanupExpiredUrls()).thenReturn(5);

        // Act
        cleanupScheduler.scheduledCleanup();

        // Assert
        verify(blobUrlService).cleanupExpiredUrls();
    }

    @Test
    void scheduledCleanup_WhenCleanupDisabled_ShouldSkipCleanup() {
        // Arrange
        when(blobUrlProperties.isEnableAutomaticCleanup()).thenReturn(false);

        // Act
        cleanupScheduler.scheduledCleanup();

        // Assert
        verify(blobUrlService, never()).cleanupExpiredUrls();
    }

    @Test
    void scheduledCleanup_WhenExceptionOccurs_ShouldHandleGracefully() {
        // Arrange
        when(blobUrlProperties.isEnableAutomaticCleanup()).thenReturn(true);
        when(blobUrlService.cleanupExpiredUrls()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThatCode(() -> cleanupScheduler.scheduledCleanup())
                .doesNotThrowAnyException();
    }

    @Test
    void cleanupOnStartup_WhenStartupCleanupEnabled_ShouldRemoveOrphanedFiles() throws IOException {
        // Arrange
        when(blobUrlProperties.isCleanupOnStartup()).thenReturn(true);
        when(blobUrlProperties.getTempDirectory()).thenReturn(tempDir.toString());
        when(blobUrlRepository.findAll()).thenReturn(Arrays.asList(
                createBlobUrl("valid-token-1", tempDir.resolve("valid-token-1").toString()),
                createBlobUrl("valid-token-2", tempDir.resolve("valid-token-2").toString())
        ));

        // Create some files in temp directory
        Files.createFile(tempDir.resolve("valid-token-1"));
        Files.createFile(tempDir.resolve("valid-token-2"));
        Files.createFile(tempDir.resolve("orphaned-token-1"));
        Files.createFile(tempDir.resolve("orphaned-token-2"));

        // Act
        cleanupScheduler.cleanupOnStartup();

        // Assert
        try {
            verify(hardLinkManager, times(2)).deleteHardLink(any(Path.class));
            verify(hardLinkManager).deleteHardLink(tempDir.resolve("orphaned-token-1"));
            verify(hardLinkManager).deleteHardLink(tempDir.resolve("orphaned-token-2"));
            verify(hardLinkManager, never()).deleteHardLink(tempDir.resolve("valid-token-1"));
            verify(hardLinkManager, never()).deleteHardLink(tempDir.resolve("valid-token-2"));
        } catch (IOException e) {
            fail("IOException should not occur in test verification");
        }
    }

    @Test
    void cleanupOnStartup_WhenStartupCleanupDisabled_ShouldSkipCleanup() {
        // Arrange
        when(blobUrlProperties.isCleanupOnStartup()).thenReturn(false);

        // Act
        cleanupScheduler.cleanupOnStartup();

        // Assert
        verify(blobUrlRepository, never()).findAll();
        try {
            verify(hardLinkManager, never()).deleteHardLink(any(Path.class));
        } catch (IOException e) {
            fail("IOException should not occur in test verification");
        }
    }

    @Test
    void cleanupOnStartup_WhenTempDirectoryDoesNotExist_ShouldHandleGracefully() {
        // Arrange
        when(blobUrlProperties.isCleanupOnStartup()).thenReturn(true);
        when(blobUrlProperties.getTempDirectory()).thenReturn("/non/existent/directory");

        // Act & Assert
        assertThatCode(() -> cleanupScheduler.cleanupOnStartup())
                .doesNotThrowAnyException();
    }

    @Test
    void forceCleanup_ShouldCallBothCleanupMethods() {
        // Arrange
        when(blobUrlService.cleanupExpiredUrls()).thenReturn(3);
        when(blobUrlProperties.getTempDirectory()).thenReturn(tempDir.toString());
        when(blobUrlRepository.findAll()).thenReturn(List.of());

        // Act
        int result = cleanupScheduler.forceCleanup();

        // Assert
        assertThat(result).isEqualTo(3);
        verify(blobUrlService).cleanupExpiredUrls();
        verify(blobUrlRepository).findAll();
    }

    @Test
    void forceCleanupByToken_WhenTokenExists_ShouldCleanupSuccessfully() throws IOException {
        // Arrange
        String token = "test-token";
        Path hardLinkPath = tempDir.resolve("test-file");
        Files.createFile(hardLinkPath);
        
        BlobUrl blobUrl = createBlobUrl(token, hardLinkPath.toString());
        when(blobUrlRepository.findById(token)).thenReturn(Optional.of(blobUrl));

        // Act
        boolean result = cleanupScheduler.forceCleanupByToken(token);

        // Assert
        assertThat(result).isTrue();
        try {
            verify(hardLinkManager).deleteHardLink(hardLinkPath);
        } catch (IOException e) {
            fail("IOException should not occur in test verification");
        }
        verify(blobUrlRepository).delete(blobUrl);
    }

    @Test
    void forceCleanupByToken_WhenTokenDoesNotExist_ShouldReturnFalse() {
        // Arrange
        String token = "non-existent-token";
        when(blobUrlRepository.findById(token)).thenReturn(Optional.empty());

        // Act
        boolean result = cleanupScheduler.forceCleanupByToken(token);

        // Assert
        assertThat(result).isFalse();
        try {
            verify(hardLinkManager, never()).deleteHardLink(any(Path.class));
        } catch (IOException e) {
            fail("IOException should not occur in test verification");
        }
        verify(blobUrlRepository, never()).delete(any(BlobUrl.class));
    }

    @Test
    void forceCleanupByToken_WhenHardLinkDeletionFails_ShouldReturnFalse() throws IOException {
        // Arrange
        String token = "test-token";
        Path hardLinkPath = tempDir.resolve("test-file");
        Files.createFile(hardLinkPath);
        
        BlobUrl blobUrl = createBlobUrl(token, hardLinkPath.toString());
        when(blobUrlRepository.findById(token)).thenReturn(Optional.of(blobUrl));
        doThrow(new IOException("Deletion failed")).when(hardLinkManager).deleteHardLink(hardLinkPath);

        // Act
        boolean result = cleanupScheduler.forceCleanupByToken(token);

        // Assert
        assertThat(result).isFalse();
        try {
            verify(hardLinkManager).deleteHardLink(hardLinkPath);
        } catch (IOException e) {
            fail("IOException should not occur in test verification");
        }
        verify(blobUrlRepository, never()).delete(any(BlobUrl.class));
    }

    @Test
    void getCleanupStats_ShouldReturnCorrectStatistics() throws IOException {
        // Arrange
        when(blobUrlRepository.countActiveUrls(any(OffsetDateTime.class))).thenReturn(10L);
        when(blobUrlRepository.findExpiredUrls(any(OffsetDateTime.class))).thenReturn(
                Arrays.asList(
                        createBlobUrl("expired-1", "path1"),
                        createBlobUrl("expired-2", "path2")
                )
        );
        when(blobUrlProperties.getTempDirectory()).thenReturn(tempDir.toString());
        when(blobUrlProperties.isEnableAutomaticCleanup()).thenReturn(true);
        when(blobUrlProperties.getCleanupInterval()).thenReturn(Duration.ofMinutes(15));

        // Create some files in temp directory
        Files.createFile(tempDir.resolve("file1"));
        Files.createFile(tempDir.resolve("file2"));
        Files.createFile(tempDir.resolve("file3"));

        // Act
        CleanupScheduler.CleanupStats stats = cleanupScheduler.getCleanupStats();

        // Assert
        assertThat(stats.getActiveUrls()).isEqualTo(10L);
        assertThat(stats.getExpiredUrls()).isEqualTo(2L);
        assertThat(stats.getFilesInTempDirectory()).isEqualTo(3L);
        assertThat(stats.getTempDirectoryPath()).isEqualTo(tempDir.toString());
        assertThat(stats.isCleanupEnabled()).isTrue();
        assertThat(stats.getCleanupInterval()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void getCleanupStats_WhenTempDirectoryDoesNotExist_ShouldHandleGracefully() {
        // Arrange
        when(blobUrlRepository.countActiveUrls(any(OffsetDateTime.class))).thenReturn(5L);
        when(blobUrlRepository.findExpiredUrls(any(OffsetDateTime.class))).thenReturn(List.of());
        when(blobUrlProperties.getTempDirectory()).thenReturn("/non/existent/directory");
        when(blobUrlProperties.isEnableAutomaticCleanup()).thenReturn(false);
        when(blobUrlProperties.getCleanupInterval()).thenReturn(Duration.ofMinutes(30));

        // Act
        CleanupScheduler.CleanupStats stats = cleanupScheduler.getCleanupStats();

        // Assert
        assertThat(stats.getActiveUrls()).isEqualTo(5L);
        assertThat(stats.getExpiredUrls()).isEqualTo(0L);
        assertThat(stats.getFilesInTempDirectory()).isEqualTo(0L);
        assertThat(stats.isCleanupEnabled()).isFalse();
    }

    @Test
    void getCleanupStats_WhenExceptionOccurs_ShouldReturnErrorStats() {
        // Arrange
        when(blobUrlRepository.countActiveUrls(any(OffsetDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));
        when(blobUrlProperties.getTempDirectory()).thenReturn(tempDir.toString());
        when(blobUrlProperties.isEnableAutomaticCleanup()).thenReturn(true);
        when(blobUrlProperties.getCleanupInterval()).thenReturn(Duration.ofMinutes(15));

        // Act
        CleanupScheduler.CleanupStats stats = cleanupScheduler.getCleanupStats();

        // Assert
        assertThat(stats.getActiveUrls()).isEqualTo(-1L);
        assertThat(stats.getExpiredUrls()).isEqualTo(-1L);
        assertThat(stats.getFilesInTempDirectory()).isEqualTo(-1L);
        assertThat(stats.isCleanupEnabled()).isTrue();
    }

    private BlobUrl createBlobUrl(String token, String hardLinkPath) {
        return BlobUrl.builder()
                .token(token)
                .originalPath("/original/path")
                .hardLinkPath(hardLinkPath)
                .filename("test.txt")
                .contentType("text/plain")
                .fileSize(1024L)
                .createdAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .createdBy("testuser")
                .build();
    }
}