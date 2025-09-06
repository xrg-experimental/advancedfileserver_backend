package com.sme.afs.service.filesystem;

import com.sme.afs.model.filesystem.DirectoryChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class DefaultDirectoryMonitorService implements DirectoryMonitorService {
    private final Map<Path, WatchService> monitoredDirectories = new HashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ApplicationEventPublisher eventPublisher;

    public DefaultDirectoryMonitorService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void startMonitoring(Path directory) {
        if (isMonitoring(directory)) {
            log.warn("Directory {} is already being monitored", directory);
            return;
        }

        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            directory.register(watchService, 
                StandardWatchEventKinds.ENTRY_CREATE, 
                StandardWatchEventKinds.ENTRY_DELETE, 
                StandardWatchEventKinds.ENTRY_MODIFY);

            monitoredDirectories.put(directory, watchService);

            executorService.submit(() -> {
                while (monitoredDirectories.containsKey(directory)) {
                    try {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            Path context = (Path) event.context();
                            Path fullPath = directory.resolve(context);

                            DirectoryChangeEvent.EventType eventType;
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                eventType = DirectoryChangeEvent.EventType.CREATED;
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                eventType = DirectoryChangeEvent.EventType.DELETED;
                            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                eventType = DirectoryChangeEvent.EventType.MODIFIED;
                            } else {
                                continue;
                            }

                            DirectoryChangeEvent changeEvent = new DirectoryChangeEvent(
                                fullPath, 
                                eventType, 
                                Instant.now()
                            );

                            eventPublisher.publishEvent(changeEvent);
                        }
                        key.reset();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });

            log.info("Started monitoring directory: {}", directory);
        } catch (IOException e) {
            log.error("Error starting directory monitoring for {}", directory, e);
            throw new RuntimeException("Failed to start directory monitoring", e);
        }
    }

    @Override
    public void stopMonitoring(Path directory) {
        WatchService watchService = monitoredDirectories.remove(directory);
        if (watchService != null) {
            try {
                watchService.close();
                log.info("Stopped monitoring directory: {}", directory);
            } catch (IOException e) {
                log.error("Error stopping directory monitoring for {}", directory, e);
            }
        }
    }

    @Override
    public boolean isMonitoring(Path directory) {
        return monitoredDirectories.containsKey(directory);
    }

    // Cleanup method to stop all monitoring when service is destroyed
    public void destroy() {
        new HashMap<>(monitoredDirectories).keySet().forEach(this::stopMonitoring);
        executorService.shutdown();
    }
}
