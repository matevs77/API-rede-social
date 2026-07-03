package com.rede_social_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rede_social_api.auth.dto.response.AuthResponse;
import com.rede_social_api.post.dto.response.PostResponse;
import com.rede_social_api.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CommentLikeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void ownerCanCommentAndLike() throws Exception {
        AuthResponse owner = TestFixtures.registerUser(mockMvc, objectMapper, "comment_owner");
        AuthResponse other = TestFixtures.registerUser(mockMvc, objectMapper, "comment_other");

        String postBody = mockMvc.perform(post("/api/posts")
                        .header("Authorization", TestFixtures.bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Post for comments\",\"mediaUrls\":[]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        PostResponse post = objectMapper.readValue(postBody, PostResponse.class);

        mockMvc.perform(post("/api/posts/" + post.id() + "/comments")
                        .header("Authorization", TestFixtures.bearer(other.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Nice post!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Nice post!"));

        mockMvc.perform(post("/api/posts/" + post.id() + "/like")
                        .header("Authorization", TestFixtures.bearer(other.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(1));
    }

    @Test
    void otherUserCannotEditComment() throws Exception {
        AuthResponse owner = TestFixtures.registerUser(mockMvc, objectMapper, "comment_ed_owner");
        AuthResponse author = TestFixtures.registerUser(mockMvc, objectMapper, "comment_ed_author");
        AuthResponse other = TestFixtures.registerUser(mockMvc, objectMapper, "comment_ed_other");

        String postBody = mockMvc.perform(post("/api/posts")
                        .header("Authorization", TestFixtures.bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Post\",\"mediaUrls\":[]}"))
                .andReturn().getResponse().getContentAsString();
        PostResponse post = objectMapper.readValue(postBody, PostResponse.class);

        String commentBody = mockMvc.perform(post("/api/posts/" + post.id() + "/comments")
                        .header("Authorization", TestFixtures.bearer(author.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Original\"}"))
                .andReturn().getResponse().getContentAsString();

        String commentId = objectMapper.readTree(commentBody).get("id").asText();

        mockMvc.perform(patch("/api/posts/" + post.id() + "/comments/" + commentId)
                        .header("Authorization", TestFixtures.bearer(other.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hacked\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unlikePostDecrementsCount() throws Exception {
        AuthResponse user = TestFixtures.registerUser(mockMvc, objectMapper, "like_user");

        String postBody = mockMvc.perform(post("/api/posts")
                        .header("Authorization", TestFixtures.bearer(user.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Like test\",\"mediaUrls\":[]}"))
                .andReturn().getResponse().getContentAsString();
        PostResponse post = objectMapper.readValue(postBody, PostResponse.class);

        mockMvc.perform(post("/api/posts/" + post.id() + "/like")
                        .header("Authorization", TestFixtures.bearer(user.accessToken())))
                .andExpect(jsonPath("$.liked").value(true));

        mockMvc.perform(delete("/api/posts/" + post.id() + "/like")
                        .header("Authorization", TestFixtures.bearer(user.accessToken())))
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(0));
    }
}
