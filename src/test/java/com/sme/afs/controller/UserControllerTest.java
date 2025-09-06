package com.sme.afs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.afs.dto.CreateUserRequest;
import com.sme.afs.dto.UserDTO;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.Role;
import com.sme.afs.model.User;
import com.sme.afs.model.UserType;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import({
    SecurityConfig.class, 
    JwtAuthenticationFilter.class, 
    JwtService.class,
    LocalAuthenticationProvider.class
})
class UserControllerTest {

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private DsmAuthenticationProvider dsmAuthenticationProvider;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private SecurityConfig securityConfig;

    @MockBean
    private LocalAuthenticationProvider localAuthenticationProvider;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private UserDTO testUserDto;
    private CreateUserRequest createUserRequest;

    @BeforeEach
    void setUp() {
        // Setup test UserDTO
        testUserDto = new UserDTO();
        testUserDto.setId(1L);
        testUserDto.setUsername("testuser");
        testUserDto.setEmail("test@example.com");
        testUserDto.setEnabled(true);
        testUserDto.setUserType(UserType.INTERNAL);
        testUserDto.setRoles(Set.of("ROLE_INTERNAL"));
        testUserDto.setGroups(Set.of("TestGroup"));
        testUserDto.setCreatedAt(LocalDateTime.now());
        testUserDto.setLastLogin(LocalDateTime.now());

        // Setup CreateUserRequest
        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("newuser");
        createUserRequest.setPassword("password123");
        createUserRequest.setEmail("newuser@example.com");
        createUserRequest.setUserType(UserType.INTERNAL);
        createUserRequest.setEnabled(true);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_ShouldReturnUsersList() throws Exception {
        List<User> users = Arrays.asList(
            createTestUser("user1"),
            createTestUser("user2")
        );
        
        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].username").value("user1"))
            .andExpect(jsonPath("$[1].username").value("user2"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_ShouldReturnCreatedUser() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(testUserDto);

        mockMvc.perform(post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value(testUserDto.getUsername()))
            .andExpect(jsonPath("$.email").value(testUserDto.getEmail()))
            .andExpect(jsonPath("$.userType").value(testUserDto.getUserType().name()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_WithExistingUsername_ShouldReturnConflict() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class)))
            .thenThrow(new AfsException(HttpStatus.CONFLICT, "Username already exists"));

        mockMvc.perform(post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "INTERNAL")
    void whenNonAdminAccessAdminEndpoint_thenForbidden() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isForbidden());
    }

    private User createTestUser(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setEnabled(true);
        user.setUserType(UserType.INTERNAL);
        user.setRoles(new HashSet<>(Set.of(Role.ROLE_INTERNAL)));
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
