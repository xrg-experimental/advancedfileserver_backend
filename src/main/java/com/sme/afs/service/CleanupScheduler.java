package com.sme.afs.service;

import com.sme.afs.config.BlobUrlProperties;
import com.sme.afs.model.BlobUrl;
import com.sme.afs.repository.BlobUrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled service for automatic cleanup of expired blob URLs and orphaned hard links.
 * Handles both periodic cleanup and startup orphan removal.
 */
@Service
@Slf4j
public class CleanupScheduler {

    private final BlobUrlService blobUrlService;
    private final BlobUrlRepository blobUrlRepository;
    private final HardLinkManager hardLinkManager;
    private final BlobUrlProperties blobUrlProperties;

    @Autowired
    public CleanupScheduler(BlobUrlService blobUrlService,
                           BlobUrlRepository blobUrlRepository,
                           HardLinkManager hardLinkManager,
                           BlobUrlProperties blobUrlProperties) {
        this.blobUrlService = blobUrlService;
        this.blobUrlRepository = blobUrlRepository;
        this.hardLinkManager = hardLinkManager;
        this.blobUrlProperties = blobUrlProperties;
    }

    /**
     * Scheduled cleanup task that runs at configured intervals.
     * Removes expired blob URLs and their associated hard links.
     */
    @Scheduled(fixedDelayString = "#{@blobUrlProperties.cleanupInterval.toMillis()}")
    @Transactional
    public void scheduledCleanup() {
        if (!blobUrlProperties.isEnableAutomaticCleanup()) {
            log.debug("Automatic cleanup is disabled, skipping scheduled cleanup");
            return;
        }

        log.debug("Starting scheduled cleanup of expired blob URLs");
        
        try {
            int cleanedCount = blobUrlService.cleanupExpiredUrls();
            if (cleanedCount > 0) {
                log.info("Scheduled cleanup completed: {} expired blob URLs removed", cleanedCount);
            } else {
                log.debug("Scheduled cleanup completed: no expired blob URLs found");
            }
        } catch (Exception e) {
            log.error("Error during scheduled cleanup", e);
        }
    }

    /**
     * Cleanup orphaned hard links on application startup.
     * This handles files that may have been left behind from previous application runs.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupOnStartup() {
        if (!blobUrlProperties.isCleanupOnStartup()) {
            log.debug("Startup cleanup is disabled, skipping orphan removal");
            return;
        }

        log.info("Starting cleanup of orphaned hard links on application startup");
        
        try {
            int orphanedCount = removeOrphanedHardLinks();
            if (orphanedCount > 0) {
                log.info("Startup cleanup completed: {} orphaned hard links removed", orphanedCount);
            } else {
                log.debug("Startup cleanup completed: no orphaned hard links found");
            }
        } catch (Exception e) {
            log.error("Error during startup cleanup", e);
        }
    }

    /**
     * Forces immediate cleanup of all expired URLs.
     * This method can be called manually for administrative purposes.
     *
     * @return Number of cleaned up URLs
     */
    @Transactional
    public int forceCleanup() {
        log.info("Starting forced cleanup of expired blob URLs");
        
        try {
            int cleanedCount = blobUrlService.cleanupExpiredUrls();
            int orphanedCount = removeOrphanedHardLinks();
            
            int totalCleaned = cleanedCount + orphanedCount;
            log.info("Forced cleanup completed: {} expired URLs and {} orphaned files removed", 
                    cleanedCount, orphanedCount);
            
            return totalCleaned;
        } catch (Exception e) {
            log.error("Error during forced cleanup", e);
            throw e;
        }
    }

    /**
     * Forces cleanup of a specific blob URL by token.
     * This method can be used for administrative cleanup of specific URLs.
     *
     * @param token The blob URL token to cleanup
     * @return true if the URL was found and cleaned up, false otherwise
     */
    @Transactional
    public boolean forceCleanupByToken(String token) {
        log.info("Starting forced cleanup of blob URL: {}", token);
        
        try {
            return blobUrlRepository.findById(token)
                    .map(blobUrl -> {
                        try {
                            // Delete the hard link first
                            Path hardLinkPath = Paths.get(blobUrl.getHardLinkPath());
                            if (Files.exists(hardLinkPath)) {
                                hardLinkManager.deleteHardLink(hardLinkPath);
                                log.debug("Deleted hard link: {}", hardLinkPath);
                            }

                            // Remove from database
                            blobUrlRepository.delete(blobUrl);
                            log.info("Successfully cleaned up blob URL: {}", token);
                            return true;
                            
                        } catch (Exception e) {
                            log.error("Failed to cleanup blob URL: {}", token, e);
                            return false;
                        }
                    })
                    .orElse(false);
                    
        } catch (Exception e) {
            log.error("Error during forced cleanup of token: {}", token, e);
            return false;
        }
    }

