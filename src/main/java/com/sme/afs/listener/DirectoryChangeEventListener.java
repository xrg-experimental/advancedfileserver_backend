package com.sme.afs.listener;

import com.sme.afs.model.filesystem.DirectoryChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DirectoryChangeEventListener {
    @EventListener
    public void handleDirectoryChangeEvent(DirectoryChangeEvent event) {
        log.info("Directory Change Event: {} at {} - Type: {}", 
            event.getPath(), 
            event.getTimestamp(), 
            event.getEventType()
        );
    }
}
