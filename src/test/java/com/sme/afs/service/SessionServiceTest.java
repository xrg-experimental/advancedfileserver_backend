package com.sme.afs.service;

import com.sme.afs.config.SessionProperties;
import com.sme.afs.exception.session.MaxSessionsExceededException;
import com.sme.afs.exception.session.SessionExpiredException;
import com.sme.afs.model.BlacklistedToken;
import com.sme.afs.model.UserSession;
import com.sme.afs.repository.BlacklistedTokenRepository;
import com.sme.afs.repository.UserSessionRepository;
import com.sme.afs.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private UserSessionRepository sessionRepository;
    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private SessionProperties sessionProperties;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(
            sessionRepository,
            sessionProperties,
            blacklistedTokenRepository,
            jwtService
        );
    }

    @Test
    void shouldCreateNewSession() {
        // Given
        String username = "testuser";
        String token = "test.jwt.token";
        when(sessionProperties.getMaxConcurrent()).thenReturn(3);
        when(sessionProperties.getTimeout()).thenReturn(1800);
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(i -> i.getArgument(0));

        // When
        UserSession session = sessionService.createSession(username, token);

        // Then
        assertThat(session).isNotNull();
        assertThat(session.getUsername()).isEqualTo(username);
        assertThat(session.getToken()).isEqualTo(token);
        assertThat(session.isActive()).isTrue();
        verify(sessionRepository).save(any(UserSession.class));
    }

    @Test
    void shouldThrowExceptionWhenMaxSessionsReached() {
        // Given
        String username = "testuser";
        UserSession oldSession = new UserSession();
        oldSession.setSessionId("old-session");
        oldSession.setActive(true);
        
        when(sessionProperties.getMaxConcurrent()).thenReturn(3);
        when(sessionRepository.findByUsernameAndActive(username, true))
            .thenReturn(Arrays.asList(oldSession, oldSession, oldSession));

        // When/Then
        assertThrows(MaxSessionsExceededException.class, ()
                -> sessionService.createSession(username, "new.token"));
    }

    @Test
    void shouldValidateActiveSession() {
        // Given
        String sessionId = "test-session";
        UserSession session = new UserSession();
        session.setActive(true);
        session.setExpiresAt(LocalDateTime.now().plusHours(1));
        
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(UserSession.class))).thenReturn(session);

        // When
        boolean isValid = sessionService.validateSession(sessionId);

        // Then
        assertThat(isValid).isTrue();
        verify(sessionRepository).save(any(UserSession.class));
    }

    @Test
    void shouldThrowExceptionWhenSessionExpired() {
        // Given
        String sessionId = "test-session";
        UserSession session = new UserSession();
        session.setActive(true);
        session.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // When/Then
        assertThrows(SessionExpiredException.class, () -> sessionService.validateSession(sessionId));
    }

    @Test
    void shouldInvalidateSessionAndBlacklistToken() {
        // Given
        String sessionId = "test-session";
        UserSession session = new UserSession();
        session.setSessionId(sessionId);
        session.setToken("test.token");
        session.setActive(true);
        
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(UserSession.class))).thenReturn(session);

        // When
        sessionService.invalidateSession(sessionId);

        // Then
        verify(sessionRepository).save(argThat(s -> !s.isActive()));
        verify(blacklistedTokenRepository).save(any(BlacklistedToken.class));
    }

    @Test
    void shouldCleanupExpiredSessions() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        UserSession expiredSession = new UserSession();
        expiredSession.setActive(true);
        expiredSession.setExpiresAt(now.minusHours(1));
        
        when(sessionRepository.findExpiredSessions(any()))
            .thenReturn(List.of(expiredSession));

        // When
        sessionService.cleanupExpiredSessions();

        // Then
        verify(sessionRepository).save(argThat(session -> !session.isActive()));
        verify(sessionRepository).deleteExpiredSessions(any());
        verify(blacklistedTokenRepository).deleteExpiredTokens(any());
    }

    @Test
    void shouldGetUserSessions() {
        // Given
        String username = "testuser";
        UserSession session = new UserSession();
        session.setUsername(username);
        
        when(sessionRepository.findByUsername(username))
            .thenReturn(List.of(session));

        // When
        List<UserSession> sessions = sessionService.getUserSessions(username);

        // Then
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getUsername()).isEqualTo(username);
    }
}
