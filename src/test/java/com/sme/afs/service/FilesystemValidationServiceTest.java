package com.sme.afs.service;

import com.sme.afs.config.BlobUrlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FilesystemValidationService.
 */
@ExtendWith(MockitoExtension.class)
class FilesystemValidationServiceTest {

    @Mock
    private BlobUrlProperties blobUrlProperties;

    @Mock
    private HardLinkManager hardLinkManager;

    @TempDir
    Path tempDir;

    private FilesystemValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new FilesystemValidationService(blobUrlProperties, hardLinkManager);
    }

    @Test
    void validateFilesystemOnStartup_WhenValidationEnabled_ShouldPerformValidation() {
        // Arrange
        HardLinkManager hardLinkManager = new HardLinkManager();
        FilesystemValidationService validationService = new FilesystemValidationService(blobUrlProperties, hardLinkManager);
        when(blobUrlProperties.isValidateFilesystemOnStartup()).thenReturn(true);
        when(blobUrlProperties.getTempDirectory()).thenReturn(tempDir.toString());

        // Act & Assert
        assertThatCode(validationService::validateFilesystemOnStartup)
                .doesNotThrowAnyException();
    }

    @Test
    void validateFilesystemOnStartup_WhenValidationDisabled_ShouldSkipValidation() {
        // Arrange
        when(blobUrlProperties.isValidateFilesystemOnStartup()).thenReturn(false);

        // Act
        validationService.validateFilesystemOnStartup();

        // Assert
        verify(blobUrlProperties, never()).getTempDirectory();
    }

    @Test
    void validateFilesystem_WhenAllValidationsPassed_ShouldReturnSuccess() throws IOException {
        // Arrange
        when(blobUrlProperties.getTempDirectory()).thenReturn(tempDir.toString());
        HardLinkManager hardLinkManagerLocal = new HardLinkManager();
        FilesystemValidationService validationServiceLocal = new FilesystemValidationService(blobUrlProperties, hardLinkManagerLocal);

        // Create test files for hard link validation
        Path testFile = tempDir.resolve("test-file");
        Files.write(testFile, "test content".getBytes());

        // Act
        FilesystemValidationService.ValidationResult result = validationServiceLocal.validateFilesystem();

        // Assert
        assertThat(result.isValid()).isTrue();
        assertThat(result.isCritical()).isFalse();
        assertThat(result.getMessage()).contains("successfully");
    }

    @Test
    void validateFilesystem_WhenTempDirectoryDoesNotExist_ShouldCreateAndValidate() {
        // Arrange
        Path nonExistentDir = tempDir.resolve("non-existent");
        when(blobUrlProperties.getTempDirectory()).thenReturn(nonExistentDir.toString());

        // Act
        validationService.validateFilesystem();

        // Assert
        assertThat(Files.exists(nonExistentDir)).isTrue();
        assertThat(Files.isDirectory(nonExistentDir)).isTrue();
    }

    @Test
    void validateFilesystem_WhenTempPathIsFile_ShouldReturnCriticalFailure() throws IOException {
        // Arrange
        Path fileInsteadOfDir = tempDir.resolve("file-not-dir");
        Files.createFile(fileInsteadOfDir);
        when(blobUrlProperties.getTempDirectory()).thenReturn(fileInsteadOfDir.toString());

        // Act
        FilesystemValidationService.ValidationResult result = validationService.validateFilesystem();

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.isCritical()).isTrue();
        assertThat(result.getMessage()).contains("not a directory");
    }

    @Test
    void validateFilesystem_WhenHardLinkCreationFails_ShouldReturnCriticalFailure() throws IOException {
        // Arrange
        when(blobUrlProperties.getTempDirectory()).thenReturn(tempDir.toString());
        doThrow(new IOException("Hard link not supported")).when(hardLinkManager)
                .createHardLink(any(Path.class), any(Path.class));

        // Act
        FilesystemValidationService.ValidationResult result = validationService.validateFilesystem();

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.isCritical()).isTrue();
        assertThat(result.getMessage()).contains("does not support hard links");
    }

    @Test
    void validateFilesystem_WhenUnexpectedErrorOccurs_ShouldReturnCriticalFailure() {
        // Arrange
        when(blobUrlProperties.getTempDirectory()).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        FilesystemValidationService.ValidationResult result = validationService.validateFilesystem();

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.isCritical()).isTrue();
        assertThat(result.getMessage()).contains("Unexpected validation error");
    }

    @Test
    void getFilesystemInfo_WhenTempDirectoryExists_ShouldReturnCorrectInfo() {
        // Arrange
        when(blobUrlProperties.getTempDirectory()).thenReturn(tempDir.toString());

        // Act
        FilesystemValidationService.FilesystemInfo info = validationService.getFilesystemInfo();

        // Assert
        assertThat(info.getTempDirectoryPath()).isEqualTo(tempDir.toString());
        assertThat(info.isExists()).isTrue();
        assertThat(info.getFileSystemType()).isNotNull();
        assertThat(info.getTotalSpace()).isNotNull();
        assertThat(info.getUsableSpace()).isNotNull();
        assertThat(info.getUnallocatedSpace()).isNotNull();
        assertThat(info.getReadOnly()).isNotNull();
        assertThat(info.getSupportsFileAttributeView()).isNotNull();
        assertThat(info.getError()).isNull();
    }

    @Test
    void getFilesystemInfo_WhenTempDirectoryDoesNotExist_ShouldReturnNonExistentInfo() {
        // Arrange
        Path nonExistentDir = tempDir.resolve("non-existent");
        when(blobUrlProperties.getTempDirectory()).thenReturn(nonExistentDir.toString());

        // Act
        FilesystemValidationService.FilesystemInfo info = validationService.getFilesystemInfo();

        // Assert
        assertThat(info.getTempDirectoryPath()).isEqualTo(nonExistentDir.toString());
        assertThat(info.isExists()).isFalse();
        assertThat(info.getFileSystemType()).isNull();
        assertThat(info.getTotalSpace()).isNull();
        assertThat(info.getUsableSpace()).isNull();
        assertThat(info.getUnallocatedSpace()).isNull();
        assertThat(info.getReadOnly()).isNull();
        assertThat(info.getSupportsFileAttributeView()).isNull();
        assertThat(info.getError()).isNull();
    }

    @Test
    void getFilesystemInfo_WhenIOExceptionOccurs_ShouldReturnErrorInfo() {
        // Arrange
        String os = System.getProperty("os.name").toLowerCase();
        String invalidPath = "/invalid/path/that/causes/error";
        String expected = os.contains("win") ? invalidPath.replace("/", "\\") : invalidPath;
        when(blobUrlProperties.getTempDirectory()).thenReturn(invalidPath);

        // Act
        FilesystemValidationService.FilesystemInfo info = validationService.getFilesystemInfo();

        // Assert
        assertThat(info.getTempDirectoryPath()).isEqualTo(expected);
        assertThat(info.isExists()).isFalse();
        assertThat(info.getError()).isNull();
    }

    @Test
    void validationResult_FactoryMethods_ShouldCreateCorrectInstances() {
        // Act & Assert
        FilesystemValidationService.ValidationResult success = 
                FilesystemValidationService.ValidationResult.success("All good");
        assertThat(success.isValid()).isTrue();
        assertThat(success.isCritical()).isFalse();
        assertThat(success.getMessage()).isEqualTo("All good");

        FilesystemValidationService.ValidationResult failure = 
                FilesystemValidationService.ValidationResult.failure("Minor issue");
        assertThat(failure.isValid()).isFalse();
        assertThat(failure.isCritical()).isFalse();
        assertThat(failure.getMessage()).isEqualTo("Minor issue");

        FilesystemValidationService.ValidationResult criticalFailure = 
                FilesystemValidationService.ValidationResult.criticalFailure("Critical issue");
        assertThat(criticalFailure.isValid()).isFalse();
        assertThat(criticalFailure.isCritical()).isTrue();
        assertThat(criticalFailure.getMessage()).isEqualTo("Critical issue");
    }

    @Test
    void validateFilesystemOnStartup_WhenCriticalValidationFails_ShouldThrowException() {
        // Arrange
        when(blobUrlProperties.isValidateFilesystemOnStartup()).thenReturn(true);
        when(blobUrlProperties.getTempDirectory()).thenReturn("/invalid/path");

        // Act & Assert
        assertThatThrownBy(() -> validationService.validateFilesystemOnStartup())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Filesystem validation failed");
    }

    @Test
    void validateFilesystemOnStartup_WhenValidationThrowsException_ShouldWrapInIllegalStateException() {
        // Arrange
        when(blobUrlProperties.isValidateFilesystemOnStartup()).thenReturn(true);
        when(blobUrlProperties.getTempDirectory()).thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        assertThatThrownBy(() -> validationService.validateFilesystemOnStartup())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Filesystem validation failed")
                .hasCauseInstanceOf(RuntimeException.class);
    }
}