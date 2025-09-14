package com.sme.afs.service;

import com.sme.afs.config.BlobUrlProperties;
import com.sme.afs.repository.BlobUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BlobUrlHealthService.
 */
@ExtendWith(MockitoExtension.class)
class BlobUrlHealthServiceTest {

    @Mock
    private BlobUrlRepository blobUrlRepository;

    @Mock
    private BlobUrlProperties blobUrlProperties;

    @Mock
    private FilesystemValidationService filesystemValidationService;

    @Mock
    private CleanupScheduler cleanupScheduler;

    @TempDir
    Path tempDir;

    private BlobUrlHealthService healthService;

    @BeforeEach
    void setUp() {
        healthService = new BlobUrlHealthService(
                blobUrlRepository,
                blobUrlProperties,
                filesystemValidationService,
                cleanupScheduler
        );
    }

    @Test
    void performHealthCheck_WhenAllSystemsHealthy_ShouldReturnHealthyStatus() {
        // Arrange
        setupHealthyMocks();

        // Act
        BlobUrlHealthService.HealthStatus status = healthService.performHealthCheck();

        // Assert
        assertThat(status.isOverallHealthy()).isTrue();
        assertThat(status.getCheckTimestamp()).isNotNull();
        assertThat(status.getDatabaseHealth().isHealthy()).isTrue();
        assertThat(status.getFilesystemHealth().isHealthy()).isTrue();
        assertThat(status.getCleanupHealth().isHealthy()).isTrue();
        assertThat(status.getOperationalMetrics()).isNotNull();
        assertThat(status.getWarningConditions()).isNotNull();
        assertThat(status.getError()).isNull();
    }

