package com.sme.afs.exception;

import com.sme.afs.dto.ProblemResponse;
import com.sme.afs.exception.session.SessionExpiredException;
import com.sme.afs.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        MDC.put(CorrelationIdFilter.CORRELATION_ID, "test-correlation-id");
    }

    @Test
    void handleSessionExpiredException_shouldReturnProblemResponse() {
        // Given
        SessionExpiredException exception = new SessionExpiredException();

        // When
        ResponseEntity<ProblemResponse> response = handler.handleSessionExpiredException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType().toString()).contains("application/problem+json");

        ProblemResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.type()).isEqualTo("https://errors.afs/SESSION_EXPIRED");
        assertThat(body.title()).isEqualTo("Session expired");
        assertThat(body.status()).isEqualTo(401);
        assertThat(body.detail()).isEqualTo("The session has expired. Please authenticate again.");
        assertThat(body.code()).isEqualTo("SESSION_EXPIRED");
        assertThat(body.correlationId()).isEqualTo("test-correlation-id");
        assertThat(body.instance()).isEqualTo("urn:uuid:test-correlation-id");
    }
}