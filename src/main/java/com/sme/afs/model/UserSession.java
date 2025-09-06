package com.sme.afs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "user_sessions")
public class UserSession {
    @Id
    private String sessionId;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime lastAccessedAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    private String token;
    
    @Column(nullable = false)
    private boolean active = true;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastAccessedAt = createdAt;
    }
}
