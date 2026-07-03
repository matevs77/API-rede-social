package com.rede_social_api.websocket.dto;

import com.rede_social_api.notification.dto.response.NotificationResponse;

public record WsNotificationPayload(NotificationResponse notification) {}
