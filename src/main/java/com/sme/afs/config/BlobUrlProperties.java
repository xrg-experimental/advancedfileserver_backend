package com.sme.afs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for blob URL functionality.
 * Configures temporary directory, expiration times, and cleanup behavior.
 */
@Component
@ConfigurationProperties(prefix = "afs.blob-urls")
@Data
public class BlobUrlProperties {
    
    /**
     * Directory where temporary hard links will be created.
     * Must be on the same filesystem as the files being linked.
     * Default: System temp directory + "afs-downloads"
     */
    private String tempDirectory = System.getProperty("java.io.tmpdir") + "/afs-downloads";
    
    /**
     * Default expiration time for blob URLs.
     * Default: 1 hour
     */
    private Duration defaultExpiration = Duration.ofHours(1);
    
    /**
     * Interval between automatic cleanup runs.
     * Default: 15 minutes
     */
    private Duration cleanupInterval = Duration.ofMinutes(15);
    
    /**
     * Whether to validate filesystem support for hard links on startup.
     * Default: true
     */
    private boolean validateFilesystemOnStartup = true;
    
    /**
     * Maximum number of concurrent blob URLs allowed.
     * Helps prevent filesystem exhaustion.
     * Default: 1000
     */
    private long maxConcurrentUrls = 1000;
    
    /**
     * Whether to enable automatic cleanup of expired URLs.
     * Default: true
     */
    private boolean enableAutomaticCleanup = true;
    
    /**
     * Whether to perform cleanup on application startup.
     * Removes any orphaned hard links from previous sessions.
     * Default: true
     */
    private boolean cleanupOnStartup = true;
    
    /**
     * Base URL path for blob URL downloads.
     * Default: "/downloads"
     */
    private String downloadUrlPath = "/downloads";
    
    /**
     * Token length in characters (before base64 encoding).
     * Longer tokens provide better security but use more storage.
     * Default: 32 (results in ~43 character base64 token)
     */
    private int tokenLength = 32;
}