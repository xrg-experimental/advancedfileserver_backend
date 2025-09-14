package com.sme.afs.controller;

import com.sme.afs.dto.FileListResponse;
import com.sme.afs.dto.FileInfoResponse;
import com.sme.afs.dto.RenameRequest;
import com.sme.afs.dto.PathRequest;
import com.sme.afs.dto.MoveRequest;
import com.sme.afs.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File and directory management operations")
@SecurityRequirement(name = "bearerAuth")
public class FileController {
    private final FileService fileService;

    @PostMapping("/list")
    @Operation(summary = "List directory contents", 
               description = "Retrieves the contents of a directory including files and subdirectories")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved directory listing"),
        @ApiResponse(responseCode = "404", description = "Directory not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileListResponse> listDirectory(
            @Valid @RequestBody PathRequest request) {
        return ResponseEntity.ok(fileService.listDirectory(request.getPath()));
    }

    @PostMapping("/info")
    @Operation(summary = "Get file/directory info", 
               description = "Retrieves detailed information about a file or directory")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved file info"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileInfoResponse> getFileInfo(
            @Valid @RequestBody PathRequest request) {
        return ResponseEntity.ok(fileService.getFileInfo(request.getPath()));
    }

    @PostMapping("/create")
    @Operation(summary = "Create directory", 
               description = "Creates a new directory at the specified path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Directory created successfully"),
        @ApiResponse(responseCode = "409", description = "Directory already exists"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileInfoResponse> createDirectory(
            @Valid @RequestBody PathRequest request) {
        return ResponseEntity.ok(fileService.createDirectory(request.getPath()));
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete file/directory", 
               description = "Deletes a file or directory at the specified path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> delete(
            @Valid @RequestBody PathRequest request) {
        fileService.delete(request.getPath());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rename")
    @Operation(summary = "Rename file/directory", 
               description = "Renames a file or directory to a new name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully renamed"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "409", description = "Target name already exists"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileInfoResponse> rename(
            @Valid @RequestBody RenameRequest request) {
        return ResponseEntity.ok(fileService.rename(request.getPath(), request.getNewName()));
    }

    @PostMapping("/move")
    @Operation(summary = "Move file/directory", 
               description = "Moves a file or directory from source to target path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully moved"),
        @ApiResponse(responseCode = "404", description = "Source not found"),
        @ApiResponse(responseCode = "409", description = "Target already exists"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileInfoResponse> move(
            @Valid @RequestBody MoveRequest request) {
        return ResponseEntity.ok(fileService.move(request.getSourcePath(), request.getTargetPath()));
    }

    @GetMapping("/download/**")
    @Operation(summary = "Download file", 
               description = "Downloads a file directly through the API (deprecated - use blob URLs for better performance)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
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
    @Operation(summary = "Upload file", 
               description = "Uploads a file to the specified path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
        @ApiResponse(responseCode = "409", description = "File already exists"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "413", description = "File too large")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileInfoResponse> upload(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Target path for the file", required = true)
            @RequestParam("path") String path) {
        return ResponseEntity.ok(fileService.store(file, path));
    }

    private String extractPathFromRequest(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String downloadPrefix = "/api/files/download/";
        return requestURI.substring(requestURI.indexOf(downloadPrefix) + downloadPrefix.length());
    }
}
