package com.sme.afs.service;

import com.sme.afs.config.SynologyProperties;
import com.sme.afs.dto.DsmAuthResponse;
import com.sme.afs.exception.AfsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DsmAuthenticationServiceTest {

    @Mock
    private RestTemplate synologyRestTemplate;

    @Mock
    private SynologyProperties synologyProperties;

    private DsmAuthenticationService dsmAuthenticationService;

    @BeforeEach
    void setUp() {
        when(synologyProperties.getProtocol()).thenReturn("http");
        when(synologyProperties.getHost()).thenReturn("localhost");
        when(synologyProperties.getPort()).thenReturn(5000);
        when(synologyProperties.getApiVersion()).thenReturn("2");
        when(synologyProperties.getSessionName()).thenReturn("FileStation");

        dsmAuthenticationService = new DsmAuthenticationService(synologyRestTemplate, synologyProperties);
    }

    @Test
    void shouldAuthenticateValidUser() {
        // Given
        DsmAuthResponse response = new DsmAuthResponse();
        response.setSuccess(true);
        DsmAuthResponse.Data data = new DsmAuthResponse.Data();
        data.setSid("test-session-id");
        response.setData(data);

        when(synologyRestTemplate.getForObject(any(String.class), eq(DsmAuthResponse.class)))
                .thenReturn(response);

        // When
        boolean result = dsmAuthenticationService.authenticate("admin1", "password123", "333666");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldFailAuthenticationForInvalidCredentials() {
        // Given
        DsmAuthResponse response = new DsmAuthResponse();
        response.setSuccess(false);
        DsmAuthResponse.Error error = new DsmAuthResponse.Error();
        error.setCode(400);
        response.setError(error);

        when(synologyRestTemplate.getForObject(any(String.class), eq(DsmAuthResponse.class)))
                .thenReturn(response);

        // When
        boolean result = dsmAuthenticationService.authenticate("admin1", "password123", "333666");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldFailAuthenticationForInvalidOtp() {
        // Given
        DsmAuthResponse response = new DsmAuthResponse();
        response.setSuccess(false);
        DsmAuthResponse.Error error = new DsmAuthResponse.Error();
        error.setCode(400);
        response.setError(error);

        when(synologyRestTemplate.getForObject(any(String.class), eq(DsmAuthResponse.class)))
                .thenReturn(response);

        // When
        boolean result = dsmAuthenticationService.authenticate("admin1", "wrongpassword", "555000");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldHandleNullResponse() {
        // Given
        when(synologyRestTemplate.getForObject(any(String.class), eq(DsmAuthResponse.class)))
                .thenReturn(null);

        // When/Then
        assertThrows(AfsException.class, () -> 
            dsmAuthenticationService.authenticate("testuser", "password", "123456")
        );
    }

    @Test
    void shouldHandleRestClientException() {
        // Given
        when(synologyRestTemplate.getForObject(any(String.class), eq(DsmAuthResponse.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // When/Then
        assertThrows(AfsException.class, () -> 
            dsmAuthenticationService.authenticate("testuser", "password", "123456")
        );
    }
}
