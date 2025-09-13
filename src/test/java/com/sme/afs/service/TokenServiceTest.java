package com.sme.afs.service;

import com.sme.afs.config.BlobUrlProperties;
import com.sme.afs.model.BlobUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private BlobUrlProperties blobUrlProperties;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        lenient().when(blobUrlProperties.getTokenLength()).thenReturn(32);
        tokenService = new TokenService(blobUrlProperties);
    }

    @Test
    void generateSecureToken_ShouldReturnNonEmptyToken() {
        String token = tokenService.generateSecureToken();
        
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void generateSecureToken_ShouldReturnUrlSafeCharacters() {
        String token = tokenService.generateSecureToken();
        
        // Should only contain URL-safe Base64 characters
        assertThat(token).matches("^[A-Za-z0-9_-]+$");
    }

    @Test
    void generateSecureToken_ShouldReturnExpectedLength() {
        String token = tokenService.generateSecureToken();
        
        // Base64 encoding of 32 bytes should produce ~43 characters (no padding)
        assertThat(token.length()).isBetween(42, 44);
    }

    @Test
    void generateSecureToken_ShouldReturnUniqueTokens() {
        Set<String> tokens = new HashSet<>();
        
        // Generate 1000 tokens and verify they're all unique
        for (int i = 0; i < 1000; i++) {
            String token = tokenService.generateSecureToken();
            assertThat(tokens).doesNotContain(token);
            tokens.add(token);
        }
    }

    @Test
    void validateTokenFormat_ShouldReturnTrueForValidToken() {
        String validToken = tokenService.generateSecureToken();
        
        boolean isValid = tokenService.validateTokenFormat(validToken);
        
        assertThat(isValid).isTrue();
    }

    @Test
    void validateTokenFormat_ShouldReturnFalseForNullToken() {
        boolean isValid = tokenService.validateTokenFormat(null);
        
        assertThat(isValid).isFalse();
    }

    @Test
    void validateTokenFormat_ShouldReturnFalseForEmptyToken() {
        boolean isValid = tokenService.validateTokenFormat("");
        
        assertThat(isValid).isFalse();
    }

    @Test
    void validateTokenFormat_ShouldReturnFalseForWhitespaceToken() {
        boolean isValid = tokenService.validateTokenFormat("   ");
        
        assertThat(isValid).isFalse();
    }

    @Test
    void validateTokenFormat_ShouldReturnFalseForTooShortToken() {
        boolean isValid = tokenService.validateTokenFormat("abc");
        
        assertThat(isValid).isFalse();
    }

    @Test
    void validateTokenFormat_ShouldReturnFalseForTooLongToken() {
        String tooLongToken = "a".repeat(100);
        
        boolean isValid = tokenService.validateTokenFormat(tooLongToken);
        
        assertThat(isValid).isFalse();
    }

    @Test
    void validateTokenFormat_ShouldReturnFalseForInvalidCharacters() {
        String invalidToken = "abc123!@#";
        
        boolean isValid = tokenService.validateTokenFormat(invalidToken);
        
        assertThat(isValid).isFalse();
    }

    @Test
    void validateTokenFormat_ShouldReturnFalseForTokenWithSpaces() {
        String tokenWithSpaces = "abc def ghi";
        
        boolean isValid = tokenService.validateTokenFormat(tokenWithSpaces);
        
        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenExpired_ShouldReturnTrueForNullBlobUrl() {
        boolean isExpired = tokenService.isTokenExpired(null);
        
        assertThat(isExpired).isTrue();
    }

    @Test
    void isTokenExpired_ShouldReturnTrueForExpiredBlobUrl() {
        BlobUrl expiredBlobUrl = BlobUrl.builder()
                .token("test-token")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        
        boolean isExpired = tokenService.isTokenExpired(expiredBlobUrl);
        
        assertThat(isExpired).isTrue();
    }

    @Test
    void isTokenExpired_ShouldReturnFalseForActiveBlobUrl() {
        BlobUrl activeBlobUrl = BlobUrl.builder()
                .token("test-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        
        boolean isExpired = tokenService.isTokenExpired(activeBlobUrl);
        
        assertThat(isExpired).isFalse();
    }

    @Test
    void validateToken_ShouldReturnTrueForValidActiveToken() {
        String token = tokenService.generateSecureToken();
        BlobUrl activeBlobUrl = BlobUrl.builder()
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        
        boolean isValid = tokenService.validateToken(token, activeBlobUrl);
        
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_ShouldReturnFalseForInvalidTokenFormat() {
        BlobUrl activeBlobUrl = BlobUrl.builder()
                .token("valid-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        
        boolean isValid = tokenService.validateToken("invalid!token", activeBlobUrl);
        
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_ShouldReturnFalseForNullBlobUrl() {
        String token = tokenService.generateSecureToken();
        
        boolean isValid = tokenService.validateToken(token, null);
        
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_ShouldReturnFalseForExpiredBlobUrl() {
        String token = tokenService.generateSecureToken();
        BlobUrl expiredBlobUrl = BlobUrl.builder()
                .token(token)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        
        boolean isValid = tokenService.validateToken(token, expiredBlobUrl);
        
        assertThat(isValid).isFalse();
    }

    @Test
    void validateTokenFormat_ShouldHandleDifferentTokenLengths() {
        // Test with different token lengths
        when(blobUrlProperties.getTokenLength()).thenReturn(16);
        TokenService shortTokenService = new TokenService(blobUrlProperties);
        
        String shortToken = shortTokenService.generateSecureToken();
        assertThat(shortTokenService.validateTokenFormat(shortToken)).isTrue();
        
        when(blobUrlProperties.getTokenLength()).thenReturn(64);
        TokenService longTokenService = new TokenService(blobUrlProperties);
        
        String longToken = longTokenService.generateSecureToken();
        assertThat(longTokenService.validateTokenFormat(longToken)).isTrue();
    }
}