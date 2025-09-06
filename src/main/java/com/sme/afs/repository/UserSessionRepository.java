package com.sme.afs.repository;

import com.sme.afs.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    List<UserSession> findByUsernameAndActive(String username, boolean active);
    
    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < ?1 AND s.active = true")
    List<UserSession> findExpiredSessions(LocalDateTime now);
    
    List<UserSession> findByUsername(String username);
    
    @Query("SELECT s FROM UserSession s WHERE s.sessionId = ?1 AND s.active = true AND s.expiresAt > ?2")
    Optional<UserSession> findActiveSession(String sessionId, LocalDateTime now);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.username = ?1")
    void deactivateUserSessions(String username);
    
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < ?1")
    void deleteExpiredSessions(LocalDateTime before);
    
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.username = ?1 AND s.active = true")
    long countActiveSessions(String username);
}
