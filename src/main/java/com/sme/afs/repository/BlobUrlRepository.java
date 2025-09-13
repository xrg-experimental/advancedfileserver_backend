package com.sme.afs.repository;

import com.sme.afs.model.BlobUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for BlobUrl entities with custom queries for cleanup operations.
 */
@Repository
public interface BlobUrlRepository extends JpaRepository<BlobUrl, String> {

    /**
     * Count the total number of active blob URLs in the system.
     *
     * @param currentTime Current timestamp to compare against
     * @return Total number of active blob URLs
     */
    @Query("SELECT COUNT(b) FROM BlobUrl b WHERE b.expiresAt > :currentTime")
    long countActiveUrls(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Count the number of active blob URLs for a specific user.
     *
     * @param createdBy   Username of the creator
     * @param currentTime Current timestamp to compare against
     * @return Number of active blob URLs for the user
     */
    @Query("SELECT COUNT(b) FROM BlobUrl b WHERE b.createdBy = :createdBy AND b.expiresAt > :currentTime")
    long countActiveUrlsByUser(@Param("createdBy") String createdBy,
                               @Param("currentTime") LocalDateTime currentTime);

    /**
     * Delete all expired blob URLs.
     * This is used for cleanup operations.
     *
     * @param currentTime Current timestamp to compare against
     * @return Number of deleted records
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM BlobUrl b WHERE b.expiresAt <= :currentTime")
    int deleteExpiredUrls(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find a blob URL by token if it's still active.
     *
     * @param token       The blob URL token
     * @param currentTime Current timestamp to check expiration
     * @return Optional containing the blob URL if active, empty if expired or not found
     */
    @Query("SELECT b FROM BlobUrl b WHERE b.token = :token AND b.expiresAt > :currentTime")
    Optional<BlobUrl> findActiveByToken(@Param("token") String token,
                                        @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find blob URLs by original file path.
     * Useful for checking if a file already has active blob URLs.
     *
     * @param originalPath Path to the original file
     * @param currentTime  Current timestamp to filter active URLs
     * @return List of active blob URLs for the file
     */
    @Query("SELECT b FROM BlobUrl b WHERE b.originalPath = :originalPath AND b.expiresAt > :currentTime")
    List<BlobUrl> findActiveUrlsByOriginalPath(@Param("originalPath") String originalPath,
                                               @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find all blob URLs created by a specific user that are still active.
     *
     * @param createdBy   Username of the creator
     * @param currentTime Current timestamp to compare against
     * @return List of active blob URLs created by the user
     */
    @Query("SELECT b FROM BlobUrl b WHERE b.createdBy = :createdBy AND b.expiresAt > :currentTime")
    List<BlobUrl> findActiveUrlsByUser(@Param("createdBy") String createdBy,
                                       @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find all blob URLs created by a specific user.
     *
     * @param createdBy Username of the creator
     * @return List of blob URLs created by the user
     */
    List<BlobUrl> findByCreatedBy(String createdBy);

    /**
     * Find all blob URLs that have expired.
     *
     * @param currentTime Current timestamp to compare against
     * @return List of expired blob URLs
     */
    @Query("SELECT b FROM BlobUrl b WHERE b.expiresAt <= :currentTime")
    List<BlobUrl> findExpiredUrls(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find blob URLs that are about to expire within the specified time window.
     * Useful for proactive cleanup or user notifications.
     *
     * @param currentTime      Current timestamp
     * @param expirationWindow Time window for upcoming expirations
     * @return List of blob URLs expiring soon
     */
    @Query("SELECT b FROM BlobUrl b WHERE b.expiresAt > :currentTime AND b.expiresAt <= :expirationWindow")
    List<BlobUrl> findUrlsExpiringWithin(@Param("currentTime") LocalDateTime currentTime,
                                         @Param("expirationWindow") LocalDateTime expirationWindow);
}