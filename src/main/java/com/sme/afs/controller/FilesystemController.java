package com.sme.afs.controller;

import com.sme.afs.dto.FileMetadataResponse;
import com.sme.afs.service.filesystem.FilesystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/filesystem")
@RequiredArgsConstructor
public class FilesystemController {
    private final FilesystemService filesystemService;

    @GetMapping("/list")
    public ResponseEntity<?> listDirectory(@RequestParam String directory) {
        try {
            Path path = Path.of(directory);
            List<FileMetadataResponse> contents = filesystemService.listDirectory(path);
            return ResponseEntity.ok(contents);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error listing directory: " + e.getMessage());
        }
    }

    @GetMapping("/metadata")
    public ResponseEntity<?> getMetadata(@RequestParam String path) {
        try {
            Path filePath = Path.of(path);
            FileMetadataResponse metadata = filesystemService.getMetadata(filePath);
            return ResponseEntity.ok(metadata);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error getting metadata: " + e.getMessage());
        }
    }
}
