package com.sme.afs.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenAnonymousAccessProtectedEndpoint_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/internal/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "INTERNAL")
    void whenInternalUserAccessInternalEndpoint_thenOk() throws Exception {
        mockMvc.perform(get("/api/internal/test"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EXTERNAL")
    void whenExternalUserAccessInternalEndpoint_thenForbidden() throws Exception {
        mockMvc.perform(get("/api/internal/test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void whenAdminAccessAnyEndpoint_thenOk() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isOk());
    }
}
