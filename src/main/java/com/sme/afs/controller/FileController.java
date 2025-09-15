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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

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
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Directory not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL', 'EXTERNAL')")
    public ResponseEntity<FileListResponse> listDirectory(
            @Valid @RequestBody PathRequest request) {
        return ResponseEntity.ok(fileService.listDirectory(request.getPath()));
    }

    @PostMapping("/info")
    @Operation(summary = "Get file/directory info", 
               description = "Retrieves detailed information about a file or directory")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved file info"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL', 'EXTERNAL')")
    public ResponseEntity<FileInfoResponse> getFileInfo(
            @Valid @RequestBody PathRequest request) {
        return ResponseEntity.ok(fileService.getFileInfo(request.getPath()));
    }

    @PostMapping("/create")
    @Operation(summary = "Create directory", 
               description = "Creates a new directory at the specified path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Directory created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Directory already exists"),
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL', 'EXTERNAL')")
    public ResponseEntity<FileInfoResponse> createDirectory(
            @Valid @RequestBody PathRequest request) {
        return ResponseEntity.status(201).body(fileService.createDirectory(request.getPath()));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete file/directory", 
               description = "Deletes a file or directory at the specified path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL', 'EXTERNAL')")
    public ResponseEntity<Void> delete(
            @Valid @RequestBody PathRequest request) {
        fileService.delete(request.getPath());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rename")
    @Operation(summary = "Rename file/directory", 
               description = "Renames a file or directory to a new name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully renamed"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Target name already exists")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL', 'EXTERNAL')")
    public ResponseEntity<FileInfoResponse> rename(
            @Valid @RequestBody RenameRequest request) {
        return ResponseEntity.ok(fileService.rename(request.getPath(), request.getNewName()));
    }

    @PostMapping("/move")
    @Operation(summary = "Move file/directory", 
               description = "Moves a file or directory from source to target path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully moved"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Source not found"),
        @ApiResponse(responseCode = "409", description = "Target already exists")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL', 'EXTERNAL')")
    public ResponseEntity<FileInfoResponse> move(
            @Valid @RequestBody MoveRequest request) {
        return ResponseEntity.ok(fileService.move(request.getSourcePath(), request.getTargetPath()));
    }

    @GetMapping("/download/**")
    @Operation(summary = "Download file", 
               description = "Downloads a file directly through the API (deprecated - use blob URLs for better performance)",
               deprecated = true)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL', 'EXTERNAL')")
    public ResponseEntity<Resource> download(HttpServletRequest request) {
        String path = extractPathFromRequest(request);
        Resource resource = fileService.loadAsResource(path);
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + resource.getFilename() + "\"")
            .body(resource);
    }

    /* TODO: Map upload-size errors to 413 (Payload Too Large) — add handler in GlobalExceptionHandler.java
     *
     * application.yml already sets spring.servlet.multipart.max-file-size and max-request-size = 100MB,
     * but src/main/java/com/sme/afs/exception/GlobalExceptionHandler.java has no handler for
     * MaxUploadSizeExceededException/MultipartException and will fall back to the generic 500 handler.
     * Add an @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
     * (and optionally MultipartException) in GlobalExceptionHandler to return HTTP 413 with the
     * ProblemResponse body (reuse or add an appropriate ErrorCode for "file too large"); ensure the
     * handler logs safely and does not leak file contents or sensitive info.
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file", 
               description = "Uploads a file to the specified path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "File already exists"),
        @ApiResponse(responseCode = "413", description = "File too large")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL', 'EXTERNAL')")
    public ResponseEntity<FileInfoResponse> upload(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") @NotNull MultipartFile file,
            @Parameter(description = "Target path for the file", required = true)
            @RequestParam("path") @NotBlank @Size(max = 4096) String path) {
        return ResponseEntity.ok(fileService.store(file, path));
    }

    private String extractPathFromRequest(HttpServletRequest request) {
        final String pattern =
                (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);    // "/files/download/**"
        final String withinMapping =
                (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        final String extracted = new AntPathMatcher().extractPathWithinPattern(pattern, withinMapping);
        // Decode percent-encoded segments
        return UriUtils.decode(extracted, StandardCharsets.UTF_8);
    }
}
