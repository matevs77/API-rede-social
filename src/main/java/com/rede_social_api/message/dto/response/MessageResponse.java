package com.rede_social_api.message.dto.response;

import com.rede_social_api.user.dto.response.UserSummary;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UserSummary sender,
        String content,
        Instant sentAt,
        Instant readAt
) {}
