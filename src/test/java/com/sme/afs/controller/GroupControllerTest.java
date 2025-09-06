package com.sme.afs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.afs.dto.CreateGroupRequest;
import com.sme.afs.security.*;
import com.sme.afs.service.GroupService;
import com.sme.afs.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GroupController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class GroupControllerTest {

    @MockBean
    private LocalAuthenticationProvider localAuthenticationProvider;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private DsmAuthenticationProvider dsmAuthenticationProvider;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private SecurityConfig securityConfig;

    @MockBean
    private GroupService groupService;

    private CreateGroupRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreateGroupRequest();
        validRequest.setName("test-group");
        validRequest.setDescription("Test Group Description");
        validRequest.setBasePath("/shared/test-group");
    }

    @Test
    @WithMockUser(roles = "USER")
    void createGroup_NonAdminUser() throws Exception {
        mockMvc.perform(post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isForbidden());
    }
}
