package com.rede_social_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rede_social_api.auth.dto.response.AuthResponse;
import com.rede_social_api.like.entity.LikeTargetType;
import com.rede_social_api.like.repository.LikeRepository;
import com.rede_social_api.post.dto.response.PostResponse;
import com.rede_social_api.support.TestFixtures;
import com.rede_social_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class LikeCleanupIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void deletingPostRemovesAssociatedLikes() throws Exception {
        AuthResponse auth = TestFixtures.registerUser(mockMvc, objectMapper, "like_cleanup");

        var user = userRepository.findByUsername("user_like_cleanup").orElseThrow();

        String postBody = mockMvc.perform(post("/api/posts")
                        .header("Authorization", TestFixtures.bearer(auth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Post with likes\",\"mediaUrls\":[]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        PostResponse post = objectMapper.readValue(postBody, PostResponse.class);

        mockMvc.perform(post("/api/posts/" + post.id() + "/like")
                        .header("Authorization", TestFixtures.bearer(auth.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(1));

        assertTrue(likeRepository.findByUserIdAndTargetTypeAndTargetId(user.getId(), LikeTargetType.POST, post.id()).isPresent());

        mockMvc.perform(delete("/api/posts/" + post.id())
                        .header("Authorization", TestFixtures.bearer(auth.accessToken())))
                .andExpect(status().isNoContent());

        assertTrue(likeRepository.findByUserIdAndTargetTypeAndTargetId(user.getId(), LikeTargetType.POST, post.id()).isEmpty());
    }
}
