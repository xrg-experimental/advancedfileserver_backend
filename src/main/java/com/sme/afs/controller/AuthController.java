package com.sme.afs.controller;

import com.sme.afs.dto.LoginRequest;
import com.sme.afs.dto.LoginResponse;
import com.sme.afs.dto.OtpLoginRequest;
import com.sme.afs.model.Role;
import com.sme.afs.model.UserSession;
import com.sme.afs.security.DsmAuthenticationProvider;
import com.sme.afs.security.JwtService;
import com.sme.afs.security.LocalAuthenticationProvider;
import com.sme.afs.security.OtpAuthenticationToken;
import com.sme.afs.service.OtpService;
import com.sme.afs.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.jsonwebtoken.Claims;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Authentication", description = "Authentication management APIs")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final DsmAuthenticationProvider dsmAuthenticationProvider;
    private final LocalAuthenticationProvider localAuthenticationProvider;
    private final OtpService otpService;

    @Operation(summary = "Login user", description = "Authenticates user and returns JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "201", description = "Authentication postponed (OTP required)",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content()),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content())
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        log.debug("Login attempt received for user: {}", loginRequest.getUsername());
        log.debug("Request path: /auth/login");

        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            log.warn("Login attempt with invalid request data");
            return ResponseEntity.badRequest().build();
        }

        String username = loginRequest.getUsername();
        try {
            // Check if OTP is required before authentication
            if (otpService.isOtpRequired(username)) {
                return localAuthenticationProvider.createNeedsOtpLoginResponse(username);
            }

            Authentication authentication = localAuthenticationProvider.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );
            return createLoginResponse(username, authentication);
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", username, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "OTP login", description = "Authenticates user with OTP code and returns JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content()),
            @ApiResponse(responseCode = "401", description = "Invalid authentication", content = @Content())
    })
    @PostMapping("/otp-login")
    public ResponseEntity<LoginResponse> otpLogin(@RequestBody OtpLoginRequest loginRequest) {
        log.debug("OTP login attempt received for user: {}", loginRequest.getUsername());

        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null
                || loginRequest.getOtpCode() == null) {
            log.warn("OTP-Login attempt with invalid request data");
            return ResponseEntity.badRequest().build();
        }

        try {
            // Determine if this is an admin user
            boolean isAdmin = otpService.isAdminUser(loginRequest.getUsername());

            // Use appropriate authentication provider
            Authentication authentication = isAdmin ?
                    dsmAuthenticationProvider.authenticate(
                            new OtpAuthenticationToken(
                                    loginRequest.getUsername(),
                                    loginRequest.getPassword(),
                                    loginRequest.getOtpCode()
                            )
                    ) :
                    localAuthenticationProvider.authenticate(
                            new OtpAuthenticationToken(
                                    loginRequest.getUsername(),
                                    loginRequest.getPassword(),
                                    loginRequest.getOtpCode()
                            )
                    );

            return createLoginResponse(loginRequest.getUsername(), authentication);
        } catch (Exception e) {
            log.error("OTP authentication failed for user: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "Refresh token", description = "Refreshes an existing valid JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token successfully refreshed"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            String sessionId = jwtService.extractSessionId(token);
            
            if (username == null || sessionId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Validate session
            if (!sessionService.validateSession(sessionId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Refresh token
            String newToken = jwtService.refreshToken(token, sessionId);
            if (newToken == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Calculate refresh window
            long refreshWindowStart = System.currentTimeMillis() + 
                (jwtService.getJwtExpiration() - sessionService.getRefreshWindow() * 1000L);
            long refreshWindowEnd = System.currentTimeMillis() + jwtService.getJwtExpiration();

            // Extract user type from token
            Claims claims = jwtService.extractAllClaims(newToken);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            String userType = roles.stream()
                    .filter(role -> role.startsWith("ROLE_"))
                    .map(role -> role.substring(5))
                    .findFirst()
                    .orElse("GUEST");

            return ResponseEntity.ok(new LoginResponse(
                newToken,
                username,
                userType,
                refreshWindowStart,
                refreshWindowEnd,
                    false
            ));
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "Logout user", description = "Invalidates the current session and blacklists the token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged out"),
            @ApiResponse(responseCode = "401", description = "Invalid token"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String token = authHeader.substring(7);
            String username = jwtService.extractUsername(token);
            String sessionId = jwtService.extractSessionId(token);

            if (username == null || sessionId == null || !jwtService.validateToken(token, username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Invalidate session and blacklist token
            sessionService.invalidateSession(sessionId);
            jwtService.blacklistToken(token);
            log.debug("Session invalidated and token blacklisted on logout: {}", sessionId);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get user sessions", description = "Retrieves all sessions for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved sessions"),
        @ApiResponse(responseCode = "403", description = "Not authorized to view sessions"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/sessions/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSession>> getUserSessions(@PathVariable String username) {
        return ResponseEntity.ok(sessionService.getUserSessions(username));
    }

    @Operation(summary = "Revoke specific session", description = "Invalidates a specific session for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully revoked session"),
            @ApiResponse(responseCode = "403", description = "Not authorized to revoke session"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @PostMapping("/sessions/{username}/{sessionId}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revokeUserSession(
            @PathVariable String username,
            @PathVariable String sessionId) {
        sessionService.invalidateSession(sessionId);
        log.debug("Admin revoked session {} for user: {}", sessionId, username);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Revoke all user sessions", description = "Invalidates all active sessions for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully revoked all sessions"),
        @ApiResponse(responseCode = "403", description = "Not authorized to revoke sessions"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/sessions/{username}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revokeUserSessions(@PathVariable String username) {
        sessionService.invalidateUserSessions(username);
        log.debug("Admin revoked all sessions for user: {}", username);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<LoginResponse> createLoginResponse(String username,
                                                              @NonNull Authentication authentication) {
        var userDetails = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        var roles = userDetails.getAuthorities().stream()
                .map(auth -> Role.valueOf(auth.getAuthority()))
                .collect(java.util.stream.Collectors.toSet());

        // Generate initial token and create session
        String token = jwtService.generateToken(username, roles);
        if (token == null) {
            log.error("Token generation failed for user: {}", username);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Create a new session
        UserSession session = sessionService.createSession(username, token);
        if (session == null) {
            log.error("Session creation failed for user: {}", username);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Update token with session ID
        token = jwtService.updateTokenWithSession(token, session.getSessionId());
        if (token == null) {
            log.error("Token update with session failed for user: {}", username);
            sessionService.invalidateSession(session.getSessionId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Determine user type from roles
        String userType = roles.stream()
                .map(Role::name)
                .filter(role -> role.startsWith("ROLE_"))
                .map(role -> role.substring(5)) // Remove "ROLE_" prefix
                .findFirst()
                .orElse("GUEST");

        // Calculate refresh window
        long refreshWindowStart = System.currentTimeMillis() +
                (jwtService.getJwtExpiration() - sessionService.getRefreshWindow() * 1000L);
        long refreshWindowEnd = System.currentTimeMillis() + jwtService.getJwtExpiration();

        return ResponseEntity.ok(new LoginResponse(
                token,
                username,
                userType,
                refreshWindowStart,
                refreshWindowEnd,
                false
        ));
    }
}
