package com.rede_social_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rede_social_api.auth.dto.request.RefreshTokenRequest;
import com.rede_social_api.auth.dto.response.AuthResponse;
import com.rede_social_api.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RefreshTokenRevocationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void revokedRefreshTokenShouldBeRejected() throws Exception {
        AuthResponse auth = TestFixtures.registerUser(mockMvc, objectMapper, "revoke");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", TestFixtures.bearer(auth.accessToken())))
                .andExpect(status().isNoContent());

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(auth.refreshToken());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }
}
