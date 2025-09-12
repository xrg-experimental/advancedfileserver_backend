package com.sme.afs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HardLinkManager service.
 * Tests hard link creation, deletion, and validation operations.
 */
@SpringBootTest
@ActiveProfiles("test")
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

        Files.write(sourceFile, testContent.getBytes(StandardCharsets.UTF_8));

        // Act
        hardLinkManager.createHardLink(sourceFile, targetFile);

        // Assert
        assertTrue(Files.exists(targetDir), "Target directory should be created");
        assertTrue(Files.exists(targetFile), "Target file should exist");
        assertEquals(testContent, Files.readString(targetFile));
    }

    @Test
    void testCreateHardLink_IntegrationWithDeletion() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        String testContent = "Test content";

        Files.write(sourceFile, testContent.getBytes(StandardCharsets.UTF_8));
        hardLinkManager.createHardLink(sourceFile, targetFile);

        // Act - Delete the source file
        Files.delete(sourceFile);

        // Assert - Target should still exist and be accessible
        assertFalse(Files.exists(sourceFile));
        assertTrue(Files.exists(targetFile));
        assertEquals(testContent, Files.readString(targetFile));
    }

    @Test
    void testCreateHardLink_IntegrationWithModification() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        String initialContent = "Initial content";
        String modifiedContent = "Modified content";

        Files.write(sourceFile, initialContent.getBytes(StandardCharsets.UTF_8));
        hardLinkManager.createHardLink(sourceFile, targetFile);

        // Act - Modify content through the source file
        Files.write(sourceFile, modifiedContent.getBytes(StandardCharsets.UTF_8));

        // Assert - Both files should reflect the change (they're hard linked)
        assertEquals(modifiedContent, Files.readString(sourceFile));
        assertEquals(modifiedContent, Files.readString(targetFile));
    }

    @Test
    void testCreateHardLink_OverwritesExistingTarget() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        String sourceContent = "Source content";
        String oldTargetContent = "Old target content";

        Files.write(sourceFile, sourceContent.getBytes(StandardCharsets.UTF_8));
        Files.write(targetFile, oldTargetContent.getBytes(StandardCharsets.UTF_8));

        // Act
        hardLinkManager.createHardLink(sourceFile, targetFile);

        // Assert
        assertTrue(Files.exists(targetFile));
        assertEquals(sourceContent, Files.readString(targetFile));
    }

    @Test
    void testCreateHardLink_SourceFileNotExists() {
        // Arrange
        Path nonExistentSource = tempDir.resolve("nonexistent.txt");
        Path targetFile = tempDir.resolve("target.txt");

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () ->
                hardLinkManager.createHardLink(nonExistentSource, targetFile));

        assertTrue(exception.getMessage().contains("Source file does not exist"));
    }

    @Test
    void testCreateHardLink_SourceIsDirectory() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("sourcedir");
        Path targetFile = tempDir.resolve("target.txt");
        Files.createDirectory(sourceDir);

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () ->
                hardLinkManager.createHardLink(sourceDir, targetFile));

        assertTrue(exception.getMessage().contains("Source is not a regular file"));
    }

    @Test
    void testCreateHardLink_Success() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        String testContent = "Test content for hard link";

        Files.write(sourceFile, testContent.getBytes(StandardCharsets.UTF_8));

        // Act
        hardLinkManager.createHardLink(sourceFile, targetFile);

        // Assert
        assertTrue(Files.exists(targetFile), "Target hard link should exist");
        assertEquals(testContent, Files.readString(targetFile), "Content should match source");

        // Verify both files point to the same content
        byte[] sourceContent = Files.readAllBytes(sourceFile);
        byte[] targetContent = Files.readAllBytes(targetFile);
        assertArrayEquals(sourceContent, targetContent, "Hard link content should match source");
    }

    @Test
    void testCreateHardLink_WithSpecialCharactersInFilename() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source with spaces & symbols!.txt");
        Path targetFile = tempDir.resolve("target-with-dashes_and_underscores.txt");
        String testContent = "Content with special filename";

        Files.write(sourceFile, testContent.getBytes(StandardCharsets.UTF_8));

        // Act
        hardLinkManager.createHardLink(sourceFile, targetFile);

        // Assert
        assertTrue(Files.exists(targetFile));
        assertEquals(testContent, Files.readString(targetFile));
    }

    @Test
    void testDeleteHardLink_NonExistentFile() {
        // Arrange
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> hardLinkManager.deleteHardLink(nonExistentFile));
    }

    @Test
    void testDeleteHardLink_Success() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        String testContent = "Test content";

        Files.write(sourceFile, testContent.getBytes(StandardCharsets.UTF_8));
        hardLinkManager.createHardLink(sourceFile, targetFile);

        // Verify hard link exists
        assertTrue(Files.exists(targetFile));

        // Act
        hardLinkManager.deleteHardLink(targetFile);

        // Assert
        assertFalse(Files.exists(targetFile), "Hard link should be deleted");
        assertTrue(Files.exists(sourceFile), "Original file should still exist");
        assertEquals(testContent, Files.readString(sourceFile), "Original file content should be unchanged");
    }

    @Test
    void testIsOnSameFilesystem_SameDirectory() throws IOException {
        // Arrange
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");

        Files.write(file1, "content1".getBytes(StandardCharsets.UTF_8));
        Files.write(file2, "content2".getBytes(StandardCharsets.UTF_8));

        // Act
        boolean result = hardLinkManager.isOnSameFilesystem(file1, file2);

        // Assert
        assertTrue(result, "Files in same directory should be on same filesystem");
    }

    @Test
    void testIsOnSameFilesystem_SameFilesystemDifferentDirectories() throws IOException {
        // Arrange
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectory(subDir);

        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = subDir.resolve("file2.txt");

        Files.write(file1, "content1".getBytes(StandardCharsets.UTF_8));
        Files.write(file2, "content2".getBytes(StandardCharsets.UTF_8));

        // Act
        boolean result = hardLinkManager.isOnSameFilesystem(file1, file2);

        // Assert
        assertTrue(result, "Files in subdirectories should be on same filesystem");
    }

    @Test
    void testValidateFilesystemSupport_Success() {
        // Act & Assert - Should not throw exception on most modern filesystems
        assertDoesNotThrow(() -> hardLinkManager.validateFilesystemSupport(tempDir));
    }
}