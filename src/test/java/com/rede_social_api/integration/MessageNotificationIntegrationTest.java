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
class MessageNotificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sendMessageCreatesConversationAndNotification() throws Exception {
        AuthResponse user1 = TestFixtures.registerUser(mockMvc, objectMapper, "msg_user1");
        AuthResponse user2 = TestFixtures.registerUser(mockMvc, objectMapper, "msg_user2");

        String user2Id = getUserId(user2);

        String convBody = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", TestFixtures.bearer(user1.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantId\":\"" + user2Id + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String conversationId = objectMapper.readTree(convBody).get("id").asText();

        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", TestFixtures.bearer(user1.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hello!"));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", TestFixtures.bearer(user2.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("MESSAGE"));
    }

    @Test
    void nonParticipantCannotSendMessage() throws Exception {
        AuthResponse user1 = TestFixtures.registerUser(mockMvc, objectMapper, "msg_p1");
        AuthResponse user2 = TestFixtures.registerUser(mockMvc, objectMapper, "msg_p2");
        AuthResponse outsider = TestFixtures.registerUser(mockMvc, objectMapper, "msg_out");

        String user2Id = getUserId(user2);
        String convBody = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", TestFixtures.bearer(user1.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantId\":\"" + user2Id + "\"}"))
                .andReturn().getResponse().getContentAsString();
        String conversationId = objectMapper.readTree(convBody).get("id").asText();

        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", TestFixtures.bearer(outsider.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Intruder\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void markNotificationsRead() throws Exception {
        AuthResponse follower = TestFixtures.registerUser(mockMvc, objectMapper, "notif_follower");
        AuthResponse target = TestFixtures.registerUser(mockMvc, objectMapper, "notif_target");

        mockMvc.perform(post("/api/users/" + getUserId(target) + "/follow")
                        .header("Authorization", TestFixtures.bearer(follower.accessToken())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", TestFixtures.bearer(target.accessToken())))
                .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", TestFixtures.bearer(target.accessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", TestFixtures.bearer(target.accessToken())))
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    private String getUserId(AuthResponse auth) throws Exception {
        String body = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", TestFixtures.bearer(auth.accessToken())))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("id").asText();
    }
}
