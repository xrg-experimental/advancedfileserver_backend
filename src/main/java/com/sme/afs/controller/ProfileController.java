package com.sme.afs.controller;

import com.sme.afs.dto.ProfileDTO;
import com.sme.afs.dto.UpdateProfileRequest;
import com.sme.afs.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;


@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;

    @Autowired
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileDTO> getCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ProfileDTO profile = userService.getCurrentUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<ProfileDTO> updateCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest updateRequest) {
        ProfileDTO updatedProfile = userService.updateCurrentUserProfile(
            userDetails.getUsername(), updateRequest);
        return ResponseEntity.ok(updatedProfile);
    }
}
