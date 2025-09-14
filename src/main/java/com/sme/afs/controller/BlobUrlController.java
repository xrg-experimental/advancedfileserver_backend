package com.sme.afs.controller;

import com.sme.afs.dto.BlobUrlCreateRequest;
import com.sme.afs.dto.BlobUrlResponse;
import com.sme.afs.service.BlobUrlService;
import com.sme.afs.service.BlobUrlHealthService;
import com.sme.afs.service.CleanupScheduler;
import com.sme.afs.service.FilesystemValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/blob-urls")
@RequiredArgsConstructor
@Tag(name = "Blob URLs", description = "Temporary download URL management")
@SecurityRequirement(name = "bearerAuth")
public class BlobUrlController {
    
    private final BlobUrlService blobUrlService;
    private final BlobUrlHealthService blobUrlHealthService;
    private final CleanupScheduler cleanupScheduler;
    private final FilesystemValidationService filesystemValidationService;

    @PostMapping("/create")
    @Operation(summary = "Create temporary download URL", 
               description = "Creates a temporary download URL for a file using hard links")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Blob URL created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or cross-filesystem error"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "500", description = "Hard link creation failed or filesystem unsupported")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BlobUrlResponse> createBlobUrl(
            @Valid @RequestBody BlobUrlCreateRequest request) {
        String safePath = request.getFilePath() == null ? "" : request.getFilePath().replaceAll("[\\r\\n]", "");
        log.debug("Creating blob URL for file: {}", safePath);
        BlobUrlResponse response = blobUrlService.createBlobUrl(request.getFilePath());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{token}/status")
    @Operation(summary = "Get blob URL status", 
               description = "Returns the current status and metadata of a blob URL")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Token not found or expired")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BlobUrlResponse> getBlobUrlStatus(
            @Parameter(description = "Blob URL token", required = true)
            @PathVariable String token) {
        log.debug("Getting status for blob URL token: {}", token);
        BlobUrlResponse response = blobUrlService.getBlobUrlStatus(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/downloads/{token}")
    @Operation(summary = "Download file via blob URL", 
               description = "Downloads the file using the temporary blob URL. Supports range requests for partial downloads.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File download started"),
        @ApiResponse(responseCode = "206", description = "Partial content (range request)"),
        @ApiResponse(responseCode = "404", description = "Token not found, expired, or file no longer exists"),
        @ApiResponse(responseCode = "416", description = "Range not satisfiable")
    })
    public ResponseEntity<?> downloadFile(
            @Parameter(description = "Blob URL token", required = true)
            @PathVariable String token,
            HttpServletRequest request,
            HttpServletResponse ignore) {
        
        log.debug("Download requested for blob URL token: {}", token);
        
        // Get the file resource and metadata
        Resource resource = blobUrlService.validateAndGetFile(token);
        BlobUrlResponse blobUrlInfo = blobUrlService.getBlobUrlStatus(token);
        
        // Handle range requests for partial downloads
        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            return handleRangeRequest(resource, blobUrlInfo, rangeHeader);
        }
        
        // Standard full file download
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(blobUrlInfo.getContentType()))
                .contentLength(blobUrlInfo.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + blobUrlInfo.getFilename() + "\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(resource);
    }

    private ResponseEntity<?> handleRangeRequest(
            Resource resource,
            BlobUrlResponse blobUrlInfo,
            String rangeHeader) {

        long fileSize = blobUrlInfo.getFileSize();
        try {
            java.util.List<org.springframework.http.HttpRange> ranges = org.springframework.http.HttpRange.parseRanges(rangeHeader);
            if (ranges.size() != 1) {
                return ResponseEntity.status(416)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                        .build();
            }
            org.springframework.http.HttpRange r = ranges.get(0);
            long start = r.getRangeStart(fileSize);
            long end = r.getRangeEnd(fileSize);
            if (start < 0 || end < 0 || start > end || end >= fileSize) {
                return ResponseEntity.status(416)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                        .build();
            }
            long count = end - start + 1;
            String contentRange = "bytes " + start + "-" + end + "/" + fileSize;

            // Build headers required for a 206 Partial Content response
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_RANGE, contentRange);
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    org.springframework.http.ContentDisposition.attachment()
                            .filename(blobUrlInfo.getFilename(), java.nio.charset.StandardCharsets.UTF_8)
                            .build()
                            .toString());

            // Determine content type; fall back to octet-stream
            MediaType ct = org.springframework.http.MediaTypeFactory
                    .getMediaType(resource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            // Stream only the requested byte range to the client to avoid relying on ResourceRegion converters
            java.io.InputStream is = resource.getInputStream();
            try {
                // Ensure we position the stream at the requested start
                is.skipNBytes(start);
            } catch (java.io.EOFException eof) {
                // Defensive: if underlying stream shorter than expected, return 416
                return ResponseEntity.status(416)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                        .build();
            }

            // Limit the stream to the requested number of bytes
            org.apache.commons.io.input.BoundedInputStream bounded = new org.apache.commons.io.input.BoundedInputStream(is, count);
            // Ensure closing this stream does not close the underlying stream prematurely (handled by container)
            bounded.setPropagateClose(true);

            org.springframework.core.io.InputStreamResource partialResource = new org.springframework.core.io.InputStreamResource(bounded) {
                @Override
                public String getFilename() {
                    return blobUrlInfo.getFilename();
                }
            };

            return ResponseEntity.status(206)
                    .contentType(ct)
                    .headers(headers)
                    .contentLength(count)
                    .body(partialResource);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid range header: {}", rangeHeader);
            return ResponseEntity.status(416)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
        } catch (java.io.IOException ioEx) {
            log.error("I/O error processing range request: {}", ioEx.getMessage(), ioEx);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Get blob URL system health status", 
               description = "Returns comprehensive health information about the blob URL system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Health status retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Health check failed")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BlobUrlHealthService.HealthStatus> getHealthStatus() {
        log.debug("Health status requested for blob URL system");
        BlobUrlHealthService.HealthStatus health = blobUrlHealthService.performHealthCheck();
        return ResponseEntity.ok(health);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get cleanup statistics", 
               description = "Returns statistics about the cleanup system and current state")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CleanupScheduler.CleanupStats> getCleanupStats() {
        log.debug("Cleanup statistics requested");
        CleanupScheduler.CleanupStats stats = cleanupScheduler.getCleanupStats();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/admin/cleanup")
    @Operation(summary = "Force immediate cleanup", 
               description = "Triggers immediate cleanup of expired URLs and orphaned files")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cleanup completed successfully"),
        @ApiResponse(responseCode = "500", description = "Cleanup failed")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> forceCleanup() {
        log.info("Manual cleanup requested by admin");
        int cleanedCount = cleanupScheduler.forceCleanup();
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("cleanedCount", cleanedCount);
        result.put("timestamp", java.time.OffsetDateTime.now());
        result.put("message", "Cleanup completed successfully");
        
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/admin/{token}")
    @Operation(summary = "Force cleanup of specific blob URL", 
               description = "Manually removes a specific blob URL and its hard link")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Blob URL cleaned up successfully"),
        @ApiResponse(responseCode = "404", description = "Blob URL not found"),
        @ApiResponse(responseCode = "500", description = "Cleanup failed")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> forceCleanupByToken(
            @Parameter(description = "Blob URL token to cleanup", required = true)
            @PathVariable String token) {
        log.info("Manual cleanup requested for token: {}", token);
        boolean success = cleanupScheduler.forceCleanupByToken(token);
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", success);
        result.put("token", token);
        result.put("timestamp", java.time.OffsetDateTime.now());
        result.put("message", success ? "Blob URL cleaned up successfully" : "Blob URL not found or cleanup failed");
        
        return success ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

    @GetMapping("/admin/filesystem-info")
    @Operation(summary = "Get filesystem information", 
               description = "Returns information about the filesystem where blob URLs are stored")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Filesystem information retrieved successfully")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FilesystemValidationService.FilesystemInfo> getFilesystemInfo() {
        log.debug("Filesystem information requested");
        FilesystemValidationService.FilesystemInfo info = filesystemValidationService.getFilesystemInfo();
        return ResponseEntity.ok(info);
    }

    @PostMapping("/admin/validate-filesystem")
    @Operation(summary = "Validate filesystem capabilities", 
               description = "Performs validation of filesystem support for blob URL operations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Filesystem validation completed"),
        @ApiResponse(responseCode = "500", description = "Filesystem validation failed")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FilesystemValidationService.ValidationResult> validateFilesystem() {
        log.info("Manual filesystem validation requested");
        FilesystemValidationService.ValidationResult result = filesystemValidationService.validateFilesystem();
        return ResponseEntity.ok(result);
    }
}