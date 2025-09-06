package com.sme.afs.service;

import com.sme.afs.dto.RegistrationRequest;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.Role;
import com.sme.afs.model.User;
import com.sme.afs.model.UserType;
import com.sme.afs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserRegistrationService userRegistrationService;

    @BeforeEach
    void setUp() {
        userRegistrationService = new UserRegistrationService(userRepository, passwordEncoder);
    }

    @Test
    void registerNewUser_Successful_ShouldCreateExternalUser() {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setDisplayName("New User");

        // Mock repository to return empty optional (username available)
        when(userRepository.findByUsername(request.getUsername()))
            .thenReturn(Optional.empty());

        // Mock password encoding
        when(passwordEncoder.encode(request.getPassword()))
            .thenReturn("encodedPassword");

        // Mock repository save
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User registeredUser = userRegistrationService.registerNewUser(request);

        // Assert
        assertNotNull(registeredUser);
        assertEquals(request.getUsername(), registeredUser.getUsername());
        assertEquals(request.getEmail(), registeredUser.getEmail());
        assertEquals(request.getDisplayName(), registeredUser.getDisplayName());
        assertEquals(UserType.EXTERNAL, registeredUser.getUserType());
        assertFalse(registeredUser.isEnabled());
        assertTrue(registeredUser.getRoles().contains(Role.ROLE_EXTERNAL));

        // Verify interactions
        verify(userRepository).findByUsername(request.getUsername());
        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerNewUser_UsernameAlreadyExists_ShouldThrowException() {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("existinguser");
        request.setEmail("existinguser@example.com");
        request.setPassword("password123");

        // Mock repository to return existing user
        when(userRepository.findByUsername(request.getUsername()))
            .thenReturn(Optional.of(new User()));

        // Act & Assert
        AfsException exception = assertThrows(AfsException.class, () -> userRegistrationService.registerNewUser(request));

        assertEquals("Username already exists", exception.getMessage());

        // Verify interactions
        verify(userRepository).findByUsername(request.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerNewUser_WithoutDisplayName_ShouldUseUsername() {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        // No display name set

        // Mock repository to return empty optional (username available)
        when(userRepository.findByUsername(request.getUsername()))
            .thenReturn(Optional.empty());

        // Mock password encoding
        when(passwordEncoder.encode(request.getPassword()))
            .thenReturn("encodedPassword");

        // Mock repository save
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User registeredUser = userRegistrationService.registerNewUser(request);

        // Assert
        assertEquals(request.getUsername(), registeredUser.getDisplayName());
    }
}
