package com.sme.afs.service;

import com.sme.afs.config.SynologyProperties;
import com.sme.afs.dto.DsmAuthResponse;
import com.sme.afs.exception.AfsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class DsmAuthenticationService {
    private final RestTemplate synologyRestTemplate;
    private final SynologyProperties synologyProperties;

    public boolean authenticate(String username, String password, String otpCode) {
        String baseUrl = String.format("%s://%s:%d/webapi/auth.cgi",
                synologyProperties.getProtocol(),
                synologyProperties.getHost(),
                synologyProperties.getPort());

        log.debug("Authenticating with DSM for user: {}", username);
        log.debug("DSM host: {}:{}", synologyProperties.getHost(), synologyProperties.getPort());
        
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("api", "SYNO.API.Auth")
                .queryParam("version", synologyProperties.getApiVersion())
                .queryParam("method", "login")
                .queryParam("account", username)
                .queryParam("passwd", password)
                .queryParam("otp_code", otpCode)
                .queryParam("session", synologyProperties.getSessionName())
                .queryParam("format", "sid")
                .toUriString();

        try {
            DsmAuthResponse response = synologyRestTemplate.getForObject(url, DsmAuthResponse.class);
            
            if (response == null) {
                throw new AfsException("Failed to get response from DSM authentication API");
            }

            if (!response.isSuccess()) {
                log.warn("DSM authentication failed for user: {}, error code: {}, response: {}", 
                    username, response.getError() != null ? response.getError().getCode() : "unknown", response);
                return false;
            }
            log.debug("DSM authentication successful for user: {}", username);

            return response.getData() != null && response.getData().getSid() != null;
        } catch (RestClientException e) {
            log.error("Error while authenticating with DSM", e);
            log.error("DSM authentication service error: {}", e.getMessage());
            throw new AfsException(HttpStatus.SERVICE_UNAVAILABLE, 
                "DSM authentication service unavailable: " + e.getMessage());
        }
    }
}
