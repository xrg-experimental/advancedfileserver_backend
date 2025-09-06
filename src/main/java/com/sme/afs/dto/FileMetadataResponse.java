package com.sme.afs.dto;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.time.Instant;

@Data
@Builder
public class FileMetadataResponse {
    private Path path;
    private String name;
    private boolean isDirectory;
    private long size;
    private Instant createdTime;
    private Instant lastModifiedTime;
    private boolean isReadable;
    private boolean isWritable;
    private boolean isExecutable;
}
