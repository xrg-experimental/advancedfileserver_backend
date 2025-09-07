package com.sme.afs.service;

import com.sme.afs.dto.*;
import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.Group;
import com.sme.afs.model.Role;
import com.sme.afs.model.User;
import com.sme.afs.repository.GroupRepository;
import com.sme.afs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsersPaginated(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AfsException(ErrorCode.VALIDATION_FAILED, "Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setUserType(request.getUserType());
        user.setEnabled(request.isEnabled());

        // Set default role based on a user type if no roles provided
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            user.getRoles().add(Role.valueOf("ROLE_" + request.getUserType().name()));
        } else {
            user.setRoles(request.getRoles().stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet()));
        }

        // Add groups if specified
        if (request.getGroups() != null && !request.getGroups().isEmpty()) {
            Set<Group> groups = groupRepository.findAllByNameIn(request.getGroups());
            user.setGroups(groups);
        }

        User savedUser = userRepository.save(user);
        return UserDTO.fromUser(savedUser);
    }

    @Transactional
    public UserDTO updateUserStatus(String username, boolean enabled) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AfsException(ErrorCode.NOT_FOUND, 
                String.format("User not found: %s", username)));

        // Don't allow disabling admin users
        if (!enabled && user.getRoles().contains(Role.ROLE_ADMIN)) {
            throw new AfsException(ErrorCode.VALIDATION_FAILED,
                "Cannot disable administrator account");
        }

        user.setEnabled(enabled);
        
        // If disabling user, invalidate all their active sessions
        if (!enabled) {
            sessionService.invalidateUserSessions(username);
        }

        User updatedUser = userRepository.save(user);
        return UserDTO.fromUser(updatedUser);
    }

    @Transactional
    public UserDTO updateUser(String username, UpdateUserRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AfsException(ErrorCode.NOT_FOUND, "User not found"));

        // Update email if provided and valid
        if (request.getEmail() != null) {
            // Optional: Add email validation if needed
            user.setEmail(request.getEmail());
        }

        // Update user type if provided
        if (request.getUserType() != null) {
            user.setUserType(request.getUserType());
        }

        // Update roles if provided
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            // Validate roles exist
            Set<Role> validRoles = request.getRoles().stream()
                .map(roleName -> {
                    try {
                        return Role.valueOf(roleName);
                    } catch (IllegalArgumentException e) {
                        throw new AfsException(ErrorCode.VALIDATION_FAILED, "Invalid role: " + roleName);
                    }
                })
                .collect(Collectors.toSet());
            
            user.setRoles(validRoles);
        }

        // Update groups if provided
        if (request.getGroups() != null && !request.getGroups().isEmpty()) {
            Set<Group> groups = groupRepository.findAllByNameIn(request.getGroups());
            
            // Validate all requested groups exist
            if (groups.size() != request.getGroups().size()) {
                throw new AfsException(ErrorCode.VALIDATION_FAILED, "One or more groups do not exist");
            }
            
            user.setGroups(groups);
        }

        // Update enabled status if provided
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        User updatedUser = userRepository.save(user);
        return UserDTO.fromUser(updatedUser);
    }

    @Transactional(readOnly = true)
    public ProfileDTO getCurrentUserProfile(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AfsException(ErrorCode.NOT_FOUND, "User not found"));
        
        ProfileDTO profile = new ProfileDTO();
        profile.setUsername(user.getUsername());
        profile.setEmail(user.getEmail());
        profile.setDisplayName(user.getDisplayName());
        return profile;
    }

    @Transactional
    public ProfileDTO updateCurrentUserProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AfsException(ErrorCode.NOT_FOUND, "User not found"));

        // Update email if provided
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            user.setEmail(request.getEmail());
        }

        // Update display name if provided
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }

        // Update password if provided
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        User updatedUser = userRepository.save(user);
        
        ProfileDTO profile = new ProfileDTO();
        profile.setUsername(updatedUser.getUsername());
        profile.setEmail(updatedUser.getEmail());
        profile.setDisplayName(updatedUser.getDisplayName());
        return profile;
    }
}
