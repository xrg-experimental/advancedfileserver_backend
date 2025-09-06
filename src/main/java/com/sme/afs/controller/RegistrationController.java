package com.sme.afs.controller;

import com.sme.afs.dto.UserDTO;
import com.sme.afs.model.User;
import com.sme.afs.dto.RegistrationRequest;
import com.sme.afs.service.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registration")
@Slf4j
@RequiredArgsConstructor
public class RegistrationController {
    private final UserRegistrationService registrationService;

    @PostMapping
    public ResponseEntity<UserDTO> registerUser(
        @Valid @RequestBody RegistrationRequest request
    ) {
        User registeredUser = registrationService.registerNewUser(request);
        UserDTO userDTO = convertToDTO(registeredUser);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(userDTO);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setDisplayName(user.getDisplayName());
        dto.setUserType(user.getUserType());
        return dto;
    }
}
