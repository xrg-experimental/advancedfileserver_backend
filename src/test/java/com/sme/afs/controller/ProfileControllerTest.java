package com.sme.afs.controller;

import com.sme.afs.dto.ProfileDTO;
import com.sme.afs.repository.BlacklistedTokenRepository;
import com.sme.afs.security.*;
import com.sme.afs.service.SessionService;
import com.sme.afs.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProfileController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private DsmAuthenticationProvider dsmAuthenticationProvider;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private SecurityConfig securityConfig;

    @MockBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    private ProfileDTO testProfile;

    @BeforeEach
    void setUp() {
        testProfile = new ProfileDTO();
        testProfile.setUsername("testuser");
        testProfile.setEmail("test@example.com");
        testProfile.setDisplayName("Test User");

        // Add mock behavior for authentication
        when(dsmAuthenticationProvider.authenticate(any()))
            .thenThrow(new BadCredentialsException("Only admin users can authenticate through DSM"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getCurrentUserProfile_ShouldReturnProfile() throws Exception {
        when(userService.getCurrentUserProfile("testuser")).thenReturn(testProfile);

        mockMvc.perform(get("/api/profile"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.displayName").value("Test User"));
    }
}
