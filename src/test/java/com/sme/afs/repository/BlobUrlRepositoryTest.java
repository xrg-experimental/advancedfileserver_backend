package com.sme.afs.repository;

import com.sme.afs.model.BlobUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for BlobUrlRepository.
 * Tests database operations and custom queries.
 */
@DataJpaTest
@ActiveProfiles("test")
class BlobUrlRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private BlobUrlRepository blobUrlRepository;
    
    private BlobUrl activeBlobUrl;
    private BlobUrl expiredBlobUrl;
    private BlobUrl anotherActiveBlobUrl;
    
    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        // Create test data
        activeBlobUrl = BlobUrl.builder()
                .token("active-token-123")
                .originalPath("/path/to/original/file.txt")
                .hardLinkPath("/tmp/afs-downloads/active-link.txt")
                .filename("file.txt")
                .contentType("text/plain")
                .fileSize(1024L)
                .createdAt(now.minusMinutes(30))
                .expiresAt(now.plusMinutes(30))
                .createdBy("testuser")
                .build();
        
        expiredBlobUrl = BlobUrl.builder()
                .token("expired-token-456")
                .originalPath("/path/to/expired/file.txt")
                .hardLinkPath("/tmp/afs-downloads/expired-link.txt")
                .filename("expired.txt")
                .contentType("text/plain")
                .fileSize(2048L)
                .createdAt(now.minusHours(2))
                .expiresAt(now.minusMinutes(30))
                .createdBy("testuser")
                .build();
        
        anotherActiveBlobUrl = BlobUrl.builder()
                .token("another-active-789")
                .originalPath("/path/to/another/file.txt")
                .hardLinkPath("/tmp/afs-downloads/another-link.txt")
                .filename("another.txt")
                .contentType("application/pdf")
                .fileSize(4096L)
                .createdAt(now.minusMinutes(15))
                .expiresAt(now.plusHours(2)) // Changed to 2 hours to be outside the 1-hour window
                .createdBy("anotheruser")
                .build();
        
        // Persist test data
        entityManager.persist(activeBlobUrl);
        entityManager.persist(expiredBlobUrl);
        entityManager.persist(anotherActiveBlobUrl);
        entityManager.flush();
    }
    
    @Test
    void testSaveAndFindById() {
        // Arrange
        BlobUrl newBlobUrl = BlobUrl.builder()
                .token("new-token-999")
                .originalPath("/path/to/new/file.txt")
                .hardLinkPath("/tmp/afs-downloads/new-link.txt")
                .filename("new.txt")
                .contentType("text/plain")
                .fileSize(512L)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .createdBy("newuser")
                .build();
        
        // Act
        BlobUrl saved = blobUrlRepository.save(newBlobUrl);
        Optional<BlobUrl> found = blobUrlRepository.findById("new-token-999");
        
        // Assert
        assertNotNull(saved);
        assertEquals("new-token-999", saved.getToken());
        assertTrue(found.isPresent());
        assertEquals("new.txt", found.get().getFilename());
        assertEquals("newuser", found.get().getCreatedBy());
    }
    
    @Test
    void testFindExpiredUrls() {
        // Arrange
        LocalDateTime currentTime = LocalDateTime.now();
        
        // Act
        List<BlobUrl> expiredUrls = blobUrlRepository.findExpiredUrls(currentTime);
        
        // Assert
        assertEquals(1, expiredUrls.size());
        assertEquals("expired-token-456", expiredUrls.get(0).getToken());
        assertTrue(expiredUrls.get(0).isExpired());
    }
    
    @Test
    void testFindByCreatedBy() {
        // Act
        List<BlobUrl> testUserUrls = blobUrlRepository.findByCreatedBy("testuser");
        List<BlobUrl> anotherUserUrls = blobUrlRepository.findByCreatedBy("anotheruser");
        
        // Assert
        assertEquals(2, testUserUrls.size());
        assertEquals(1, anotherUserUrls.size());
        
        assertTrue(testUserUrls.stream().allMatch(url -> "testuser".equals(url.getCreatedBy())));
        assertTrue(anotherUserUrls.stream().allMatch(url -> "anotheruser".equals(url.getCreatedBy())));
    }
    
    @Test
    void testFindActiveUrlsByUser() {
        // Arrange
        LocalDateTime currentTime = LocalDateTime.now();
        
        // Act
        List<BlobUrl> activeTestUserUrls = blobUrlRepository.findActiveUrlsByUser("testuser", currentTime);
        List<BlobUrl> activeAnotherUserUrls = blobUrlRepository.findActiveUrlsByUser("anotheruser", currentTime);
        
        // Assert
        assertEquals(1, activeTestUserUrls.size());
        assertEquals("active-token-123", activeTestUserUrls.get(0).getToken());
        
        assertEquals(1, activeAnotherUserUrls.size());
        assertEquals("another-active-789", activeAnotherUserUrls.get(0).getToken());
    }
    
    @Test
    void testCountActiveUrlsByUser() {
        // Arrange
        LocalDateTime currentTime = LocalDateTime.now();
        
        // Act
        long testUserCount = blobUrlRepository.countActiveUrlsByUser("testuser", currentTime);
        long anotherUserCount = blobUrlRepository.countActiveUrlsByUser("anotheruser", currentTime);
        long nonExistentUserCount = blobUrlRepository.countActiveUrlsByUser("nonexistent", currentTime);
        
        // Assert
        assertEquals(1, testUserCount);
        assertEquals(1, anotherUserCount);
        assertEquals(0, nonExistentUserCount);
    }
    
    @Test
    void testCountActiveUrls() {
        // Arrange
        LocalDateTime currentTime = LocalDateTime.now();
        
        // Act
        long activeCount = blobUrlRepository.countActiveUrls(currentTime);
        
        // Assert
        assertEquals(2, activeCount); // activeBlobUrl and anotherActiveBlobUrl
    }
    
    @Test
    @Transactional
    void testDeleteExpiredUrls() {
        // Arrange
        LocalDateTime currentTime = LocalDateTime.now();
        long initialCount = blobUrlRepository.count();
        
        // Act
        int deletedCount = blobUrlRepository.deleteExpiredUrls(currentTime);
        entityManager.flush(); // Force the delete to be executed
        entityManager.clear(); // Clear the persistence context
        
        // Assert
        assertEquals(1, deletedCount);
        assertEquals(initialCount - 1, blobUrlRepository.count());
        
        // Verify the expired URL is gone
        Optional<BlobUrl> expiredUrl = blobUrlRepository.findById("expired-token-456");
        assertFalse(expiredUrl.isPresent());
        
        // Verify active URLs are still there
        Optional<BlobUrl> activeUrl = blobUrlRepository.findById("active-token-123");
        assertTrue(activeUrl.isPresent());
    }
    
    @Test
    void testFindUrlsExpiringWithin() {
        // Arrange
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime oneHourFromNow = currentTime.plusHours(1);
        
        // Act
        List<BlobUrl> expiringUrls = blobUrlRepository.findUrlsExpiringWithin(currentTime, oneHourFromNow);
        
        // Assert
        assertEquals(1, expiringUrls.size());
        assertEquals("active-token-123", expiringUrls.get(0).getToken());
        
        // The other active URL expires after 1 hour, so it shouldn't be included
        assertFalse(expiringUrls.stream().anyMatch(url -> "another-active-789".equals(url.getToken())));
    }
    
    @Test
    void testFindActiveUrlsByOriginalPath() {
        // Arrange
        LocalDateTime currentTime = LocalDateTime.now();
        
        // Act
        List<BlobUrl> urlsForOriginalFile = blobUrlRepository.findActiveUrlsByOriginalPath(
                "/path/to/original/file.txt", currentTime);
        List<BlobUrl> urlsForNonExistentFile = blobUrlRepository.findActiveUrlsByOriginalPath(
                "/path/to/nonexistent/file.txt", currentTime);
        
        // Assert
        assertEquals(1, urlsForOriginalFile.size());
        assertEquals("active-token-123", urlsForOriginalFile.get(0).getToken());
        
        assertEquals(0, urlsForNonExistentFile.size());
    }
    
    @Test
    void testFindActiveByToken() {
        // Arrange
        LocalDateTime currentTime = LocalDateTime.now();
        
        // Act
        Optional<BlobUrl> activeUrl = blobUrlRepository.findActiveByToken("active-token-123", currentTime);
        Optional<BlobUrl> expiredUrl = blobUrlRepository.findActiveByToken("expired-token-456", currentTime);
        Optional<BlobUrl> nonExistentUrl = blobUrlRepository.findActiveByToken("nonexistent-token", currentTime);
        
        // Assert
        assertTrue(activeUrl.isPresent());
        assertEquals("file.txt", activeUrl.get().getFilename());
        
        assertFalse(expiredUrl.isPresent());
        assertFalse(nonExistentUrl.isPresent());
    }
    
    @Test
    void testBlobUrlIsExpiredMethod() {
        // Act & Assert
        assertFalse(activeBlobUrl.isExpired());
        assertTrue(expiredBlobUrl.isExpired());
        assertFalse(anotherActiveBlobUrl.isExpired());
    }
    
    @Test
    void testPrePersistCreatedAt() {
        // Arrange
        BlobUrl newBlobUrl = BlobUrl.builder()
                .token("test-prepersist")
                .originalPath("/test/path")
                .hardLinkPath("/tmp/test-link")
                .filename("test.txt")
                .contentType("text/plain")
                .fileSize(100L)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .createdBy("testuser")
                // Note: not setting createdAt to test @PrePersist
                .build();
        
        // Act
        BlobUrl saved = blobUrlRepository.save(newBlobUrl);
        
        // Assert
        assertNotNull(saved.getCreatedAt());
        assertTrue(saved.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(saved.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(1)));
    }
    
    @Test
    void testEntityConstraints() {
        // Test that required fields are enforced
        BlobUrl invalidBlobUrl = new BlobUrl();
        invalidBlobUrl.setToken("test-constraints");
        
        // This should fail due to null constraints
        assertThrows(Exception.class, () -> {
            blobUrlRepository.saveAndFlush(invalidBlobUrl);
        });
    }
}