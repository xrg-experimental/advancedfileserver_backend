package com.sme.afs.security;

import com.sme.afs.service.DsmAuthenticationService;
import com.sme.afs.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DsmAuthenticationProviderTest {

    @Mock
    private DsmAuthenticationService dsmAuthenticationService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private SessionService sessionService;

    private DsmAuthenticationProvider dsmAuthenticationProvider;

    @BeforeEach
    void setUp() {
        dsmAuthenticationProvider = new DsmAuthenticationProvider(
            dsmAuthenticationService, 
            userDetailsService, 
            sessionService
        );
    }

    @Test
    void authenticate_WithAdminUser_Success() {
        // Arrange
        String username = "admin";
        String password = "password";
        String otpCode = "123456";
        UserDetails userDetails = new User(username, password, 
            Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")));
        
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(dsmAuthenticationService.authenticate(username, password, otpCode)).thenReturn(true);
        when(sessionService.getUserSessions(anyString())).thenReturn(List.of());
        when(sessionService.getMaxConcurrentSessions()).thenReturn(3);

        Authentication authentication = new OtpAuthenticationToken(username, password, otpCode);

        // Act
        Authentication result = dsmAuthenticationProvider.authenticate(authentication);

        // Assert
        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals(username, result.getName());
        assertTrue(result.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void authenticate_WithNonAdminUser_ThrowsException() {
        // Arrange
        String username = "user";
        String password = "password";
        UserDetails userDetails = new User(username, password, 
            Collections.singleton(new SimpleGrantedAuthority("ROLE_INTERNAL")));
        
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        
        Authentication authentication = new OtpAuthenticationToken(username, password, "123");

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> dsmAuthenticationProvider.authenticate(authentication));
        
        verify(dsmAuthenticationService, never()).authenticate(anyString(), anyString(), anyString());
    }

    @Test
    void authenticate_WithInvalidDsmCredentials_ThrowsException() {
        // Arrange
        String username = "admin";
        String password = "wrong_password";
        String otpCode = "123456";
        UserDetails userDetails = new User(username, password, 
            Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")));
        
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(dsmAuthenticationService.authenticate(username, password, otpCode)).thenReturn(false);
        when(sessionService.getUserSessions(anyString())).thenReturn(List.of());
        when(sessionService.getMaxConcurrentSessions()).thenReturn(3);

        Authentication authentication = new OtpAuthenticationToken(username, password, otpCode);

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> dsmAuthenticationProvider.authenticate(authentication));
    }
}
