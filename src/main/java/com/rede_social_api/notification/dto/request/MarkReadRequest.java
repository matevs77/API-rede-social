package com.rede_social_api.notification.dto.request;

import java.util.List;
import java.util.UUID;

public record MarkReadRequest(
        List<UUID> notificationIds
) {}
