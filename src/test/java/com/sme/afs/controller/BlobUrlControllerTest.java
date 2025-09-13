package com.sme.afs.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.afs.dto.BlobUrlCreateRequest;
import com.sme.afs.dto.BlobUrlResponse;
import com.sme.afs.exception.*;
import com.sme.afs.service.BlobUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlobUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlobUrlService blobUrlService;

    @Autowired
    private ObjectMapper objectMapper;

    private BlobUrlCreateRequest createRequest;
    private BlobUrlCreateRequest invalidRequest;
    private BlobUrlResponse blobUrlResponse;

    @BeforeEach
    void setUp() {
        // Arrange
        createRequest = new BlobUrlCreateRequest("/shared/test-file.pdf");
        invalidRequest = new BlobUrlCreateRequest("/shared/nonexistent.pdf");

        blobUrlResponse = BlobUrlResponse.builder()
                .downloadUrl("/api/blob-urls/downloads/test-token-123")
                .token("test-token-123")
                .filename("test-file.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .status("active")
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void createBlobUrl_Success() throws Exception {
        // Arrange
        when(blobUrlService.createBlobUrl(anyString())).thenReturn(blobUrlResponse);

        // Act
        MvcResult result = mockMvc.perform(post("/blob-urls/create").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        // Assert
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        BlobUrlResponse body = objectMapper.readValue(response.getContentAsString(), BlobUrlResponse.class);
        assertThat(body.getDownloadUrl()).isEqualTo("/api/blob-urls/downloads/test-token-123");
        assertThat(body.getToken()).isEqualTo("test-token-123");
        assertThat(body.getFilename()).isEqualTo("test-file.pdf");
        assertThat(body.getFileSize()).isEqualTo(1024L);
        assertThat(body.getContentType()).isEqualTo("application/pdf");
        assertThat(body.getStatus()).isEqualTo("active");

        verify(blobUrlService).createBlobUrl("/shared/test-file.pdf");
    }

    @Test
    @WithMockUser(roles = "USER")
    void createBlobUrl_FileNotFound() throws Exception {
        // Arrange
        when(blobUrlService.createBlobUrl(anyString()))
                .thenThrow(new FileNotFoundException("/shared/nonexistent.pdf"));

        // Act
        MvcResult result = mockMvc.perform(post("/blob-urls/create").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andReturn();

        // Assert
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Map<String, Object> problem = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
        assertThat(problem.get("code")).isEqualTo("FILE_NOT_FOUND");
    }

    @Test
    @WithMockUser(roles = "USER")
    void createBlobUrl_CrossFilesystemError() throws Exception {
        // Arrange
        when(blobUrlService.createBlobUrl(anyString()))
                .thenThrow(new CrossFilesystemException("Cannot create hard link across filesystems"));

        // Act
        MvcResult result = mockMvc.perform(post("/blob-urls/create").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andReturn();

        // Assert
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Map<String, Object> problem = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
        assertThat(problem.get("code")).isEqualTo("CROSS_FILESYSTEM");
    }

    @Test
    @WithMockUser(roles = "USER")
    void createBlobUrl_LinkCreationFailed() throws Exception {
        // Arrange
        when(blobUrlService.createBlobUrl(anyString()))
                .thenThrow(new LinkCreationFailedException("Failed to create hard link"));

        // Act
        MvcResult result = mockMvc.perform(post("/blob-urls/create").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        // Assert
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Map<String, Object> problem = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
        assertThat(problem.get("code")).isEqualTo("LINK_CREATION_FAILED");
    }

    @Test
    @WithMockUser(roles = "USER")
    void createBlobUrl_ValidationError_EmptyPath() throws Exception {
        // Arrange
        BlobUrlCreateRequest invalidRequest = new BlobUrlCreateRequest("");

        // Act
        MvcResult result = mockMvc.perform(post("/blob-urls/create").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andReturn();

        // Assert
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Map<String, Object> problem = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
        assertThat(problem.get("code")).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void createBlobUrl_Unauthorized() throws Exception {
        // Arrange
        // (no stubbing required)

        // Act
        MvcResult result = mockMvc.perform(post("/blob-urls/create").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        // Assert
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getBlobUrlStatus_Success() throws Exception {
        // Arrange
        when(blobUrlService.getBlobUrlStatus("test-token-123")).thenReturn(blobUrlResponse);

        // Act
        MvcResult result = mockMvc.perform(get("/blob-urls/test-token-123/status"))
                .andReturn();

        // Assert
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        BlobUrlResponse body = objectMapper.readValue(response.getContentAsString(), BlobUrlResponse.class);
        assertThat(body.getToken()).isEqualTo("test-token-123");
        assertThat(body.getStatus()).isEqualTo("active");
        assertThat(body.getFilename()).isEqualTo("test-file.pdf");

        verify(blobUrlService).getBlobUrlStatus("test-token-123");
    }

    @Test
    @WithMockUser(roles = "USER")
    void getBlobUrlStatus_TokenInvalid() throws Exception {
        // Arrange
        when(blobUrlService.getBlobUrlStatus("invalid-token"))
                .thenThrow(new TokenInvalidException("invalid-token"));

        // Act
        MvcResult result = mockMvc.perform(get("/blob-urls/invalid-token/status"))
                .andReturn();

        // Assert
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Map<String, Object> problem = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
        assertThat(problem.get("code")).isEqualTo("TOKEN_INVALID");
    }

    @Test
    void getBlobUrlStatus_Unauthorized() throws Exception {
        // Arrange
        // (no stubbing required)

        // Act
        MvcResult result = mockMvc.perform(get("/blob-urls/test-token-123/status"))
                .andReturn();

        // Assert
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void downloadFile_Success() throws Exception {
        // Arrange
        Resource mockResource = new ByteArrayResource("test file content".getBytes());
        when(blobUrlService.validateAndGetFile("test-token-123")).thenReturn(mockResource);
        when(blobUrlService.getBlobUrlStatus("test-token-123")).thenReturn(blobUrlResponse);

        // Act
        MvcResult result = mockMvc.perform(get("/blob-urls/downloads/test-token-123"))
                .andReturn();

        // Assert
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(response.getHeader("Content-Disposition")).isEqualTo("attachment; filename=\"test-file.pdf\"");
        assertThat(response.getHeader("Accept-Ranges")).isEqualTo("bytes");
        assertThat(response.getHeader("Content-Length")).isEqualTo("1024");

        verify(blobUrlService).validateAndGetFile("test-token-123");
        verify(blobUrlService).getBlobUrlStatus("test-token-123");
    }

    @Test
    void downloadFile_TokenInvalid() throws Exception {
        // Arrange
        when(blobUrlService.validateAndGetFile("invalid-token"))
                .thenThrow(new TokenInvalidException("invalid-token"));

        // Act
        MvcResult result = mockMvc.perform(get("/blob-urls/downloads/invalid-token"))
                .andReturn();

        // Assert
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Map<String, Object> problem = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
        assertThat(problem.get("code")).isEqualTo("TOKEN_INVALID");

        verify(blobUrlService, never()).getBlobUrlStatus(anyString());
    }

    @Test
    void downloadFile_RangeRequest() throws Exception {
        // Arrange
        Resource mockResource = new ByteArrayResource(new byte[1024]);
        when(blobUrlService.validateAndGetFile("test-token-123")).thenReturn(mockResource);
        when(blobUrlService.getBlobUrlStatus("test-token-123")).thenReturn(blobUrlResponse);

        // Act
        MvcResult result = mockMvc.perform(get("/blob-urls/downloads/test-token-123")
                        .header("Range", "bytes=0-499"))
                .andReturn();

        // Assert
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(206);
        assertThat(response.getHeader("Content-Range")).isEqualTo("bytes 0-499/1024");
        assertThat(response.getHeader("Accept-Ranges")).isEqualTo("bytes");
        assertThat(response.getContentLengthLong()).isEqualTo(500L);

        verify(blobUrlService).validateAndGetFile("test-token-123");
        verify(blobUrlService).getBlobUrlStatus("test-token-123");
    }

    @Test
    void downloadFile_InvalidRangeRequest() throws Exception {
        // Arrange
        Resource mockResource = new ByteArrayResource("test file content".getBytes());
        when(blobUrlService.validateAndGetFile("test-token-123")).thenReturn(mockResource);
        when(blobUrlService.getBlobUrlStatus("test-token-123")).thenReturn(blobUrlResponse);

        // Act
        MvcResult result = mockMvc.perform(get("/blob-urls/downloads/test-token-123")
                        .header("Range", "bytes=2000-3000")) // Range beyond file size
                .andReturn();

        // Assert
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(416);
        assertThat(response.getHeader("Content-Range")).isEqualTo("bytes */1024");
    }

    @Test
    void downloadFile_NoAuthentication_AllowedForDownload() throws Exception {
        // Arrange
        // Download endpoint should allow unauthenticated access for valid tokens
        Resource mockResource = new ByteArrayResource("test file content".getBytes());
        when(blobUrlService.validateAndGetFile("test-token-123")).thenReturn(mockResource);
        when(blobUrlService.getBlobUrlStatus("test-token-123")).thenReturn(blobUrlResponse);

        // Act
        MvcResult result = mockMvc.perform(get("/blob-urls/downloads/test-token-123"))
                .andReturn();

        // Assert
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
    }
}