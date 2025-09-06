package com.sme.afs.security;

import com.sme.afs.model.Role;
import com.sme.afs.repository.BlacklistedTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Getter
    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, Set<Role> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles.stream()
                .map(Enum::name)
                .collect(Collectors.toList()));
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String username) {
        if (blacklistedTokenRepository.existsByToken(token)) {
            return false;
        }
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    public String updateTokenWithSession(String token, String sessionId) {
        Claims claims = extractAllClaims(token);
        claims.put("sessionId", sessionId);
        claims.put("sessionCreated", new Date().getTime());
        claims.put("sessionLastAccessed", new Date().getTime());
        return createToken(claims, claims.getSubject());
    }

    public String extractSessionId(String token) {
        return extractClaim(token, claims -> claims.get("sessionId", String.class));
    }

    public Long extractSessionCreationTime(String token) {
        return extractClaim(token, claims -> claims.get("sessionCreated", Long.class));
    }

    public Long extractSessionLastAccessTime(String token) {
        return extractClaim(token, claims -> claims.get("sessionLastAccessed", Long.class));
    }

    public String refreshToken(String token, String sessionId) {
        Claims claims = extractAllClaims(token);
        claims.put("sessionLastAccessed", new Date().getTime());
        claims.put("sessionId", sessionId);
        return createToken(claims, claims.getSubject());
    }

    public void blacklistToken(String token) {
        var blacklistedToken = new com.sme.afs.model.BlacklistedToken();
        blacklistedToken.setToken(token);
        // Create SHA-256 hash of the token
        String tokenHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(token);
        blacklistedToken.setTokenHash(tokenHash);
        blacklistedToken.setBlacklistedAt(java.time.LocalDateTime.now());
        blacklistedToken.setExpiresAt(java.time.LocalDateTime.now().plusSeconds(jwtExpiration / 1000));
        blacklistedToken.setUsername(extractUsername(token));
        blacklistedTokenRepository.save(blacklistedToken);
    }
}
