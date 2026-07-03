package com.rede_social_api.message.dto.response;

import com.rede_social_api.user.dto.response.UserSummary;

import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        List<UserSummary> participants,
        MessageResponse lastMessage,
        long unreadCount
) {}
