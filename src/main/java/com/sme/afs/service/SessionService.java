package com.sme.afs.service;

import com.sme.afs.config.SessionProperties;
import com.sme.afs.exception.session.InvalidSessionException;
import com.sme.afs.exception.session.MaxSessionsExceededException;
import com.sme.afs.exception.session.SessionExpiredException;
import com.sme.afs.exception.session.SessionNotFoundException;
import com.sme.afs.model.BlacklistedToken;
import com.sme.afs.model.UserSession;
import com.sme.afs.repository.BlacklistedTokenRepository;
import com.sme.afs.repository.UserSessionRepository;
import com.sme.afs.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final UserSessionRepository sessionRepository;
    private final SessionProperties sessionProperties;
    
    public int getMaxConcurrentSessions() {
        return sessionProperties.getMaxConcurrent();
    }
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final JwtService jwtService;

    public int getRefreshWindow() {
        return sessionProperties.getRefreshWindow();
    }

    @Transactional
    public UserSession createSession(String username, String token) {
        // Check for existing active sessions
        List<UserSession> activeSessions = sessionRepository.findByUsernameAndActive(username, true);
        
        // If max concurrent sessions reached, throw exception
        if (activeSessions.size() >= sessionProperties.getMaxConcurrent()) {
            throw new MaxSessionsExceededException(username, sessionProperties.getMaxConcurrent());
        }

        // Create new session
        UserSession session = new UserSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUsername(username);
        session.setToken(token);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(sessionProperties.getTimeout()));
        
        return sessionRepository.save(session);
    }

    @Transactional
    public boolean validateSession(String sessionId) {
        return sessionRepository.findById(sessionId)
            .map(session -> {
                if (!session.isActive()) {
                    throw new InvalidSessionException(sessionId);
                }
                if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
                    throw new SessionExpiredException(sessionId);
                }
                updateLastAccessed(session);
                return true;
            })
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    @Transactional
    public void invalidateSession(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setActive(false);
            sessionRepository.save(session);
            
            // Blacklist the token
            if (session.getToken() != null) {
                BlacklistedToken blacklistedToken = new BlacklistedToken();
                blacklistedToken.setToken(session.getToken());
                blacklistedToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtService.getJwtExpiration() / 1000));
                blacklistedTokenRepository.save(blacklistedToken);
            }
            
            log.debug("Session invalidated and token blacklisted: {}", sessionId);
        });
    }

    @Transactional
    public void invalidateUserSessions(String username) {
        List<UserSession> sessions = sessionRepository.findByUsername(username);
        sessions.forEach(session -> {
            session.setActive(false);
            sessionRepository.save(session);
            
            // Blacklist the token if present
            if (session.getToken() != null) {
                BlacklistedToken blacklistedToken = new BlacklistedToken();
                blacklistedToken.setToken(session.getToken());
                blacklistedToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtService.getJwtExpiration() / 1000));
                blacklistedTokenRepository.save(blacklistedToken);
            }
        });
        log.debug("All sessions ({}) invalidated for user: {}", sessions.size(), username);
    }

    @Transactional(readOnly = true)
    public List<UserSession> getUserSessions(String username) {
        return sessionRepository.findByUsername(username);
    }

    @Scheduled(fixedRateString = "#{${session.cleanup-interval} * 1000}")
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        
        // Mark expired active sessions as inactive
        List<UserSession> expiredSessions = sessionRepository.findExpiredSessions(now);
        expiredSessions.forEach(session -> {
            session.setActive(false);
            sessionRepository.save(session);
        });
        if (!expiredSessions.isEmpty()) {
            log.debug("Marked {} expired sessions as inactive", expiredSessions.size());
        }
        
        // Delete all expired sessions (both active and inactive)
        LocalDateTime cleanupThreshold = now.minusDays(1); // Keep expired sessions for 1 day before deletion
        sessionRepository.deleteExpiredSessions(cleanupThreshold);
        
        // Cleanup expired blacklisted tokens
        blacklistedTokenRepository.deleteExpiredTokens(now);
        
        log.debug("Session cleanup completed at {}", now);
    }

    @Transactional
    public void updateLastAccessed(String sessionId) {
        UserSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
        
        if (!session.isActive()) {
            throw new InvalidSessionException(sessionId);
        }
        
        LocalDateTime now = LocalDateTime.now();
        session.setLastAccessedAt(now);
        session.setExpiresAt(now.plusSeconds(sessionProperties.getTimeout()));
        sessionRepository.save(session);
        log.trace("Updated last accessed time for session: {}", sessionId);
    }

    private void updateLastAccessed(UserSession session) {
        LocalDateTime now = LocalDateTime.now();
        session.setLastAccessedAt(now);
        session.setExpiresAt(now.plusSeconds(sessionProperties.getTimeout()));
        sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public int getCurrentSessionCount(String username) {
        return sessionRepository.findByUsernameAndActive(username, true).size();
    }
}
