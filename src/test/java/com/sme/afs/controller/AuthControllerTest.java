package com.sme.afs.controller;

import com.sme.afs.dto.LoginRequest;
import com.sme.afs.model.UserSession;
import com.sme.afs.security.JwtService;
import com.sme.afs.service.DsmAuthenticationService;
import com.sme.afs.service.SessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @MockBean
    private DsmAuthenticationService dsmAuthenticationService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private SessionService sessionService;

    private LoginRequest validLoginRequest;
    private static final String TEST_TOKEN = "test.jwt.token";

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("testuser");
        validLoginRequest.setPassword("password123");

        User userDetails = new User(
            "testuser",
            "password123",
            Collections.singleton(new SimpleGrantedAuthority("ROLE_INTERNAL"))
        );

        when(dsmAuthenticationService.authenticate(anyString(), anyString(), anyString())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
    }

    @Test
    void loginFailure() throws Exception {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When/Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginInvalidRequest() throws Exception {
        // Given
        LoginRequest invalidRequest = new LoginRequest();
        // username and password are null

        // When/Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        // Given
        String token = TEST_TOKEN;
        String sessionId = "test-session-id";
        
        when(jwtService.validateToken(token, "testuser")).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn("testuser");
        when(jwtService.extractSessionId(token)).thenReturn(sessionId);
        when(sessionService.validateSession(sessionId)).thenReturn(true);
        when(jwtService.refreshToken(token, sessionId)).thenReturn(token);
        when(jwtService.extractAllClaims(token))
            .thenReturn(Jwts.claims(Map.of("roles", Collections.singletonList("ROLE_INTERNAL"))));
        when(jwtService.getJwtExpiration()).thenReturn(86400000L);
        when(sessionService.getRefreshWindow()).thenReturn(300);

        // When/Then
        mockMvc.perform(post("/auth/refresh")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.userType").value("INTERNAL"));
    }

    @Test
    void shouldLogoutSuccessfully() throws Exception {
        // Given
        String token = TEST_TOKEN;
        String sessionId = "test-session-id";
        String username = "testuser";
        
        Claims claims = Jwts.claims();
        claims.put("roles", Collections.singletonList("ROLE_INTERNAL"));
        
        when(jwtService.extractSessionId(token)).thenReturn(sessionId);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.validateToken(token, username)).thenReturn(true);
        when(jwtService.extractAllClaims(token)).thenReturn(claims);
        when(sessionService.validateSession(sessionId)).thenReturn(true);
        doNothing().when(sessionService).invalidateSession(sessionId);

        // When/Then
        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
        
        verify(sessionService).invalidateSession(sessionId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetUserSessions() throws Exception {
        // Given
        String username = "testuser";
        UserSession session = new UserSession();
        session.setSessionId("test-session");
        session.setUsername(username);
        
        when(sessionService.getUserSessions(username))
            .thenReturn(Collections.singletonList(session));

        // When/Then
        mockMvc.perform(get("/auth/sessions/{username}", username))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sessionId").value("test-session"))
            .andExpect(jsonPath("$[0].username").value(username));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRevokeUserSessions() throws Exception {
        // Given
        String username = "testuser";
        doNothing().when(sessionService).invalidateUserSessions(username);

        // When/Then
        mockMvc.perform(post("/auth/sessions/{username}/revoke", username))
            .andExpect(status().isOk());
        
        verify(sessionService).invalidateUserSessions(username);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRevokeSingleSession() throws Exception {
        // Given
        String username = "testuser";
        String sessionId = "test-session";
        doNothing().when(sessionService).invalidateSession(sessionId);

        // When/Then
        mockMvc.perform(post("/auth/sessions/{username}/{sessionId}/revoke", 
                username, sessionId))
            .andExpect(status().isOk());
        
        verify(sessionService).invalidateSession(sessionId);
    }
}
