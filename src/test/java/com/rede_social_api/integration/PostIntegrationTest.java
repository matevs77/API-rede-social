package com.rede_social_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rede_social_api.auth.dto.response.AuthResponse;
import com.rede_social_api.post.dto.request.CreatePostRequest;
import com.rede_social_api.post.dto.response.PostResponse;
import com.rede_social_api.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PostIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void ownerCanCreateAndUpdatePost() throws Exception {
        AuthResponse owner = TestFixtures.registerUser(mockMvc, objectMapper, "post_owner");

        CreatePostRequest createRequest = new CreatePostRequest("Hello world", List.of("https://img.test/a.png"));
        String createBody = mockMvc.perform(post("/api/posts")
                        .header("Authorization", TestFixtures.bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hello world"))
                .andReturn().getResponse().getContentAsString();

        PostResponse post = objectMapper.readValue(createBody, PostResponse.class);

        mockMvc.perform(patch("/api/posts/" + post.id())
                        .header("Authorization", TestFixtures.bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Updated\",\"mediaUrls\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated"));
    }

    @Test
    void unauthorizedUserCannotAccessProtectedPostEndpoint() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"x\",\"mediaUrls\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otherUserCannotDeletePost() throws Exception {
        AuthResponse owner = TestFixtures.registerUser(mockMvc, objectMapper, "post_owner2");
        AuthResponse other = TestFixtures.registerUser(mockMvc, objectMapper, "post_other");

        String createBody = mockMvc.perform(post("/api/posts")
                        .header("Authorization", TestFixtures.bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"secret\",\"mediaUrls\":[]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        PostResponse post = objectMapper.readValue(createBody, PostResponse.class);

        mockMvc.perform(delete("/api/posts/" + post.id())
                        .header("Authorization", TestFixtures.bearer(other.accessToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getNonExistentPostReturns404() throws Exception {
        AuthResponse user = TestFixtures.registerUser(mockMvc, objectMapper, "post_404");
        mockMvc.perform(get("/api/posts/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", TestFixtures.bearer(user.accessToken())))
                .andExpect(status().isNotFound());
    }
}