    @Test
    void performHealthCheck_WhenDatabaseUnhealthy_ShouldReturnUnhealthyStatus() {
        // Arrange
        when(blobUrlRepository.countActiveUrls(any(OffsetDateTime.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act
        BlobUrlHealthService.HealthStatus status = healthService.performHealthCheck();

        // Assert
        assertThat(status.isOverallHealthy()).isFalse();
        assertThat(status.getError()).isNotNull();
        assertThat(status.getError()).contains("Health check failed");
    }

    @Test
    void performHealthCheck_WhenFilesystemUnhealthy_ShouldReturnUnhealthyStatus() {
        // Arrange
        setupHealthyDatabaseMocks();
        when(filesystemValidationService.validateFilesystem())
                .thenReturn(FilesystemValidationService.ValidationResult.criticalFailure("Hard links not supported"));
        when(filesystemValidationService.getFilesystemInfo())
                .thenReturn(FilesystemValidationService.FilesystemInfo.builder()
                        .exists(true)
                        .usableSpace(1024L * 1024L * 1024L) // 1GB
                        .build());
        setupHealthyCleanupMocks();

        // Act
        BlobUrlHealthService.HealthStatus status = healthService.performHealthCheck();

        // Assert
        assertThat(status.isOverallHealthy()).isFalse();
        assertThat(status.getFilesystemHealth().isHealthy()).isFalse();
        assertThat(status.getFilesystemHealth().getMessage()).contains("Hard links not supported");
    }

    @Test
    void performHealthCheck_WhenCleanupUnhealthy_ShouldReturnUnhealthyStatus() {
        // Arrange
        setupHealthyDatabaseMocks();
        setupHealthyFilesystemMocks();
        when(cleanupScheduler.getCleanupStats())
                .thenReturn(CleanupScheduler.CleanupStats.builder()
                        .activeUrls(10L)
                        .expiredUrls(150L) // High number of expired URLs
                        .filesInTempDirectory(160L)
                        .cleanupEnabled(false) // Cleanup disabled
                        .cleanupInterval(Duration.ofMinutes(15))
                        .build());

        // Act
        BlobUrlHealthService.HealthStatus status = healthService.performHealthCheck();

        // Assert
        assertThat(status.isOverallHealthy()).isFalse();
        assertThat(status.getCleanupHealth().isHealthy()).isFalse();
        assertThat(status.getCleanupHealth().getMessage()).contains("Automatic cleanup is disabled");
        assertThat(status.getCleanupHealth().getMessage()).contains("High number of expired URLs");
    }

    @Test
    void performHealthCheck_WhenExceptionOccurs_ShouldReturnErrorStatus() {
        // Arrange
        when(blobUrlRepository.countActiveUrls(any(OffsetDateTime.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        BlobUrlHealthService.HealthStatus status = healthService.performHealthCheck();

        // Assert
        assertThat(status.isOverallHealthy()).isFalse();
        assertThat(status.getDatabaseHealth()).isNull();
    }

    @Test
    void performHealthCheck_WhenLowDiskSpace_ShouldReturnUnhealthyFilesystem() {
        // Arrange
        setupHealthyDatabaseMocks();
        when(filesystemValidationService.validateFilesystem())
                .thenReturn(FilesystemValidationService.ValidationResult.success("Validation passed"));
        when(filesystemValidationService.getFilesystemInfo())
                .thenReturn(FilesystemValidationService.FilesystemInfo.builder()
                        .exists(true)
                        .usableSpace(512L * 1024L * 1024L) // 512MB - less than 1GB threshold
                        .fileSystemType("ext4")
                        .build());
        setupHealthyCleanupMocks();

        // Act
        BlobUrlHealthService.HealthStatus status = healthService.performHealthCheck();

        // Assert
        assertThat(status.isOverallHealthy()).isFalse();
        assertThat(status.getFilesystemHealth().isHealthy()).isFalse();
        assertThat(status.getFilesystemHealth().getMessage()).contains("Low disk space");
    }

    @Test
    void performHealthCheck_ShouldDetectWarningConditions() {
        // Arrange
        setupHealthyFilesystemMocks();
        
        // Setup high utilization scenario
        when(blobUrlProperties.getMaxConcurrentUrls()).thenReturn(100L);
        when(blobUrlRepository.countActiveUrls(any(OffsetDateTime.class))).thenReturn(85L); // 85% utilization
        when(blobUrlRepository.findExpiredUrls(any(OffsetDateTime.class))).thenReturn(List.of()); // No expired URLs
        when(blobUrlProperties.getTempDirectory()).thenReturn(tempDir.toString());
        
        when(cleanupScheduler.getCleanupStats())
                .thenReturn(CleanupScheduler.CleanupStats.builder()
                        .activeUrls(85L)
                        .expiredUrls(0L)
                        .filesInTempDirectory(85L)
                        .cleanupEnabled(true)
                        .cleanupInterval(Duration.ofMinutes(15))
                        .build());

        // Act
        BlobUrlHealthService.HealthStatus status = healthService.performHealthCheck();

        // Assert
        assertThat(status.isOverallHealthy()).isTrue(); // Still healthy, just warnings
        assertThat(status.getWarningConditions().isHasWarnings()).isTrue();
        assertThat(status.getWarningConditions().getWarnings()).contains("High utilization");
    }

    @Test
    void performHealthCheck_ShouldDetectFileMismatchWarning() throws IOException {
        // Arrange
        setupHealthyFilesystemMocks();
        
        // Create more files in temp directory than active URLs
        Files.createFile(tempDir.resolve("file1"));
        Files.createFile(tempDir.resolve("file2"));
        Files.createFile(tempDir.resolve("file3"));
        Files.createFile(tempDir.resolve("file4"));
        Files.createFile(tempDir.resolve("file5"));
        Files.createFile(tempDir.resolve("file6"));
        Files.createFile(tempDir.resolve("file7")); // 7 files but only 2 active URLs
        
        when(blobUrlRepository.countActiveUrls(any(OffsetDateTime.class))).thenReturn(2L);
        when(blobUrlRepository.findExpiredUrls(any(OffsetDateTime.class))).thenReturn(List.of());
        when(blobUrlProperties.getTempDirectory()).thenReturn(tempDir.toString());
        when(blobUrlProperties.getMaxConcurrentUrls()).thenReturn(100L);
        
        when(cleanupScheduler.getCleanupStats())
                .thenReturn(CleanupScheduler.CleanupStats.builder()
                        .activeUrls(2L)
                        .expiredUrls(0L)
                        .filesInTempDirectory(7L)
                        .cleanupEnabled(true)
                        .cleanupInterval(Duration.ofMinutes(15))
                        .build());

        // Act
        BlobUrlHealthService.HealthStatus status = healthService.performHealthCheck();

        // Assert
        assertThat(status.isOverallHealthy()).isTrue(); // Still healthy, just warnings
        assertThat(status.getWarningConditions().isHasWarnings()).isTrue();
        assertThat(status.getWarningConditions().getWarnings()).contains("File count mismatch");
    }

    @Test
    void performHealthCheck_ShouldProvideOperationalMetrics() {
        // Arrange
        setupHealthyMocks();
        when(blobUrlProperties.getDefaultExpiration()).thenReturn(Duration.ofHours(2));
        when(blobUrlProperties.getCleanupInterval()).thenReturn(Duration.ofMinutes(30));

        // Act
        BlobUrlHealthService.HealthStatus status = healthService.performHealthCheck();

        // Assert
        BlobUrlHealthService.OperationalMetrics metrics = status.getOperationalMetrics();
        assertThat(metrics.getActiveUrls()).isEqualTo(10L);
        assertThat(metrics.getExpiredUrls()).isEqualTo(0L);
        assertThat(metrics.getMaxConcurrentUrls()).isEqualTo(1000L);
        assertThat(metrics.getUtilizationPercent()).isEqualTo(1.0); // 10/1000 * 100
        assertThat(metrics.getDefaultExpirationHours()).isEqualTo(2L);
        assertThat(metrics.getCleanupIntervalMinutes()).isEqualTo(30L);
    }

    private void setupHealthyMocks() {
        setupHealthyDatabaseMocks();
        setupHealthyFilesystemMocks();
        setupHealthyCleanupMocks();
    }

    private void setupHealthyDatabaseMocks() {
        when(blobUrlRepository.countActiveUrls(any(OffsetDateTime.class))).thenReturn(10L);
        when(blobUrlRepository.findExpiredUrls(any(OffsetDateTime.class))).thenReturn(List.of());
    }

    private void setupHealthyFilesystemMocks() {
        when(filesystemValidationService.validateFilesystem())
                .thenReturn(FilesystemValidationService.ValidationResult.success("All validations passed"));
        when(filesystemValidationService.getFilesystemInfo())
                .thenReturn(FilesystemValidationService.FilesystemInfo.builder()
                        .exists(true)
                        .usableSpace(10L * 1024L * 1024L * 1024L) // 10GB
                        .fileSystemType("ext4")
                        .build());
    }

    private void setupHealthyCleanupMocks() {
        when(cleanupScheduler.getCleanupStats())
                .thenReturn(CleanupScheduler.CleanupStats.builder()
                        .activeUrls(10L)
                        .expiredUrls(0L)
                        .filesInTempDirectory(10L)
                        .cleanupEnabled(true)
                        .cleanupInterval(Duration.ofMinutes(15))
                        .build());
        
        when(blobUrlProperties.getMaxConcurrentUrls()).thenReturn(1000L);
        when(blobUrlProperties.getTempDirectory()).thenReturn(tempDir.toString());
        when(blobUrlProperties.getDefaultExpiration()).thenReturn(Duration.ofHours(1));
        when(blobUrlProperties.getCleanupInterval()).thenReturn(Duration.ofMinutes(15));
    }
}