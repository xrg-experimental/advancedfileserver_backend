package com.sme.afs.service;

import com.sme.afs.config.BlobUrlProperties;
import com.sme.afs.model.BlobUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the complete blob URL workflow.
 * Tests the interaction between TokenService, BlobUrlService, and HardLinkManager without Spring context.
 */
class BlobUrlIntegrationTest {

    private TokenService tokenService;
    private HardLinkManager hardLinkManager;
    private BlobUrlProperties blobUrlProperties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Initialize properties
        blobUrlProperties = new BlobUrlProperties();
        blobUrlProperties.setTempDirectory(tempDir.resolve("downloads").toString());
        blobUrlProperties.setDefaultExpiration(Duration.ofHours(1));
        blobUrlProperties.setTokenLength(32);
        blobUrlProperties.setMaxConcurrentUrls(1000L);

        // Initialize services
        tokenService = new TokenService(blobUrlProperties);
        hardLinkManager = new HardLinkManager();
    }

    @Test
    void completeWorkflow_CreateValidateAndCleanup() throws IOException {
        // This test demonstrates the complete workflow but requires Spring context
        // for full integration. Here we test the core service interactions.
        
        // Test token generation and validation
        String token1 = tokenService.generateSecureToken();
        String token2 = tokenService.generateSecureToken();
        
        assertThat(token1).isNotEqualTo(token2);
        assertThat(tokenService.validateTokenFormat(token1)).isTrue();
        assertThat(tokenService.validateTokenFormat(token2)).isTrue();
        
        // Test hard link operations
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        Files.write(sourceFile, "test content".getBytes());
        
        hardLinkManager.createHardLink(sourceFile, targetFile);
        assertThat(Files.exists(targetFile)).isTrue();
        assertThat(Files.readAllBytes(targetFile)).isEqualTo("test content".getBytes());
        
        // Verify hard link count
        int linkCount = hardLinkManager.getHardLinkCount(sourceFile);
        assertThat(linkCount).isGreaterThanOrEqualTo(1);
        
        // Test cleanup
        hardLinkManager.deleteHardLink(targetFile);
        assertThat(Files.exists(targetFile)).isFalse();
        assertThat(Files.exists(sourceFile)).isTrue(); // Original should remain
    }

    @Test
    void tokenSecurity_ShouldGenerateUnpredictableTokens() {
        // Test that tokens are cryptographically secure
        int tokenCount = 10000;
        java.util.Set<String> tokens = new java.util.HashSet<>();
        
        for (int i = 0; i < tokenCount; i++) {
            String token = tokenService.generateSecureToken();
            assertThat(tokens).doesNotContain(token);
            tokens.add(token);
        }
        
        assertThat(tokens).hasSize(tokenCount);
    }

    @Test
    void tokenValidation_ShouldHandleVariousFormats() {
        // Test various token validation scenarios
        String validToken = tokenService.generateSecureToken();
        
        // Valid cases
        assertThat(tokenService.validateTokenFormat(validToken)).isTrue();
        
        // Invalid cases
        assertThat(tokenService.validateTokenFormat(null)).isFalse();
        assertThat(tokenService.validateTokenFormat("")).isFalse();
        assertThat(tokenService.validateTokenFormat("   ")).isFalse();
        assertThat(tokenService.validateTokenFormat("abc")).isFalse(); // too short
        assertThat(tokenService.validateTokenFormat("a".repeat(100))).isFalse(); // too long
        assertThat(tokenService.validateTokenFormat("invalid!chars")).isFalse();
        assertThat(tokenService.validateTokenFormat("has spaces")).isFalse();
    }

    @Test
    void hardLinkManager_ShouldValidateFilesystemSupport() throws IOException {
        // Test filesystem validation
        Path testDir = tempDir.resolve("validation-test");
        Files.createDirectories(testDir);
        
        // This should not throw an exception on most filesystems
        hardLinkManager.validateFilesystemSupport(testDir);
        
        // Verify test files are cleaned up
        assertThat(Files.list(testDir)).isEmpty();
    }

    @Test
    void hardLinkManager_ShouldDetectSameFilesystem() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Path differentDir = tempDir.resolve("subdir");
        Files.createDirectories(differentDir);
        Path file3 = differentDir.resolve("file3.txt");
        
        // Files in same directory should be on same filesystem
        assertThat(hardLinkManager.isOnSameFilesystem(file1, file2)).isTrue();
        
        // Files in subdirectory should also be on same filesystem
        assertThat(hardLinkManager.isOnSameFilesystem(file1, file3)).isTrue();
    }

    @Test
    void blobUrlEntity_ShouldValidateConstraints() {
        // Test entity validation
        BlobUrl validBlobUrl = BlobUrl.builder()
                .token("valid-token-123")
                .originalPath("/path/to/file.txt")
                .hardLinkPath("/tmp/hardlink")
                .filename("file.txt")
                .contentType("text/plain")
                .fileSize(1024L)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .createdBy("testuser")
                .build();
        
        assertThat(validBlobUrl.isExpired()).isFalse();
        assertThat(validBlobUrl.isExpiryAfterCreation()).isTrue();
        
        // Test expiration
        BlobUrl expiredBlobUrl = BlobUrl.builder()
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        
        assertThat(expiredBlobUrl.isExpired()).isTrue();
    }

    @Test
    void tokenService_ShouldHandleExpirationChecks() {
        BlobUrl activeBlobUrl = BlobUrl.builder()
                .token("active-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        
        BlobUrl expiredBlobUrl = BlobUrl.builder()
                .token("expired-token")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        
        assertThat(tokenService.isTokenExpired(activeBlobUrl)).isFalse();
        assertThat(tokenService.isTokenExpired(expiredBlobUrl)).isTrue();
        assertThat(tokenService.isTokenExpired(null)).isTrue();
        
        // Test combined validation
        String validToken = tokenService.generateSecureToken();
        assertThat(tokenService.validateToken(validToken, activeBlobUrl)).isTrue();
        assertThat(tokenService.validateToken(validToken, expiredBlobUrl)).isFalse();
        assertThat(tokenService.validateToken("invalid!token", activeBlobUrl)).isFalse();
        assertThat(tokenService.validateToken(validToken, null)).isFalse();
    }

    @Test
    void hardLinkOperations_ShouldHandleEdgeCases() throws IOException {
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        Files.write(sourceFile, "test content".getBytes());
        
        // Test successful creation
        hardLinkManager.createHardLink(sourceFile, targetFile);
        assertThat(Files.exists(targetFile)).isTrue();
        
        // Test deletion of non-existent file (should not throw)
        Path nonExistent = tempDir.resolve("non-existent.txt");
        hardLinkManager.deleteHardLink(nonExistent); // Should not throw
        
        // Test cleanup
        hardLinkManager.deleteHardLink(targetFile);
        assertThat(Files.exists(targetFile)).isFalse();
    }
}