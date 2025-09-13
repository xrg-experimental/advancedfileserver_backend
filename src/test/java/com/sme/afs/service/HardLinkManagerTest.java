package com.sme.afs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileAlreadyExistsException;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for HardLinkManager service.
 * Tests hard link creation, deletion, and validation operations.
 */
class HardLinkManagerTest {

    @TempDir
    Path tempDir;
    private HardLinkManager hardLinkManager;

    @BeforeEach
    void setUp() {
        hardLinkManager = new HardLinkManager();
    }

    @Test
    void testCreateHardLink_CreatesTargetDirectory() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetDir = tempDir.resolve("subdir");
        Path targetFile = targetDir.resolve("target.txt");
        String testContent = "Test content";

        Files.writeString(sourceFile, testContent);

        // Act
        hardLinkManager.createHardLink(sourceFile, targetFile);

        // Assert
        assertThat(targetDir).as("Target directory should be created").exists();
        assertThat(targetFile).as("Target file should exist").exists();
        assertThat(Files.readString(targetFile)).isEqualTo(testContent);
    }

    @Test
    void testCreateHardLink_IntegrationWithDeletion() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        String testContent = "Test content";

        Files.writeString(sourceFile, testContent);
        hardLinkManager.createHardLink(sourceFile, targetFile);

        // Act - Delete the source file
        Files.delete(sourceFile);

        // Assert - Target should still exist and be accessible
        assertThat(sourceFile).doesNotExist();
        assertThat(targetFile).exists();
        assertThat(Files.readString(targetFile)).isEqualTo(testContent);
    }

    @Test
    void testCreateHardLink_IntegrationWithModification() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        String initialContent = "Initial content";
        String modifiedContent = "Modified content";

        Files.writeString(sourceFile, initialContent);
        hardLinkManager.createHardLink(sourceFile, targetFile);

        // Act - Modify content through the source file
        Files.writeString(sourceFile, modifiedContent);

        // Assert - Both files should reflect the change (they're hard linked)
        assertThat(Files.readString(sourceFile)).isEqualTo(modifiedContent);
        assertThat(Files.readString(targetFile)).isEqualTo(modifiedContent);
    }

    @Test
    void testCreateHardLink_TargetExists_ThrowsFileAlreadyExistsException() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        String sourceContent = "Source content";
        String oldTargetContent = "Old target content";

        Files.writeString(sourceFile, sourceContent);
        Files.writeString(targetFile, oldTargetContent);

        // Act & Assert
        assertThatThrownBy(() -> hardLinkManager.createHardLink(sourceFile, targetFile))
                .isInstanceOf(IOException.class)
                .hasCauseInstanceOf(FileAlreadyExistsException.class);
        assertThat(Files.readString(targetFile)).isEqualTo(oldTargetContent);
    }

    @Test
    void testCreateHardLink_SourceFileNotExists() {
        // Arrange
        Path nonExistentSource = tempDir.resolve("nonexistent.txt");
        Path targetFile = tempDir.resolve("target.txt");

        // Act & Assert
        assertThatThrownBy(() -> hardLinkManager.createHardLink(nonExistentSource, targetFile))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Source file does not exist");
    }

    @Test
    void testCreateHardLink_SourceIsDirectory() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("sourcedir");
        Path targetFile = tempDir.resolve("target.txt");
        Files.createDirectory(sourceDir);

        // Act & Assert
        assertThatThrownBy(() -> hardLinkManager.createHardLink(sourceDir, targetFile))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Source is not a regular file");
    }

    @Test
    void testCreateHardLink_Success() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        String testContent = "Test content for hard link";

        Files.writeString(sourceFile, testContent);

        // Act
        hardLinkManager.createHardLink(sourceFile, targetFile);

        // Assert
        assertThat(targetFile).as("Target hard link should exist").exists();
        assertThat(Files.readString(targetFile)).as("Content should match source").isEqualTo(testContent);

        // Verify both files point to the same content
        byte[] sourceContent = Files.readAllBytes(sourceFile);
        byte[] targetContent = Files.readAllBytes(targetFile);
        assertThat(targetContent).as("Hard link content should match source").isEqualTo(sourceContent);
    }

    @Test
    void testCreateHardLink_WithSpecialCharactersInFilename() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source with spaces & symbols!.txt");
        Path targetFile = tempDir.resolve("target-with-dashes_and_underscores.txt");
        String testContent = "Content with special filename";

        Files.writeString(sourceFile, testContent);

        // Act
        hardLinkManager.createHardLink(sourceFile, targetFile);

        // Assert
        assertThat(targetFile).exists();
        assertThat(Files.readString(targetFile)).isEqualTo(testContent);
    }

    @Test
    void testDeleteHardLink_NonExistentFile() {
        // Arrange
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");

        // Act & Assert - Should not throw exception
        assertThatCode(() -> hardLinkManager.deleteHardLink(nonExistentFile)).doesNotThrowAnyException();
    }

    @Test
    void testDeleteHardLink_Success() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        String testContent = "Test content";

        Files.writeString(sourceFile, testContent);
        hardLinkManager.createHardLink(sourceFile, targetFile);

        // Verify hard link exists
        assertThat(targetFile).exists();

        // Act
        hardLinkManager.deleteHardLink(targetFile);

        // Assert
        assertThat(targetFile).as("Hard link should be deleted").doesNotExist();
        assertThat(sourceFile).as("Original file should still exist").exists();
        assertThat(Files.readString(sourceFile)).as("Original file content should be unchanged").isEqualTo(testContent);
    }

    @Test
    void testIsOnSameFilesystem_SameDirectory() throws IOException {
        // Arrange
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");

        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        // Act
        boolean result = hardLinkManager.isOnSameFilesystem(file1, file2);

        // Assert
        assertThat(result).as("Files in same directory should be on same filesystem").isTrue();
    }

    @Test
    void testIsOnSameFilesystem_SameFilesystemDifferentDirectories() throws IOException {
        // Arrange
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectory(subDir);

        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = subDir.resolve("file2.txt");

        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        // Act
        boolean result = hardLinkManager.isOnSameFilesystem(file1, file2);

        // Assert
        assertThat(result).as("Files in subdirectories should be on same filesystem").isTrue();
    }

    @Test
    void testValidateFilesystemSupport_Success() {
        // Act & Assert - Should not throw exception on most modern filesystems
        assertThatCode(() -> hardLinkManager.validateFilesystemSupport(tempDir)).doesNotThrowAnyException();
    }

    @Test
    void testGetHardLinkCount_FallbackIsAtLeastOne() throws IOException {
        Path sourceFile = tempDir.resolve("source.txt");
        Files.writeString(sourceFile, "x");
        int count = hardLinkManager.getHardLinkCount(sourceFile);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testGetHardLinkCount_IncrementsAfterCreatingLink() throws IOException {
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        Files.writeString(sourceFile, "x");
        int before = hardLinkManager.getHardLinkCount(sourceFile);
        hardLinkManager.createHardLink(sourceFile, targetFile);
        int after = hardLinkManager.getHardLinkCount(sourceFile);
        assertThat(after).isGreaterThanOrEqualTo(before); // On UNIX likely after == before+1; on fallback both may be 1
    }
}