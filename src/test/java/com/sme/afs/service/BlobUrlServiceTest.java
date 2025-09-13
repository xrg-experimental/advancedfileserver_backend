package com.sme.afs.service;

import com.sme.afs.config.BlobUrlProperties;
import com.sme.afs.dto.FileInfoResponse;
import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.BlobUrl;
import com.sme.afs.repository.BlobUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BlobUrlServiceTest {

    @Mock
    private BlobUrlRepository blobUrlRepository;
    
    @Mock
    private TokenService tokenService;
    
    @Mock
    private HardLinkManager hardLinkManager;
    
    @Mock
    private FileService fileService;
    
    @Mock
    private BlobUrlProperties blobUrlProperties;

    private BlobUrlService blobUrlService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        lenient().when(blobUrlProperties.getTempDirectory()).thenReturn(tempDir.toString());
        lenient().when(blobUrlProperties.getDefaultExpiration()).thenReturn(Duration.ofHours(1));
        lenient().when(blobUrlProperties.getMaxConcurrentUrls()).thenReturn(1000L);
        
        blobUrlService = new BlobUrlService(
                blobUrlRepository, tokenService, hardLinkManager, fileService, blobUrlProperties);
    }

    @Test
    void createBlobUrl_ShouldCreateSuccessfully() throws IOException {
        // Arrange
        String filePath = "test/file.txt";
        String createdBy = "testuser";
        String token = "secure-token-123";
        
        FileInfoResponse fileInfo = new FileInfoResponse();
        fileInfo.setName("file.txt");
        fileInfo.setSize(1024L);
        fileInfo.setMimeType("text/plain");
        fileInfo.setDirectory(false);
        
        Path originalFile = tempDir.resolve("original.txt");
        Files.write(originalFile, "test content".getBytes());
        
        Resource mockResource = new UrlResource(originalFile.toUri());
        
        when(fileService.getFileInfo(filePath)).thenReturn(fileInfo);
        when(fileService.loadAsResource(filePath)).thenReturn(mockResource);
        when(tokenService.generateSecureToken()).thenReturn(token);
        when(blobUrlRepository.countActiveUrls(any(LocalDateTime.class))).thenReturn(0L);
        when(blobUrlRepository.save(any(BlobUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BlobUrl result = blobUrlService.createBlobUrl(filePath, createdBy);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(token);
        assertThat(result.getFilename()).isEqualTo("file.txt");
        assertThat(result.getContentType()).isEqualTo("text/plain");
        assertThat(result.getFileSize()).isEqualTo(1024L);
        assertThat(result.getCreatedBy()).isEqualTo(createdBy);
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
        
        verify(hardLinkManager).createHardLink(eq(originalFile), any(Path.class));
        verify(blobUrlRepository).save(any(BlobUrl.class));
    }

    @Test
    void createBlobUrl_ShouldThrowExceptionForDirectory() {
        // Arrange
        String filePath = "test/directory";
        String createdBy = "testuser";
        
        FileInfoResponse fileInfo = new FileInfoResponse();
        fileInfo.setDirectory(true);
        
        when(fileService.getFileInfo(filePath)).thenReturn(fileInfo);
        
        // Act & Assert
        assertThatThrownBy(() -> blobUrlService.createBlobUrl(filePath, createdBy))
                .isInstanceOf(AfsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED)
                .hasMessageContaining("Cannot create blob URL for directory");
    }

    @Test
    void createBlobUrl_ShouldThrowExceptionWhenMaxConcurrentUrlsReached() {
        // Arrange
        String filePath = "test/file.txt";
        String createdBy = "testuser";
        
        FileInfoResponse fileInfo = new FileInfoResponse();
        fileInfo.setDirectory(false);
        
        when(fileService.getFileInfo(filePath)).thenReturn(fileInfo);
        when(blobUrlRepository.countActiveUrls(any(LocalDateTime.class))).thenReturn(1000L);
        
        // Act & Assert
        assertThatThrownBy(() -> blobUrlService.createBlobUrl(filePath, createdBy))
                .isInstanceOf(AfsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED)
                .hasMessageContaining("Maximum concurrent blob URLs limit reached");
    }

    @Test
    void createBlobUrl_ShouldHandleHardLinkCreationFailure() throws IOException {
        // Arrange
        String filePath = "test/file.txt";
        String createdBy = "testuser";
        String token = "secure-token-123";
        
        FileInfoResponse fileInfo = new FileInfoResponse();
        fileInfo.setName("file.txt");
        fileInfo.setSize(1024L);
        fileInfo.setMimeType("text/plain");
        fileInfo.setDirectory(false);
        
        Path originalFile = tempDir.resolve("original.txt");
        Files.write(originalFile, "test content".getBytes());
        
        Resource mockResource = new UrlResource(originalFile.toUri());
        
        when(fileService.getFileInfo(filePath)).thenReturn(fileInfo);
        when(fileService.loadAsResource(filePath)).thenReturn(mockResource);
        when(tokenService.generateSecureToken()).thenReturn(token);
        when(blobUrlRepository.countActiveUrls(any(LocalDateTime.class))).thenReturn(0L);
        doThrow(new IOException("Hard link creation failed")).when(hardLinkManager)
                .createHardLink(any(Path.class), any(Path.class));
        
        // Act & Assert
        assertThatThrownBy(() -> blobUrlService.createBlobUrl(filePath, createdBy))
                .isInstanceOf(AfsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_ERROR)
                .hasMessageContaining("Failed to create temporary download link");
        
        verify(blobUrlRepository).deleteById(token);
    }

    @Test
    void getBlobUrlStatus_ShouldReturnBlobUrlForValidToken() {
        // Arrange
        String token = "valid-token";
        BlobUrl blobUrl = BlobUrl.builder()
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        
        when(tokenService.validateTokenFormat(token)).thenReturn(true);
        when(blobUrlRepository.findById(token)).thenReturn(Optional.of(blobUrl));
        when(tokenService.isTokenExpired(blobUrl)).thenReturn(false);
        
        // Act
        Optional<BlobUrl> result = blobUrlService.getBlobUrlStatusInternal(token);
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(blobUrl);
    }

    @Test
    void getBlobUrlStatus_ShouldReturnEmptyForInvalidTokenFormat() {
        // Arrange
        String token = "invalid-token!";
        
        when(tokenService.validateTokenFormat(token)).thenReturn(false);
        
        // Act
        Optional<BlobUrl> result = blobUrlService.getBlobUrlStatusInternal(token);
        
        // Assert
        assertThat(result).isEmpty();
        verify(blobUrlRepository, never()).findById(any());
    }

    @Test
    void getBlobUrlStatus_ShouldReturnEmptyForNonExistentToken() {
        // Arrange
        String token = "non-existent-token";
        
        when(tokenService.validateTokenFormat(token)).thenReturn(true);
        when(blobUrlRepository.findById(token)).thenReturn(Optional.empty());
        
        // Act
        Optional<BlobUrl> result = blobUrlService.getBlobUrlStatusInternal(token);
        
        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getBlobUrlStatus_ShouldReturnEmptyForExpiredToken() {
        // Arrange
        String token = "expired-token";
        BlobUrl blobUrl = BlobUrl.builder()
                .token(token)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        
        when(tokenService.validateTokenFormat(token)).thenReturn(true);
        when(blobUrlRepository.findById(token)).thenReturn(Optional.of(blobUrl));
        when(tokenService.isTokenExpired(blobUrl)).thenReturn(true);
        
        // Act
        Optional<BlobUrl> result = blobUrlService.getBlobUrlStatusInternal(token);
        
        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void validateAndGetFile_ShouldReturnResourceForValidToken() throws IOException {
        // Arrange
        String token = "valid-token";
        Path hardLinkFile = tempDir.resolve("hardlink.txt");
        Files.write(hardLinkFile, "test content".getBytes());
        
        BlobUrl blobUrl = BlobUrl.builder()
                .token(token)
                .hardLinkPath(hardLinkFile.toString())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        
        when(tokenService.validateTokenFormat(token)).thenReturn(true);
        when(blobUrlRepository.findById(token)).thenReturn(Optional.of(blobUrl));
        when(tokenService.isTokenExpired(blobUrl)).thenReturn(false);
        
        // Act
        Resource result = blobUrlService.validateAndGetFile(token);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
        assertThat(result.isReadable()).isTrue();
    }

    @Test
    void validateAndGetFile_ShouldThrowExceptionForInvalidToken() {
        // Arrange
        String token = "invalid-token";
        
        when(tokenService.validateTokenFormat(token)).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> blobUrlService.validateAndGetFile(token))
                .isInstanceOf(AfsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND)
                .hasMessageContaining("Download URL is invalid or expired");
    }

    @Test
    void validateAndGetFile_ShouldThrowExceptionWhenHardLinkMissing() {
        // Arrange
        String token = "valid-token";
        Path nonExistentFile = tempDir.resolve("missing.txt");
        
        BlobUrl blobUrl = BlobUrl.builder()
                .token(token)
                .hardLinkPath(nonExistentFile.toString())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        
        when(tokenService.validateTokenFormat(token)).thenReturn(true);
        when(blobUrlRepository.findById(token)).thenReturn(Optional.of(blobUrl));
        when(tokenService.isTokenExpired(blobUrl)).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> blobUrlService.validateAndGetFile(token))
                .isInstanceOf(AfsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND)
                .hasMessageContaining("Download file is no longer available");
    }

    @Test
    void cleanupExpiredUrls_ShouldCleanupSuccessfully() throws IOException {
        // Arrange
        Path hardLink1 = tempDir.resolve("link1.txt");
        Path hardLink2 = tempDir.resolve("link2.txt");
        Files.write(hardLink1, "content1".getBytes());
        Files.write(hardLink2, "content2".getBytes());
        
        BlobUrl expiredUrl1 = BlobUrl.builder()
                .token("token1")
                .hardLinkPath(hardLink1.toString())
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        
        BlobUrl expiredUrl2 = BlobUrl.builder()
                .token("token2")
                .hardLinkPath(hardLink2.toString())
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        
        when(blobUrlRepository.findExpiredUrls(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredUrl1, expiredUrl2));
        
        // Act
        int cleanedCount = blobUrlService.cleanupExpiredUrls();
        
        // Assert
        assertThat(cleanedCount).isEqualTo(2);
        verify(hardLinkManager).deleteHardLink(hardLink1);
        verify(hardLinkManager).deleteHardLink(hardLink2);
        verify(blobUrlRepository).delete(expiredUrl1);
        verify(blobUrlRepository).delete(expiredUrl2);
    }

    @Test
    void cleanupExpiredUrls_ShouldContinueOnIndividualFailures() throws IOException {
        // Arrange
        Path hardLink1 = tempDir.resolve("link1.txt");
        Path hardLink2 = tempDir.resolve("link2.txt");
        Files.write(hardLink1, "content1".getBytes());
        Files.write(hardLink2, "content2".getBytes());
        
        BlobUrl expiredUrl1 = BlobUrl.builder()
                .token("token1")
                .hardLinkPath(hardLink1.toString())
                .build();
        
        BlobUrl expiredUrl2 = BlobUrl.builder()
                .token("token2")
                .hardLinkPath(hardLink2.toString())
                .build();
        
        when(blobUrlRepository.findExpiredUrls(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredUrl1, expiredUrl2));
        
        // First cleanup fails, second succeeds
        doThrow(new IOException("Cleanup failed")).when(hardLinkManager).deleteHardLink(hardLink1);
        doNothing().when(hardLinkManager).deleteHardLink(hardLink2);
        
        // Act
        int cleanedCount = blobUrlService.cleanupExpiredUrls();
        
        // Assert
        assertThat(cleanedCount).isEqualTo(1); // Only second one succeeded
        verify(blobUrlRepository).delete(expiredUrl2);
        verify(blobUrlRepository, never()).delete(expiredUrl1);
    }

    @Test
    void getActiveUrlsByUser_ShouldReturnUserUrls() {
        // Arrange
        String username = "testuser";
        List<BlobUrl> expectedUrls = Arrays.asList(
                BlobUrl.builder().token("token1").createdBy(username).build(),
                BlobUrl.builder().token("token2").createdBy(username).build()
        );
        
        when(blobUrlRepository.findActiveUrlsByUser(eq(username), any(LocalDateTime.class)))
                .thenReturn(expectedUrls);
        
        // Act
        List<BlobUrl> result = blobUrlService.getActiveUrlsByUser(username);
        
        // Assert
        assertThat(result).isEqualTo(expectedUrls);
    }

    @Test
    void getActiveUrlCount_ShouldReturnTotalCount() {
        // Arrange
        when(blobUrlRepository.countActiveUrls(any(LocalDateTime.class))).thenReturn(42L);
        
        // Act
        long result = blobUrlService.getActiveUrlCount();
        
        // Assert
        assertThat(result).isEqualTo(42L);
    }

    @Test
    void getActiveUrlCountByUser_ShouldReturnUserCount() {
        // Arrange
        String username = "testuser";
        when(blobUrlRepository.countActiveUrlsByUser(eq(username), any(LocalDateTime.class)))
                .thenReturn(5L);
        
        // Act
        long result = blobUrlService.getActiveUrlCountByUser(username);
        
        // Assert
        assertThat(result).isEqualTo(5L);
    }
}