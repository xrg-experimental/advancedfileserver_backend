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
            org.springframework.core.io.support.ResourceRegion region =
                    new org.springframework.core.io.support.ResourceRegion(resource, start, count);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_RANGE, contentRange);
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    org.springframework.http.ContentDisposition.attachment()
                            .filename(blobUrlInfo.getFilename(), java.nio.charset.StandardCharsets.UTF_8)
                            .build()
                            .toString());

            return ResponseEntity.status(206)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .headers(headers)
                    .contentLength(count)
                    .body(resource);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid range header: {}", rangeHeader);
            return ResponseEntity.status(416)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
        }
    }
}