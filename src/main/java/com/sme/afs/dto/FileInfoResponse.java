package com.sme.afs.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FileInfoResponse {
    private String name;
    private String path;
    private String type;
    private long size;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private String owner;
    private String group;
    private String permissions;
    private boolean isDirectory;
    private String mimeType;
}
