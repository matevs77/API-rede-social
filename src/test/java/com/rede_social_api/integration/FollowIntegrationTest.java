package com.rede_social_api.integration;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class FollowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicProfileFollowIsAcceptedImmediately() throws Exception {
        AuthResponse follower = TestFixtures.registerUser(mockMvc, objectMapper, "follower");
        AuthResponse target = TestFixtures.registerUser(mockMvc, objectMapper, "target");

        mockMvc.perform(post("/api/users/" + getUserId(target) + "/follow")
                        .header("Authorization", TestFixtures.bearer(follower.accessToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void privateProfileRequiresApproval() throws Exception {
        AuthResponse follower = TestFixtures.registerUser(mockMvc, objectMapper, "priv_follower");
        AuthResponse target = TestFixtures.registerUser(mockMvc, objectMapper, "priv_target");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", TestFixtures.bearer(target.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPrivate\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/" + getUserId(target) + "/follow")
                        .header("Authorization", TestFixtures.bearer(follower.accessToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void cannotFollowSelf() throws Exception {
        AuthResponse user = TestFixtures.registerUser(mockMvc, objectMapper, "self_follow");
        String userId = getUserId(user);

        mockMvc.perform(post("/api/users/" + userId + "/follow")
                        .header("Authorization", TestFixtures.bearer(user.accessToken())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthorizedCannotFollow() throws Exception {
        AuthResponse target = TestFixtures.registerUser(mockMvc, objectMapper, "follow_target");
        mockMvc.perform(post("/api/users/" + getUserId(target) + "/follow"))
                .andExpect(status().isUnauthorized());
    }

    private String getUserId(AuthResponse auth) throws Exception {
        String body = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", TestFixtures.bearer(auth.accessToken())))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("id").asText();
    }
}
