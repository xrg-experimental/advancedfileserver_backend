package com.sme.afs.service;

import com.sme.afs.config.BlobUrlProperties;
import com.sme.afs.repository.BlobUrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.Duration;

/**
 * Health monitoring service for blob URL operations.
 * Provides system health checks and operational metrics.
 */
@Service
@Slf4j
public class BlobUrlHealthService {

    private final BlobUrlRepository blobUrlRepository;
    private final BlobUrlProperties blobUrlProperties;
    private final FilesystemValidationService filesystemValidationService;
    private final CleanupScheduler cleanupScheduler;

    @Autowired
    public BlobUrlHealthService(BlobUrlRepository blobUrlRepository,
                               BlobUrlProperties blobUrlProperties,
                               FilesystemValidationService filesystemValidationService,
                               CleanupScheduler cleanupScheduler) {
        this.blobUrlRepository = blobUrlRepository;
        this.blobUrlProperties = blobUrlProperties;
        this.filesystemValidationService = filesystemValidationService;
        this.cleanupScheduler = cleanupScheduler;
    }

    /**
     * Performs a comprehensive health check of the blob URL system.
     *
     * @return HealthStatus with detailed system health information
     */
    public HealthStatus performHealthCheck() {
        log.debug("Performing blob URL system health check");
        
        HealthStatus.HealthStatusBuilder builder = HealthStatus.builder();
        boolean overallHealthy = true;
        
        try {
            // Check database connectivity and basic queries
            DatabaseHealth dbHealth = checkDatabaseHealth();
            builder.databaseHealth(dbHealth);
            if (!dbHealth.isHealthy()) {
                overallHealthy = false;
            }
            
            // Check filesystem health
            FilesystemHealth fsHealth = checkFilesystemHealth();
            builder.filesystemHealth(fsHealth);
            if (!fsHealth.isHealthy()) {
                overallHealthy = false;
            }
            
            // Check cleanup system health
            CleanupHealth cleanupHealth = checkCleanupHealth();
            builder.cleanupHealth(cleanupHealth);
            if (!cleanupHealth.isHealthy()) {
                overallHealthy = false;
            }
            
            // Get operational metrics
            OperationalMetrics metrics = getOperationalMetrics();
            builder.operationalMetrics(metrics);
            
            // Check for warning conditions
            WarningConditions warnings = checkWarningConditions(metrics);
            builder.warningConditions(warnings);
            
            builder.overallHealthy(overallHealthy);
            builder.checkTimestamp(OffsetDateTime.now());
            
            HealthStatus status = builder.build();
            
            if (overallHealthy) {
                log.debug("Blob URL system health check completed: HEALTHY");
            } else {
                log.warn("Blob URL system health check completed: UNHEALTHY - {}", 
                        getUnhealthyReasons(status));
            }
            
            return status;
            
        } catch (Throwable e) {
            log.error("Error during health check", e);
            return HealthStatus.builder()
                    .overallHealthy(false)
                    .checkTimestamp(OffsetDateTime.now())
                    .error("Health check failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Checks database connectivity and basic operations.
     */
    private DatabaseHealth checkDatabaseHealth() {
        try {
            // Test basic database connectivity
            OffsetDateTime now = OffsetDateTime.now();
            long activeCount = blobUrlRepository.countActiveUrls(now);
            
            // Test query performance (should be fast)
            long startTime = System.currentTimeMillis();
            blobUrlRepository.findExpiredUrls(now.minusHours(1));
            long queryTime = System.currentTimeMillis() - startTime;
            
            return DatabaseHealth.builder()
                    .healthy(true)
                    .activeUrlCount(activeCount)
                    .queryResponseTimeMs(queryTime)
                    .message("Database connectivity confirmed")
                    .build();
                    
        } catch (Exception e) {
            log.error("Database health check failed", e);
            // Propagate to be handled at a higher level as an error in the overall health status
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    /**
     * Checks filesystem health and accessibility.
     */
    private FilesystemHealth checkFilesystemHealth() {
        try {
            // Validate filesystem capabilities
            FilesystemValidationService.ValidationResult validation = 
                    filesystemValidationService.validateFilesystem();
            
            // Get filesystem information
            FilesystemValidationService.FilesystemInfo fsInfo = 
                    filesystemValidationService.getFilesystemInfo();
            
            // Check disk space
            boolean hasAdequateSpace = true;
            String spaceWarning = null;
            
            if (fsInfo.getUsableSpace() != null) {
                long usableSpaceGB = fsInfo.getUsableSpace() / (1024 * 1024 * 1024);
                if (usableSpaceGB < 1) {
                    hasAdequateSpace = false;
                    spaceWarning = "Low disk space: " + usableSpaceGB + "GB available";
                } else if (usableSpaceGB < 5) {
                    spaceWarning = "Disk space warning: " + usableSpaceGB + "GB available";
                }
            }
            
            boolean healthy = validation.isValid() && hasAdequateSpace;
            String message = validation.getMessage();
            if (spaceWarning != null) {
                message += "; " + spaceWarning;
            }
            
            return FilesystemHealth.builder()
                    .healthy(healthy)
                    .tempDirectoryExists(fsInfo.isExists())
                    .hardLinkSupported(validation.isValid())
                    .usableSpaceGB(fsInfo.getUsableSpace() != null ? 
                            fsInfo.getUsableSpace() / (1024 * 1024 * 1024) : null)
                    .fileSystemType(fsInfo.getFileSystemType())
                    .message(message)
                    .build();
                    
        } catch (Exception e) {
            log.error("Filesystem health check failed", e);
            return FilesystemHealth.builder()
                    .healthy(false)
                    .message("Filesystem health check failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Checks cleanup system health.
     */
    private CleanupHealth checkCleanupHealth() {
        try {
            CleanupScheduler.CleanupStats stats = cleanupScheduler.getCleanupStats();
            
            // Check for potential issues
            boolean healthy = true;
            StringBuilder issues = new StringBuilder();
            
            if (!stats.isCleanupEnabled()) {
                healthy = false;
                issues.append("Automatic cleanup is disabled; ");
            }
            
            if (stats.getExpiredUrls() > 100) {
                healthy = false;
                issues.append("High number of expired URLs (").append(stats.getExpiredUrls()).append("); ");
            }
            
            // Check for orphaned files using raw filesystem count
            long totalFiles = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(blobUrlProperties.getTempDirectory()))) {
                for (Path p : stream) {
                    if (Files.isRegularFile(p)) totalFiles++;
                }
            } catch (IOException e) {
                log.warn("Failed to count files for orphan detection", e);
            }
            long orphanedFiles = Math.max(0, totalFiles - stats.getActiveUrls());
            if (orphanedFiles > 10) {
                healthy = false;
                issues.append("Potential orphaned files (").append(orphanedFiles).append("); ");
            }
            
            String message = healthy ? "Cleanup system operating normally" : issues.toString();
            
            return CleanupHealth.builder()
                    .healthy(healthy)
                    .cleanupEnabled(stats.isCleanupEnabled())
                    .expiredUrlCount(stats.getExpiredUrls())
                    .filesInTempDirectory(stats.getFilesInTempDirectory())
                    .cleanupInterval(stats.getCleanupInterval())
                    .message(message)
                    .build();
                    
        } catch (Exception e) {
            log.error("Cleanup health check failed", e);
            return CleanupHealth.builder()
                    .healthy(false)
                    .message("Cleanup health check failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Gets operational metrics for the blob URL system.
     */
    private OperationalMetrics getOperationalMetrics() {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            
            long activeUrls = blobUrlRepository.countActiveUrls(now);
            long expiredUrls = blobUrlRepository.findExpiredUrls(now).size();
            
            // Count files in temp directory
            long filesInTempDir = 0;
            Path tempDir = Paths.get(blobUrlProperties.getTempDirectory());
            if (Files.exists(tempDir) && Files.isDirectory(tempDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
                    for (Path ignored : stream) {
                        filesInTempDir++;
                    }
                } catch (IOException e) {
                    log.warn("Failed to count files in temp directory", e);
                }
            }
            
            // Calculate utilization percentage
            double utilizationPercent = blobUrlProperties.getMaxConcurrentUrls() > 0 ?
                    (double) activeUrls / blobUrlProperties.getMaxConcurrentUrls() * 100 : 0;
            
            return OperationalMetrics.builder()
                    .activeUrls(activeUrls)
                    .expiredUrls(expiredUrls)
                    .filesInTempDirectory(filesInTempDir)
                    .maxConcurrentUrls(blobUrlProperties.getMaxConcurrentUrls())
                    .utilizationPercent(utilizationPercent)
                    .defaultExpirationHours(blobUrlProperties.getDefaultExpiration().toHours())
                    .cleanupIntervalMinutes(blobUrlProperties.getCleanupInterval().toMinutes())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to gather operational metrics", e);
            return OperationalMetrics.builder()
                    .activeUrls(-1)
                    .expiredUrls(-1)
                    .filesInTempDirectory(-1)
                    .maxConcurrentUrls(blobUrlProperties.getMaxConcurrentUrls())
                    .utilizationPercent(-1)
                    .build();
        }
    }

    /**
     * Checks for warning conditions that don't necessarily indicate system failure.
     */
    private WarningConditions checkWarningConditions(OperationalMetrics metrics) {
        boolean hasWarnings = false;
        StringBuilder warnings = new StringBuilder();
        
        // High-utilization warning
        if (metrics.getUtilizationPercent() > 80) {
            hasWarnings = true;
            warnings.append("High utilization (").append(String.format("%.1f", metrics.getUtilizationPercent())).append("%); ");
        }
        
        // Expired URLs accumulation warning
        if (metrics.getExpiredUrls() > 50) {
            hasWarnings = true;
            warnings.append("Expired URLs accumulating (").append(metrics.getExpiredUrls()).append("); ");
        }
        
        // File count mismatch warning
        long expectedFiles = metrics.getActiveUrls();
        long actualFiles = metrics.getFilesInTempDirectory();
        if (Math.abs(actualFiles - expectedFiles) >= 5) {
            hasWarnings = true;
            warnings.append("File count mismatch (expected: ").append(expectedFiles)
                    .append(", actual: ").append(actualFiles).append("); ");
        }
        
        return WarningConditions.builder()
                .hasWarnings(hasWarnings)
                .warnings(hasWarnings ? warnings.toString() : "No warnings")
                .build();
    }

    /**
     * Gets a summary of unhealthy reasons for logging.
     */
    private String getUnhealthyReasons(HealthStatus status) {
        StringBuilder reasons = new StringBuilder();
        
        if (status.getDatabaseHealth() != null && !status.getDatabaseHealth().isHealthy()) {
            reasons.append("Database: ").append(status.getDatabaseHealth().getMessage()).append("; ");
        }
        
        if (status.getFilesystemHealth() != null && !status.getFilesystemHealth().isHealthy()) {
            reasons.append("Filesystem: ").append(status.getFilesystemHealth().getMessage()).append("; ");
        }
        
        if (status.getCleanupHealth() != null && !status.getCleanupHealth().isHealthy()) {
            reasons.append("Cleanup: ").append(status.getCleanupHealth().getMessage()).append("; ");
        }
        
        return reasons.toString();
    }

    // Health status data classes
    @lombok.Builder
    @lombok.Data
    public static class HealthStatus {
        private final boolean overallHealthy;
        private final OffsetDateTime checkTimestamp;
        private final DatabaseHealth databaseHealth;
        private final FilesystemHealth filesystemHealth;
        private final CleanupHealth cleanupHealth;
        private final OperationalMetrics operationalMetrics;
        private final WarningConditions warningConditions;
        private final String error;
    }

    @lombok.Builder
    @lombok.Data
    public static class DatabaseHealth {
        private final boolean healthy;
        private final Long activeUrlCount;
        private final Long queryResponseTimeMs;
        private final String message;
    }

    @lombok.Builder
    @lombok.Data
    public static class FilesystemHealth {
        private final boolean healthy;
        private final Boolean tempDirectoryExists;
        private final Boolean hardLinkSupported;
        private final Long usableSpaceGB;
        private final String fileSystemType;
        private final String message;
    }

    @lombok.Builder
    @lombok.Data
    public static class CleanupHealth {
        private final boolean healthy;
        private final Boolean cleanupEnabled;
        private final Long expiredUrlCount;
        private final Long filesInTempDirectory;
        private final Duration cleanupInterval;
        private final String message;
    }

    @lombok.Builder
    @lombok.Data
    public static class OperationalMetrics {
        private final long activeUrls;
        private final long expiredUrls;
        private final long filesInTempDirectory;
        private final long maxConcurrentUrls;
        private final double utilizationPercent;
        private final Long defaultExpirationHours;
        private final Long cleanupIntervalMinutes;
    }

    @lombok.Builder
    @lombok.Data
    public static class WarningConditions {
        private final boolean hasWarnings;
        private final String warnings;
    }
}