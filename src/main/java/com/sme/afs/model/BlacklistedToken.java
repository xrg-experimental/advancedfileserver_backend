package com.sme.afs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "blacklisted_tokens")
public class BlacklistedToken {
    @Id
    private String token;
    
    private String tokenHash;
    
    private String username;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private LocalDateTime blacklistedAt;
    
    @PrePersist
    protected void onCreate() {
        blacklistedAt = LocalDateTime.now();
    }
}
