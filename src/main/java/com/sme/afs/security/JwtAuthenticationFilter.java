package com.sme.afs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sme.afs.dto.ErrorResponse;
import com.sme.afs.exception.session.SessionException;
import io.jsonwebtoken.Claims;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.sme.afs.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SessionService sessionService;
    private final ObjectMapper objectMapper;
    private static final String BEARER_PREFIX = "Bearer ";

    // Constructor for dependency injection
    public JwtAuthenticationFilter(JwtService jwtService, SessionService sessionService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            final String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }

            final String jwt = authHeader.substring(BEARER_PREFIX.length());
            final String username = jwtService.extractUsername(jwt);
            final String sessionId = jwtService.extractSessionId(jwt);

            if (username == null || sessionId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token format");
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // Validate token first (includes blacklist check)
                if (!jwtService.validateToken(jwt, username)) {
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                    return;
                }

                try {
                    // Then validate session
                    sessionService.validateSession(sessionId);

                    // Update session last accessed time
                    sessionService.updateLastAccessed(sessionId);

                    Claims claims = jwtService.extractAllClaims(jwt);
                    @SuppressWarnings("unchecked")
                    List<String> roles = claims.get("roles", List.class);
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } catch (SessionException ex) {
                    sendErrorResponse(response, ex.getStatus().value(), ex.getMessage());
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Authentication error occurred", e);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(
                status,
                HttpStatus.valueOf(status).getReasonPhrase(),
                message,
                "N/A"  // Filter doesn't have access to the request URI
        );
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}