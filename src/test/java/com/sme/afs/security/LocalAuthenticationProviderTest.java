package com.sme.afs.security;

import com.sme.afs.service.OtpService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalAuthenticationProviderTest {

    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private SessionService sessionService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OtpService otpService;

    private LocalAuthenticationProvider localAuthenticationProvider;

    @BeforeEach
    void setUp() {
        localAuthenticationProvider = new LocalAuthenticationProvider(
                userDetailsService,
                sessionService,
                passwordEncoder,
                otpService
        );
    }

    @Test
    void authenticate_WithNonAdminUser_Success() {
        // Arrange
        String username = "user";
        String password = "password";
        UserDetails userDetails = new User(username, password, 
            Collections.singleton(new SimpleGrantedAuthority("ROLE_INTERNAL")));
        
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(passwordEncoder.matches(password, password)).thenReturn(true);
        when(sessionService.getUserSessions(anyString())).thenReturn(List.of());
        when(sessionService.getMaxConcurrentSessions()).thenReturn(3);

        Authentication authentication = new UsernamePasswordAuthenticationToken(username, password);

        // Act
        Authentication result = localAuthenticationProvider.authenticate(authentication);

        // Assert
        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals(username, result.getName());
        assertTrue(result.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_INTERNAL")));
    }

    @Test
    void authenticate_WithInvalidPassword_ThrowsException() {
        // Arrange
        String username = "user";
        String password = "wrong_password";
        UserDetails userDetails = new User(username, password,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_INTERNAL")));

        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        Authentication authentication = new UsernamePasswordAuthenticationToken(username, password);

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> localAuthenticationProvider.authenticate(authentication));
    }
}
