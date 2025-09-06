package com.sme.afs.service.filesystem;

import java.nio.file.Path;

public interface DirectoryMonitorService {
    /**
     * Start monitoring a specific directory for changes
     * @param directory Path to the directory to monitor
     */
    void startMonitoring(Path directory);

    /**
     * Stop monitoring a specific directory
     * @param directory Path to the directory to stop monitoring
     */
    void stopMonitoring(Path directory);

    /**
     * Check if a directory is currently being monitored
     * @param directory Path to the directory
     * @return true if the directory is being monitored, false otherwise
     */
    boolean isMonitoring(Path directory);
}
