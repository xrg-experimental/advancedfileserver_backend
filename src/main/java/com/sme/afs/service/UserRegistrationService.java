package com.sme.afs.service;

import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.Role;
import com.sme.afs.model.User;
import com.sme.afs.model.UserType;
import com.sme.afs.repository.UserRepository;
import com.sme.afs.dto.RegistrationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserRegistrationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerNewUser(RegistrationRequest request) {
        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AfsException(ErrorCode.VALIDATION_FAILED, "Username already exists");
        }

        // Create new user with PENDING status
        User newUser = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .displayName(request.getDisplayName() != null ? 
                request.getDisplayName() : request.getUsername())
            .userType(UserType.EXTERNAL)
            .enabled(false)  // Requires admin approval
            .createdAt(LocalDateTime.now())
            .roles(Set.of(Role.valueOf("ROLE_EXTERNAL")))
            .build();

        return userRepository.save(newUser);
    }
}
