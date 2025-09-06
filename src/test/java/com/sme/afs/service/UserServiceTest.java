package com.sme.afs.service;

import com.sme.afs.dto.ProfileDTO;
import com.sme.afs.dto.UpdateProfileRequest;
import com.sme.afs.model.User;
import com.sme.afs.repository.UserRepository;
import com.sme.afs.util.TestDataUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sme.afs.dto.UpdateUserRequest;
import com.sme.afs.dto.UserDTO;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.Group;
import com.sme.afs.model.Role;
import com.sme.afs.model.UserType;
import com.sme.afs.repository.GroupRepository;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private UserService userService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldGetAllUsers() {
        // Given
        User user1 = TestDataUtil.createTestUser("user1");
        User user2 = TestDataUtil.createTestUser("user2");
        List<User> expectedUsers = Arrays.asList(user1, user2);

        when(userRepository.findAll()).thenReturn(expectedUsers);

        // When
        List<User> actualUsers = userService.getAllUsers();

        // Then
        assertThat(actualUsers).hasSize(2);
        assertThat(actualUsers).containsExactlyElementsOf(expectedUsers);
    }

    @Test
    void updateUser_WithValidEmail_ShouldUpdateSuccessfully() {
        // Given
        User existingUser = TestDataUtil.createTestUser("testuser");
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setEmail("updated@example.com");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserDTO updatedUser = userService.updateUser("testuser", updateRequest);

        // Then
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateUser_WithNonExistentUser_ShouldThrowNotFoundException() {
        // Given
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setEmail("updated@example.com");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.updateUser("testuser", updateRequest))
            .isInstanceOf(AfsException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void updateUser_WithUserType_ShouldUpdateUserType() {
        // Given
        User existingUser = TestDataUtil.createTestUser("testuser");
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setUserType(UserType.EXTERNAL);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserDTO updatedUser = userService.updateUser("testuser", updateRequest);

        // Then
        assertThat(updatedUser.getUserType()).isEqualTo(UserType.EXTERNAL);
    }

    @Test
    void updateUser_WithRoles_ShouldUpdateRoles() {
        // Given
        User existingUser = TestDataUtil.createTestUser("testuser");
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setRoles(Set.of("ROLE_EXTERNAL"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserDTO updatedUser = userService.updateUser("testuser", updateRequest);

        // Then
        assertThat(updatedUser.getRoles()).containsExactly("ROLE_EXTERNAL");
    }

    @Test
    void updateUser_WithInvalidRoles_ShouldThrowException() {
        // Given
        User existingUser = TestDataUtil.createTestUser("testuser");
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setRoles(Set.of("INVALID_ROLE"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));

        // When/Then
        assertThatThrownBy(() -> userService.updateUser("testuser", updateRequest))
            .isInstanceOf(AfsException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateUser_WithGroups_ShouldUpdateGroups() {
        // Given
        User existingUser = TestDataUtil.createTestUser("testuser");
        Group group = new Group();
        group.setName("NewGroup");
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setGroups(Set.of("NewGroup"));
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(groupRepository.findAllByNameIn(Set.of("NewGroup"))).thenReturn(Set.of(group));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserDTO updatedUser = userService.updateUser("testuser", updateRequest);

        // Then
        assertThat(updatedUser.getGroups()).containsExactly("NewGroup");
    }

    @Test
    void updateUser_WithNonExistentGroups_ShouldThrowException() {
        // Given
        User existingUser = TestDataUtil.createTestUser("testuser");
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setGroups(Set.of("NonExistentGroup"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(groupRepository.findAllByNameIn(Set.of("NonExistentGroup"))).thenReturn(Set.of());

        // When/Then
        assertThatThrownBy(() -> userService.updateUser("testuser", updateRequest))
            .isInstanceOf(AfsException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateUser_WithEnabledStatus_ShouldUpdateEnabledStatus() {
        // Given
        User existingUser = TestDataUtil.createTestUser("testuser");
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setEnabled(false);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserDTO updatedUser = userService.updateUser("testuser", updateRequest);

        // Then
        assertThat(updatedUser.isEnabled()).isFalse();
    }

    @Test
    void updateUserStatus_ShouldDisableUser() {
        // Given
        User existingUser = TestDataUtil.createTestUser("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(sessionService).invalidateUserSessions(anyString());

        // When
        UserDTO updatedUser = userService.updateUserStatus("testuser", false);

        // Then
        assertThat(updatedUser.isEnabled()).isFalse();
        verify(sessionService).invalidateUserSessions("testuser");
    }

    @Test
    void updateUserStatus_WithAdminUser_ShouldNotAllowDisabling() {
        // Given
        User adminUser = TestDataUtil.createTestUser("admin");
        adminUser.setRoles(Set.of(Role.ROLE_ADMIN));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // When/Then
        assertThatThrownBy(() -> userService.updateUserStatus("admin", false))
            .isInstanceOf(AfsException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateUserStatus_WithNonExistentUser_ShouldThrowNotFoundException() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.updateUserStatus("nonexistent", false))
            .isInstanceOf(AfsException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void updateUser_WithPartialUpdate_ShouldPreserveExistingValues() {
        // Given
        User existingUser = TestDataUtil.createTestUser("testuser");
        existingUser.setUserType(UserType.EXTERNAL);
        existingUser.setRoles(Set.of(Role.ROLE_EXTERNAL));
        
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setEmail("updated@example.com");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserDTO updatedUser = userService.updateUser("testuser", updateRequest);

        // Then
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getUserType()).isEqualTo(UserType.EXTERNAL);
        assertThat(updatedUser.getRoles()).containsExactly("ROLE_EXTERNAL");
    }

    @Test
    void shouldGetAllUsersPaginated() {
        // Given
        User user1 = TestDataUtil.createTestUser("user1");
        User user2 = TestDataUtil.createTestUser("user2");
        List<User> expectedUsers = Arrays.asList(user1, user2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> expectedPage = new PageImpl<>(expectedUsers, pageable, expectedUsers.size());

        when(userRepository.findAll(pageable)).thenReturn(expectedPage);

        // When
        Page<User> actualPage = userService.getAllUsersPaginated(pageable);

        // Then
        assertThat(actualPage.getContent()).hasSize(2);
        assertThat(actualPage.getContent()).containsExactlyElementsOf(expectedUsers);
    }
    @Test
    void getCurrentUserProfile_ShouldReturnProfileDTO() {
        // Given
        User user = TestDataUtil.createTestUser("testuser");
        user.setDisplayName("Test User");
        user.setEmail("test@example.com");
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // When
        ProfileDTO profile = userService.getCurrentUserProfile("testuser");

        // Then
        assertThat(profile.getUsername()).isEqualTo("testuser");
        assertThat(profile.getEmail()).isEqualTo("test@example.com");
        assertThat(profile.getDisplayName()).isEqualTo("Test User");
    }

    @Test
    void getCurrentUserProfile_WithNonExistentUser_ShouldThrowNotFoundException() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.getCurrentUserProfile("nonexistent"))
            .isInstanceOf(AfsException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void updateCurrentUserProfile_ShouldUpdateAndReturnProfile() {
        // Given
        User existingUser = TestDataUtil.createTestUser("testuser");
        existingUser.setDisplayName("Original Name");
        existingUser.setEmail("original@example.com");

        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setDisplayName("Updated Name");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setNewPassword("newPassword123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProfileDTO updatedProfile = userService.updateCurrentUserProfile("testuser", updateRequest);

        // Then
        assertThat(updatedProfile.getUsername()).isEqualTo("testuser");
        assertThat(updatedProfile.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedProfile.getDisplayName()).isEqualTo("Updated Name");
        assertThat(existingUser.getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    void updateCurrentUserProfile_WithoutPasswordChange_ShouldNotUpdatePassword() {
        // Given
        User existingUser = TestDataUtil.createTestUser("testuser");
        String originalPassword = existingUser.getPassword();

        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setDisplayName("Updated Name");
        updateRequest.setEmail("updated@example.com");
        // No new password provided

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProfileDTO updatedProfile = userService.updateCurrentUserProfile("testuser", updateRequest);

        // Then
        assertThat(existingUser.getPassword()).isEqualTo(originalPassword);
        assertThat(updatedProfile.getDisplayName()).isEqualTo("Updated Name");
        assertThat(updatedProfile.getEmail()).isEqualTo("updated@example.com");
    }
}
