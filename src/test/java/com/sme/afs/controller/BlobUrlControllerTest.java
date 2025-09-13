package com.sme.afs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.afs.dto.BlobUrlCreateRequest;
import com.sme.afs.dto.BlobUrlResponse;
import com.sme.afs.exception.*;
import com.sme.afs.service.BlobUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class BlobUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlobUrlService blobUrlService;

    @Autowired
    private ObjectMapper objectMapper;

    private BlobUrlCreateRequest createRequest;
    private BlobUrlResponse blobUrlResponse;

    @BeforeEach
    void setUp() {
        createRequest = new BlobUrlCreateRequest("/shared/test-file.pdf");
        
        blobUrlResponse = BlobUrlResponse.builder()
                .downloadUrl("/api/blob-urls/downloads/test-token-123")
                .token("test-token-123")
                .filename("test-file.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .status("active")
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void createBlobUrl_Success() throws Exception {
        when(blobUrlService.createBlobUrl(anyString())).thenReturn(blobUrlResponse);

        mockMvc.perform(post("/blob-urls/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.downloadUrl").value("/api/blob-urls/downloads/test-token-123"))
                .andExpect(jsonPath("$.token").value("test-token-123"))
                .andExpect(jsonPath("$.filename").value("test-file.pdf"))
                .andExpect(jsonPath("$.fileSize").value(1024))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.status").value("active"));

        verify(blobUrlService).createBlobUrl("/shared/test-file.pdf");
    }

    @Test
    @WithMockUser(roles = "USER")
    void createBlobUrl_FileNotFound() throws Exception {
        when(blobUrlService.createBlobUrl(anyString()))
                .thenThrow(new FileNotFoundException("/shared/nonexistent.pdf"));

        mockMvc.perform(post("/blob-urls/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createBlobUrl_CrossFilesystemError() throws Exception {
        when(blobUrlService.createBlobUrl(anyString()))
                .thenThrow(new CrossFilesystemException("Cannot create hard link across filesystems"));

        mockMvc.perform(post("/blob-urls/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("CROSS_FILESYSTEM"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createBlobUrl_LinkCreationFailed() throws Exception {
        when(blobUrlService.createBlobUrl(anyString()))
                .thenThrow(new LinkCreationFailedException("Failed to create hard link"));

        mockMvc.perform(post("/blob-urls/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("LINK_CREATION_FAILED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createBlobUrl_ValidationError_EmptyPath() throws Exception {
        BlobUrlCreateRequest invalidRequest = new BlobUrlCreateRequest("");

        mockMvc.perform(post("/blob-urls/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createBlobUrl_Unauthorized() throws Exception {
        mockMvc.perform(post("/blob-urls/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getBlobUrlStatus_Success() throws Exception {
        when(blobUrlService.getBlobUrlStatus("test-token-123")).thenReturn(blobUrlResponse);

        mockMvc.perform(get("/blob-urls/test-token-123/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("test-token-123"))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.filename").value("test-file.pdf"));

        verify(blobUrlService).getBlobUrlStatus("test-token-123");
    }

    @Test
    @WithMockUser(roles = "USER")
    void getBlobUrlStatus_TokenInvalid() throws Exception {
        when(blobUrlService.getBlobUrlStatus("invalid-token"))
                .thenThrow(new TokenInvalidException("invalid-token"));

        mockMvc.perform(get("/blob-urls/invalid-token/status"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void getBlobUrlStatus_Unauthorized() throws Exception {
        mockMvc.perform(get("/blob-urls/test-token-123/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void downloadFile_Success() throws Exception {
        Resource mockResource = new ByteArrayResource("test file content".getBytes());
        when(blobUrlService.validateAndGetFile("test-token-123")).thenReturn(mockResource);
        when(blobUrlService.getBlobUrlStatus("test-token-123")).thenReturn(blobUrlResponse);

        mockMvc.perform(get("/blob-urls/downloads/test-token-123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test-file.pdf\""))
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("Content-Length", "1024"));

        verify(blobUrlService).validateAndGetFile("test-token-123");
        verify(blobUrlService).getBlobUrlStatus("test-token-123");
    }

    @Test
    void downloadFile_TokenInvalid() throws Exception {
        when(blobUrlService.validateAndGetFile("invalid-token"))
                .thenThrow(new TokenInvalidException("invalid-token"));

        mockMvc.perform(get("/blob-urls/downloads/invalid-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void downloadFile_RangeRequest() throws Exception {
        Resource mockResource = new ByteArrayResource("test file content".getBytes());
        when(blobUrlService.validateAndGetFile("test-token-123")).thenReturn(mockResource);
        when(blobUrlService.getBlobUrlStatus("test-token-123")).thenReturn(blobUrlResponse);

        mockMvc.perform(get("/blob-urls/downloads/test-token-123")
                        .header("Range", "bytes=0-499"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 0-499/1024"))
                .andExpect(header().string("Accept-Ranges", "bytes"));

        verify(blobUrlService).validateAndGetFile("test-token-123");
        verify(blobUrlService).getBlobUrlStatus("test-token-123");
    }

    @Test
    void downloadFile_InvalidRangeRequest() throws Exception {
        Resource mockResource = new ByteArrayResource("test file content".getBytes());
        when(blobUrlService.validateAndGetFile("test-token-123")).thenReturn(mockResource);
        when(blobUrlService.getBlobUrlStatus("test-token-123")).thenReturn(blobUrlResponse);

        mockMvc.perform(get("/blob-urls/downloads/test-token-123")
                        .header("Range", "bytes=2000-3000")) // Range beyond file size
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string("Content-Range", "bytes */1024"));
    }

    @Test
    void downloadFile_NoAuthentication_AllowedForDownload() throws Exception {
        // Download endpoint should allow unauthenticated access for valid tokens
        Resource mockResource = new ByteArrayResource("test file content".getBytes());
        when(blobUrlService.validateAndGetFile("test-token-123")).thenReturn(mockResource);
        when(blobUrlService.getBlobUrlStatus("test-token-123")).thenReturn(blobUrlResponse);

        mockMvc.perform(get("/blob-urls/downloads/test-token-123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }
}