package com.sme.afs.controller;

import com.sme.afs.dto.FileListResponse;
import com.sme.afs.dto.FileInfoResponse;
import com.sme.afs.dto.RenameRequest;
import com.sme.afs.dto.PathRequest;
import com.sme.afs.dto.MoveRequest;
import com.sme.afs.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping("/list")
    @Operation(summary = "List directory contents")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved directory listing"),
        @ApiResponse(responseCode = "404", description = "Directory not found")
    })
    public ResponseEntity<FileListResponse> listDirectory(
            @RequestBody PathRequest request) {
        return ResponseEntity.ok(fileService.listDirectory(request.getPath()));
    }

    @PostMapping("/info")
    @Operation(summary = "Get file/directory info")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved file info"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<FileInfoResponse> getFileInfo(
            @RequestBody PathRequest request) {
        return ResponseEntity.ok(fileService.getFileInfo(request.getPath()));
    }

    @PostMapping("/create")
    @Operation(summary = "Create directory")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Directory created successfully"),
        @ApiResponse(responseCode = "409", description = "Directory already exists")
    })
    public ResponseEntity<FileInfoResponse> createDirectory(
            @RequestBody PathRequest request) {
        return ResponseEntity.ok(fileService.createDirectory(request.getPath()));
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete file/directory")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<Void> delete(
            @RequestBody PathRequest request) {
        fileService.delete(request.getPath());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rename")
    @Operation(summary = "Rename file/directory")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully renamed"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "409", description = "Target name already exists")
    })
    public ResponseEntity<FileInfoResponse> rename(
            @RequestBody RenameRequest request) {
        return ResponseEntity.ok(fileService.rename(request.getPath(), request.getNewName()));
    }

    @PostMapping("/move")
    @Operation(summary = "Move file/directory")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully moved"),
        @ApiResponse(responseCode = "404", description = "Source not found"),
        @ApiResponse(responseCode = "409", description = "Target already exists")
    })
    public ResponseEntity<FileInfoResponse> move(
            @RequestBody MoveRequest request) {
        return ResponseEntity.ok(fileService.move(request.getSourcePath(), request.getTargetPath()));
    }

    @GetMapping("/download/**")
    @Operation(summary = "Download file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<Resource> download(HttpServletRequest request) {
        String path = extractPathFromRequest(request);
        Resource resource = fileService.loadAsResource(path);
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + resource.getFilename() + "\"")
            .body(resource);
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
        @ApiResponse(responseCode = "409", description = "File already exists")
    })
    public ResponseEntity<FileInfoResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("path") String path) {
        return ResponseEntity.ok(fileService.store(file, path));
    }

    private String extractPathFromRequest(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String downloadPrefix = "/api/files/download/";
        return requestURI.substring(requestURI.indexOf(downloadPrefix) + downloadPrefix.length());
    }
}
