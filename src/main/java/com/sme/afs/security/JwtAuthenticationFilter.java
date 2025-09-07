package com.sme.afs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sme.afs.dto.ProblemResponse;
import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.session.SessionException;
import com.sme.afs.web.CorrelationIdFilter;
import io.jsonwebtoken.Claims;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.sme.afs.service.SessionService;
import org.springframework.http.MediaType;
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

    private static final String BEARER_PREFIX = "Bearer ";
    private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

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
                respondWithProblemDetails(response, ErrorCode.SESSION_INVALID, "Invalid token format");
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // Validate token first (includes denylist check)
                if (!jwtService.validateToken(jwt, username)) {
                    respondWithProblemDetails(response, ErrorCode.ACCESS_DENIED, "Invalid or expired token");
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
                    // Prefer to use error code if available
                    try {
                        respondWithProblemDetails(response, ex.getErrorCode(), ex.getMessage());
                    } catch (NoSuchMethodError | Exception ignored) {
                        // Fallback to access denied if error code not available at runtime
                        respondWithProblemDetails(response, ErrorCode.ACCESS_DENIED, ex.getMessage());
                    }
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Authentication error occurred", e);
            respondWithProblemDetails(response, ErrorCode.ACCESS_DENIED, "Authentication failed");
        }
    }

    private ProblemResponse createProblem(ErrorCode errorCode, String detail) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID);
        return new ProblemResponse(
                "https://errors.afs/" + errorCode.code,
                errorCode.title,
                errorCode.status.value(),
                detail,
                "urn:uuid:" + correlationId,
                errorCode.code,
                correlationId
        );
    }

    private void respondWithProblemDetails(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        ProblemResponse problemResponse = createProblem(errorCode, message);
        response.setStatus(errorCode.status.value());
        response.setContentType(PROBLEM_JSON.toString());
        response.getWriter().write(objectMapper.writeValueAsString(problemResponse));
    }
}