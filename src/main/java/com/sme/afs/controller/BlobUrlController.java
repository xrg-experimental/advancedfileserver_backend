package com.sme.afs.controller;

import com.sme.afs.dto.BlobUrlCreateRequest;
import com.sme.afs.dto.BlobUrlResponse;
import com.sme.afs.service.BlobUrlService;
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

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/blob-urls")
@RequiredArgsConstructor
@Tag(name = "Blob URLs", description = "Temporary download URL management")
@SecurityRequirement(name = "bearerAuth")
public class BlobUrlController {
    
    private final BlobUrlService blobUrlService;

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
        log.debug("Creating blob URL for file: {}", request.getFilePath());
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
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Blob URL token", required = true)
            @PathVariable String token,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        log.debug("Download requested for blob URL token: {}", token);
        
        // Get the file resource and metadata
        Resource resource = blobUrlService.validateAndGetFile(token);
        BlobUrlResponse blobUrlInfo = blobUrlService.getBlobUrlStatus(token);
        
        // Handle range requests for partial downloads
        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            return handleRangeRequest(resource, blobUrlInfo, rangeHeader, response);
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
    
    private ResponseEntity<Resource> handleRangeRequest(
            Resource resource, 
            BlobUrlResponse blobUrlInfo, 
            String rangeHeader,
            HttpServletResponse response) throws IOException {
        
        long fileSize = blobUrlInfo.getFileSize();
        
        // Parse range header (simplified - supports single range only)
        String range = rangeHeader.substring(6); // Remove "bytes="
        String[] ranges = range.split("-");
        
        long start = 0;
        long end = fileSize - 1;
        
        try {
            if (!ranges[0].isEmpty()) {
                start = Long.parseLong(ranges[0]);
            }
            if (ranges.length > 1 && !ranges[1].isEmpty()) {
                end = Long.parseLong(ranges[1]);
            }
            
            // Validate range
            if (start < 0 || end >= fileSize || start > end) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize);
                return ResponseEntity.status(416).build();
            }
            
            long contentLength = end - start + 1;
            String contentRange = "bytes " + start + "-" + end + "/" + fileSize;
            
            return ResponseEntity.status(206) // Partial Content
                    .contentType(MediaType.parseMediaType(blobUrlInfo.getContentType()))
                    .contentLength(contentLength)
                    .header(HttpHeaders.CONTENT_RANGE, contentRange)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + blobUrlInfo.getFilename() + "\"")
                    .body(resource);
                    
        } catch (NumberFormatException e) {
            log.warn("Invalid range header: {}", rangeHeader);
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize);
            return ResponseEntity.status(416).build();
        }
    }
}