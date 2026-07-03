package com.rede_social_api.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rede_social_api.auth.dto.request.LoginRequest;
import com.rede_social_api.auth.dto.request.RegisterRequest;
import com.rede_social_api.auth.dto.response.AuthResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestFixtures {

    private TestFixtures() {}

    public static AuthResponse registerUser(MockMvc mockMvc, ObjectMapper objectMapper, String suffix) throws Exception {
        RegisterRequest request = new RegisterRequest(
                "user_" + suffix,
                suffix + "@test.com",
                "password123",
                "User " + suffix
        );
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    public static AuthResponse loginUser(MockMvc mockMvc, ObjectMapper objectMapper, String usernameOrEmail) throws Exception {
        LoginRequest request = new LoginRequest(usernameOrEmail, "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
