package com.sme.afs.controller;

import com.sme.afs.dto.CreateUserRequest;
import com.sme.afs.dto.UpdateUserRequest;
import com.sme.afs.dto.UserDTO;
import com.sme.afs.security.annotation.IsAdmin;
import com.sme.afs.service.UserService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    @IsAdmin
    @Operation(summary = "List all users", description = "Retrieve a list of all users in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
        @ApiResponse(responseCode = "403", description = "Not authorized to view users")
    })
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers().stream()
            .map(UserDTO::fromUser)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @IsAdmin
    @Operation(summary = "Create new user", description = "Create a new user account in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Not authorized to create users"),
        @ApiResponse(responseCode = "409", description = "Username already exists")
    })
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDTO createdUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{username}")
    @IsAdmin
    @Operation(summary = "Update user", description = "Partially update user information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data or non-existent groups/roles"),
        @ApiResponse(responseCode = "403", description = "Not authorized to update users"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserDTO> updateUser(
        @PathVariable String username, 
        @Valid @RequestBody UpdateUserRequest request
    ) {
        UserDTO updatedUser = userService.updateUser(username, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{username}/status")
    @IsAdmin
    @Operation(summary = "Enable/disable user", description = "Toggle user account status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User status updated successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to modify user status"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserDTO> updateUserStatus(
        @PathVariable String username,
        @RequestParam boolean enabled
    ) {
        UserDTO updatedUser = userService.updateUserStatus(username, enabled);
        return ResponseEntity.ok(updatedUser);
    }
}
