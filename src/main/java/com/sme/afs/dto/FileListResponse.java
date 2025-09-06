package com.sme.afs.dto;

import lombok.Data;
import java.util.List;

@Data
public class FileListResponse {
    private String path;
    private List<FileInfoResponse> entries;
    private long totalSize;
    private int totalFiles;
    private int totalDirectories;
}
