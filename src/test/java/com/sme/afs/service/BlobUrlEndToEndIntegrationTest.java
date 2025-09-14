package com.sme.afs.service;

import com.sme.afs.config.BlobUrlProperties;
import com.sme.afs.dto.BlobUrlResponse;
import com.sme.afs.model.BlobUrl;
import com.sme.afs.repository.BlobUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-end integration tests for the complete blob URL workflow including cleanup operations.
 * Tests the entire system from creation to cleanup with real filesystem operations.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlobUrlEndToEndIntegrationTest {

    @Autowired
    private BlobUrlService blobUrlService;

    @Autowired
    private CleanupScheduler cleanupScheduler;

    @Autowired
    private BlobUrlHealthService healthService;

    @Autowired
    private FilesystemValidationService validationService;

    @Autowired
    private BlobUrlRepository blobUrlRepository;

    @Autowired
    private BlobUrlProperties blobUrlProperties;

    @TempDir
    Path testTempDir;

    private Path originalTempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Store original temp directory and set test temp directory
        originalTempDir = Path.of(blobUrlProperties.getTempDirectory());
        blobUrlProperties.setTempDirectory(testTempDir.toString());
        
        // Ensure temp directory exists
        Files.createDirectories(testTempDir);
        
        // Clean up any existing data
        blobUrlRepository.deleteAll();
    }

    @Test
    void completeWorkflow_CreateDownloadAndCleanup_ShouldWorkEndToEnd() throws IOException {
        // Arrange - Create a test file
        Path testFile = testTempDir.resolve("test-source.txt");
        String testContent = "This is a test file for blob URL functionality";
        Files.write(testFile, testContent.getBytes());

        // Act 1: Create blob URL
        BlobUrl blobUrl = blobUrlService.createBlobUrl(testFile.toString(), "testuser");
        
        // Assert 1: Blob URL created successfully
        assertThat(blobUrl).isNotNull();
        assertThat(blobUrl.getToken()).isNotNull();
        assertThat(blobUrl.getOriginalPath()).isEqualTo(testFile.toString());
        assertThat(blobUrl.getHardLinkPath()).isNotNull();
        assertThat(Files.exists(Path.of(blobUrl.getHardLinkPath()))).isTrue();

        // Act 2: Get blob URL status
        BlobUrlResponse statusResponse = blobUrlService.getBlobUrlStatus(blobUrl.getToken());
        
        // Assert 2: Status is correct
        assertThat(statusResponse.getStatus()).isEqualTo("active");
        assertThat(statusResponse.getFileSize()).isEqualTo(testContent.length());
        assertThat(statusResponse.getFilename()).isEqualTo("test-source.txt");

        // Act 3: Download file
        Resource downloadResource = blobUrlService.validateAndGetFile(blobUrl.getToken());
        
        // Assert 3: Download works correctly
        assertThat(downloadResource.exists()).isTrue();
        assertThat(downloadResource.isReadable()).isTrue();
        byte[] downloadedContent = downloadResource.getInputStream().readAllBytes();
        assertThat(new String(downloadedContent)).isEqualTo(testContent);

        // Act 4: Verify hard link integrity
        byte[] hardLinkContent = Files.readAllBytes(Path.of(blobUrl.getHardLinkPath()));
        assertThat(new String(hardLinkContent)).isEqualTo(testContent);

        // Act 5: Force cleanup
        boolean cleanupSuccess = cleanupScheduler.forceCleanupByToken(blobUrl.getToken());
        
        // Assert 5: Cleanup successful
        assertThat(cleanupSuccess).isTrue();
        assertThat(Files.exists(Path.of(blobUrl.getHardLinkPath()))).isFalse();
        assertThat(blobUrlRepository.findById(blobUrl.getToken())).isEmpty();
    }

    @Test
    void expiredUrlWorkflow_ShouldBeCleanedUpAutomatically() throws IOException, InterruptedException {
        // Arrange - Create a test file and set short expiration
        Path testFile = testTempDir.resolve("expired-test.txt");
        Files.write(testFile, "Expired content".getBytes());
        
        // Temporarily set very short expiration
        Duration originalExpiration = blobUrlProperties.getDefaultExpiration();
        blobUrlProperties.setDefaultExpiration(Duration.ofMillis(100));

        try {
            // Act 1: Create blob URL with short expiration
            BlobUrl blobUrl = blobUrlService.createBlobUrl(testFile.toString(), "testuser");
            Path hardLinkPath = Path.of(blobUrl.getHardLinkPath());
            
            // Assert 1: Initially active
            assertThat(Files.exists(hardLinkPath)).isTrue();
            assertThat(blobUrlRepository.findById(blobUrl.getToken())).isPresent();

            // Act 2: Wait for expiration
            Thread.sleep(200);

            // Act 3: Try to access expired URL
            assertThatThrownBy(() -> blobUrlService.getBlobUrlStatus(blobUrl.getToken()))
                    .hasMessageContaining("invalid or expired");

            // Act 4: Run cleanup
            int cleanedCount = blobUrlService.cleanupExpiredUrls();
            
            // Assert 4: Expired URL cleaned up
            assertThat(cleanedCount).isEqualTo(1);
            assertThat(Files.exists(hardLinkPath)).isFalse();
            assertThat(blobUrlRepository.findById(blobUrl.getToken())).isEmpty();
            
        } finally {
            // Restore original expiration
            blobUrlProperties.setDefaultExpiration(originalExpiration);
        }
    }

    @Test
    void orphanedFileCleanup_ShouldRemoveFilesWithoutDatabaseEntries() throws IOException {
        // Arrange - Create orphaned files in temp directory
        Path orphanedFile1 = testTempDir.resolve("orphaned-token-1");
        Path orphanedFile2 = testTempDir.resolve("orphaned-token-2");
        Path validFile = testTempDir.resolve("valid-token");
        
        Files.write(orphanedFile1, "orphaned content 1".getBytes());
        Files.write(orphanedFile2, "orphaned content 2".getBytes());
        Files.write(validFile, "valid content".getBytes());

        // Create a valid blob URL entry for one file
        BlobUrl validBlobUrl = BlobUrl.builder()
                .token("valid-token")
                .originalPath("/original/path")
                .hardLinkPath(validFile.toString())
                .filename("valid.txt")
                .contentType("text/plain")
                .fileSize(13L)
                .createdAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .createdBy("testuser")
                .build();
        blobUrlRepository.save(validBlobUrl);

        // Act - Run startup cleanup (which removes orphaned files)
        cleanupScheduler.cleanupOnStartup();

        // Assert - Orphaned files removed, valid file remains
        assertThat(Files.exists(orphanedFile1)).isFalse();
        assertThat(Files.exists(orphanedFile2)).isFalse();
        assertThat(Files.exists(validFile)).isTrue();
        assertThat(blobUrlRepository.findById("valid-token")).isPresent();
    }

    @Test
    void healthCheckWorkflow_ShouldDetectSystemState() throws IOException {
        // Arrange - Create some test data
        Path testFile = testTempDir.resolve("health-test.txt");
        Files.write(testFile, "Health check test".getBytes());
        
        BlobUrl activeBlobUrl = blobUrlService.createBlobUrl(testFile.toString(), "testuser");
        
        // Create an expired blob URL manually
        BlobUrl expiredBlobUrl = BlobUrl.builder()
                .token("expired-token")
                .originalPath(testFile.toString())
                .hardLinkPath(testTempDir.resolve("expired-token").toString())
                .filename("expired.txt")
                .contentType("text/plain")
                .fileSize(100L)
                .createdAt(OffsetDateTime.now().minusHours(2))
                .expiresAt(OffsetDateTime.now().minusHours(1)) // Expired 1 hour ago
                .createdBy("testuser")
                .build();
        blobUrlRepository.save(expiredBlobUrl);

        // Act - Perform health check
        BlobUrlHealthService.HealthStatus health = healthService.performHealthCheck();

        // Assert - Health check provides accurate information
        assertThat(health.isOverallHealthy()).isTrue(); // Should be healthy despite expired URLs
        
        // Database health
        assertThat(health.getDatabaseHealth().isHealthy()).isTrue();
        assertThat(health.getDatabaseHealth().getActiveUrlCount()).isEqualTo(1L); // Only active URL
        
        // Filesystem health
        assertThat(health.getFilesystemHealth().isHealthy()).isTrue();
        assertThat(health.getFilesystemHealth().getTempDirectoryExists()).isTrue();
        assertThat(health.getFilesystemHealth().getHardLinkSupported()).isTrue();
        
        // Cleanup health
        assertThat(health.getCleanupHealth().isHealthy()).isTrue();
        assertThat(health.getCleanupHealth().getExpiredUrlCount()).isEqualTo(1L);
        
        // Operational metrics
        BlobUrlHealthService.OperationalMetrics metrics = health.getOperationalMetrics();
        assertThat(metrics.getActiveUrls()).isEqualTo(1L);
        assertThat(metrics.getExpiredUrls()).isEqualTo(1L);
        assertThat(metrics.getUtilizationPercent()).isLessThan(1.0); // Very low utilization
    }

    @Test
    void filesystemValidationWorkflow_ShouldValidateCapabilities() {
        // Act - Validate filesystem
        FilesystemValidationService.ValidationResult result = validationService.validateFilesystem();

        // Assert - Validation should pass for test environment
        assertThat(result.isValid()).isTrue();
        assertThat(result.getMessage()).contains("successfully");

        // Act - Get filesystem info
        FilesystemValidationService.FilesystemInfo info = validationService.getFilesystemInfo();

        // Assert - Filesystem info should be populated
        assertThat(info.isExists()).isTrue();
        assertThat(info.getTempDirectoryPath()).isEqualTo(testTempDir.toString());
        assertThat(info.getFileSystemType()).isNotNull();
        assertThat(info.getTotalSpace()).isNotNull();
        assertThat(info.getUsableSpace()).isNotNull();
    }

    @Test
    void concurrentOperationsWorkflow_ShouldHandleMultipleUrls() throws IOException {
        // Arrange - Create multiple test files
        Path testFile1 = testTempDir.resolve("concurrent-test-1.txt");
        Path testFile2 = testTempDir.resolve("concurrent-test-2.txt");
        Path testFile3 = testTempDir.resolve("concurrent-test-3.txt");
        
        Files.write(testFile1, "Content 1".getBytes());
        Files.write(testFile2, "Content 2".getBytes());
        Files.write(testFile3, "Content 3".getBytes());

        // Act - Create multiple blob URLs
        BlobUrl blobUrl1 = blobUrlService.createBlobUrl(testFile1.toString(), "user1");
        BlobUrl blobUrl2 = blobUrlService.createBlobUrl(testFile2.toString(), "user2");
        BlobUrl blobUrl3 = blobUrlService.createBlobUrl(testFile3.toString(), "user3");

        // Assert - All URLs created successfully
        assertThat(blobUrl1.getToken()).isNotEqualTo(blobUrl2.getToken());
        assertThat(blobUrl2.getToken()).isNotEqualTo(blobUrl3.getToken());
        assertThat(blobUrl1.getToken()).isNotEqualTo(blobUrl3.getToken());

        // Act - Verify all hard links exist
        assertThat(Files.exists(Path.of(blobUrl1.getHardLinkPath()))).isTrue();
        assertThat(Files.exists(Path.of(blobUrl2.getHardLinkPath()))).isTrue();
        assertThat(Files.exists(Path.of(blobUrl3.getHardLinkPath()))).isTrue();

        // Act - Download from all URLs
        Resource resource1 = blobUrlService.validateAndGetFile(blobUrl1.getToken());
        Resource resource2 = blobUrlService.validateAndGetFile(blobUrl2.getToken());
        Resource resource3 = blobUrlService.validateAndGetFile(blobUrl3.getToken());

        // Assert - All downloads work correctly
        assertThat(resource1.getInputStream().readAllBytes()).isEqualTo("Content 1".getBytes());
        assertThat(resource2.getInputStream().readAllBytes()).isEqualTo("Content 2".getBytes());
        assertThat(resource3.getInputStream().readAllBytes()).isEqualTo("Content 3".getBytes());

        // Act - Get statistics
        CleanupScheduler.CleanupStats stats = cleanupScheduler.getCleanupStats();

        // Assert - Statistics reflect multiple active URLs
        assertThat(stats.getActiveUrls()).isEqualTo(3L);
        assertThat(stats.getFilesInTempDirectory()).isEqualTo(3L);

        // Act - Force cleanup all
        int cleanedCount = cleanupScheduler.forceCleanup();

        // Assert - All URLs cleaned up (none were expired, so only orphaned files if any)
        assertThat(cleanedCount).isGreaterThanOrEqualTo(0);
        
        // Verify URLs still exist (they weren't expired)
        assertThat(blobUrlRepository.findById(blobUrl1.getToken())).isPresent();
        assertThat(blobUrlRepository.findById(blobUrl2.getToken())).isPresent();
        assertThat(blobUrlRepository.findById(blobUrl3.getToken())).isPresent();
    }

    @Test
    void errorRecoveryWorkflow_ShouldHandleFailuresGracefully() throws IOException {
        // Arrange - Create a test file
        Path testFile = testTempDir.resolve("error-test.txt");
        Files.write(testFile, "Error recovery test".getBytes());

        // Act 1: Create blob URL
        BlobUrl blobUrl = blobUrlService.createBlobUrl(testFile.toString(), "testuser");
        Path hardLinkPath = Path.of(blobUrl.getHardLinkPath());

        // Act 2: Manually delete the hard link file (simulate filesystem error)
        Files.delete(hardLinkPath);

        // Act 3: Try to download (should fail gracefully)
        assertThatThrownBy(() -> blobUrlService.validateAndGetFile(blobUrl.getToken()))
                .hasMessageContaining("no longer available");

        // Act 4: Cleanup should still work
        boolean cleanupSuccess = cleanupScheduler.forceCleanupByToken(blobUrl.getToken());

        // Assert - Cleanup handles missing file gracefully
        assertThat(cleanupSuccess).isTrue();
        assertThat(blobUrlRepository.findById(blobUrl.getToken())).isEmpty();
    }
}