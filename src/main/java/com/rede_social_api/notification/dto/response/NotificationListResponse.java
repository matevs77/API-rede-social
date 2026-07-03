package com.rede_social_api.notification.dto.response;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> items,
        String nextCursor,
        long unreadCount
) {}
