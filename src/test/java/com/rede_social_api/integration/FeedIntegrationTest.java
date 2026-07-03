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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class FeedIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void feedShowsPostsFromFollowedUsers() throws Exception {
        AuthResponse author = TestFixtures.registerUser(mockMvc, objectMapper, "feed_author");
        AuthResponse viewer = TestFixtures.registerUser(mockMvc, objectMapper, "feed_viewer");

        String authorId = getUserId(author);

        mockMvc.perform(post("/api/users/" + authorId + "/follow")
                        .header("Authorization", TestFixtures.bearer(viewer.accessToken())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", TestFixtures.bearer(author.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Feed post\",\"mediaUrls\":[]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/feed")
                        .header("Authorization", TestFixtures.bearer(viewer.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].content").value("Feed post"));
    }

    @Test
    void feedRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/feed"))
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
