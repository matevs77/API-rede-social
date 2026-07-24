package com.rede_social_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rede_social_api.auth.dto.response.AuthResponse;
import com.rede_social_api.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthUserIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerAndLogin() throws Exception {
        AuthResponse auth = TestFixtures.registerUser(mockMvc, objectMapper, "auth1");
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", TestFixtures.bearer(auth.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user_auth1"));

        TestFixtures.loginUser(mockMvc, objectMapper, "user_auth1");
    }

    @Test
    void publicProfileVisibleWithoutAuth() throws Exception {
        TestFixtures.registerUser(mockMvc, objectMapper, "public_profile");
        mockMvc.perform(get("/api/users/user_public_profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user_public_profile"));
    }

    @Test
    void updateProfileRequiresAuth() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Hacker\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchUsersRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/users/search").param("q", "user"))
                .andExpect(status().isUnauthorized());
    }
}