    /**
     * Removes orphaned hard links that exist in the filesystem but have no corresponding database entry.
     * This can happen if the application was terminated unexpectedly or database cleanup failed.
     *
     * @return Number of orphaned files removed
     */
    private int removeOrphanedHardLinks() {
        Path tempDir = Paths.get(blobUrlProperties.getTempDirectory());
        
        if (!Files.exists(tempDir)) {
            log.debug("Temporary directory does not exist: {}", tempDir);
            return 0;
        }

        if (!Files.isDirectory(tempDir)) {
            log.warn("Temporary path is not a directory: {}", tempDir);
            return 0;
        }

        try {
            // Get all valid tokens from database
            List<BlobUrl> allBlobUrls = blobUrlRepository.findAll();
            Set<String> validTokens = new HashSet<>();
            for (BlobUrl blobUrl : allBlobUrls) {
                validTokens.add(blobUrl.getToken());
            }

            int orphanedCount = 0;
            
            // Scan temporary directory for files
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
                for (Path file : stream) {
                    if (Files.isRegularFile(file)) {
                        String filename = file.getFileName().toString();
                        
                        // Check if this file corresponds to a valid token
                        if (!validTokens.contains(filename)) {
                            try {
                                hardLinkManager.deleteHardLink(file);
                                orphanedCount++;
                                log.debug("Removed orphaned hard link: {}", file);
                            } catch (Exception e) {
                                log.warn("Failed to remove orphaned hard link: {}", file, e);
                            }
                        }
                    }
                }
            }
            
            return orphanedCount;
            
        } catch (IOException e) {
            log.error("Error scanning temporary directory for orphaned files: {}", tempDir, e);
            return 0;
        }
    }

    /**
     * Gets cleanup statistics for monitoring purposes.
     *
     * @return CleanupStats with current system state
     */
    public CleanupStats getCleanupStats() {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            long activeUrls = blobUrlRepository.countActiveUrls(now);
            long expiredUrls = blobUrlRepository.findExpiredUrls(now).size();
            
            Path tempDir = Paths.get(blobUrlProperties.getTempDirectory());
            long filesInTempDir = 0;
            
            if (Files.exists(tempDir) && Files.isDirectory(tempDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
                    for (Path ignored : stream) {
                        filesInTempDir++;
                    }
                } catch (IOException e) {
                    log.warn("Failed to count files in temporary directory", e);
                }
            }
            
            return CleanupStats.builder()
                    .activeUrls(activeUrls)
                    .expiredUrls(expiredUrls)
                    .filesInTempDirectory(filesInTempDir)
                    .tempDirectoryPath(tempDir.toString())
                    .cleanupEnabled(blobUrlProperties.isEnableAutomaticCleanup())
                    .cleanupInterval(blobUrlProperties.getCleanupInterval())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error gathering cleanup statistics", e);
            return CleanupStats.builder()
                    .activeUrls(-1)
                    .expiredUrls(-1)
                    .filesInTempDirectory(-1)
                    .tempDirectoryPath(blobUrlProperties.getTempDirectory())
                    .cleanupEnabled(blobUrlProperties.isEnableAutomaticCleanup())
                    .cleanupInterval(blobUrlProperties.getCleanupInterval())
                    .build();
        }
    }

    /**
     * Statistics about the cleanup system state.
     */
    @lombok.Builder
    @lombok.Data
    public static class CleanupStats {
        private final long activeUrls;
        private final long expiredUrls;
        private final long filesInTempDirectory;
        private final String tempDirectoryPath;
        private final boolean cleanupEnabled;
        private final java.time.Duration cleanupInterval;
    }
}