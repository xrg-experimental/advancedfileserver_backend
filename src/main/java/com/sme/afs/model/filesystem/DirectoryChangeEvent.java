package com.sme.afs.model.filesystem;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class DirectoryChangeEvent {
    public enum EventType {
        CREATED,
        MODIFIED,
        DELETED
    }

    private final Path path;
    private final EventType eventType;
    private final Instant timestamp;
}
