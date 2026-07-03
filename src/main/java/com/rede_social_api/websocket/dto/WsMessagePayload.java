package com.rede_social_api.websocket.dto;

import com.rede_social_api.message.dto.response.MessageResponse;

import java.util.UUID;

public record WsMessagePayload(
        UUID conversationId,
        MessageResponse message
) {}
