package com.rede_social_api.message.controller;

import com.rede_social_api.common.security.AuthenticatedUser;
import com.rede_social_api.common.security.CurrentUser;
import com.rede_social_api.message.dto.request.CreateConversationRequest;
import com.rede_social_api.message.dto.request.SendMessageRequest;
import com.rede_social_api.message.dto.response.ConversationResponse;
import com.rede_social_api.message.dto.response.MessageListResponse;
import com.rede_social_api.message.dto.response.MessageResponse;
import com.rede_social_api.message.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse createConversation(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody CreateConversationRequest request) {
        return messageService.createConversation(currentUser.getId(), request);
    }

    @GetMapping
    public List<ConversationResponse> listConversations(@CurrentUser AuthenticatedUser currentUser) {
        return messageService.listConversations(currentUser.getId());
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return messageService.sendMessage(conversationId, currentUser.getId(), request);
    }

    @GetMapping("/{conversationId}/messages")
    public MessageListResponse getMessages(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID conversationId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return messageService.getMessages(conversationId, currentUser.getId(), cursor, limit);
    }
}
