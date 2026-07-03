package com.rede_social_api.notification.dto.response;

import com.rede_social_api.notification.entity.NotificationType;
import com.rede_social_api.user.dto.response.UserSummary;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        UserSummary actor,
        UUID referenceId,
        boolean read,
        Instant createdAt
) {}
