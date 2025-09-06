package com.sme.afs.repository;

import com.sme.afs.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {
    boolean existsByToken(String token);
    
    @Modifying
    @Query("DELETE FROM BlacklistedToken b WHERE b.expiresAt < ?1")
    void deleteExpiredTokens(LocalDateTime now);
}
