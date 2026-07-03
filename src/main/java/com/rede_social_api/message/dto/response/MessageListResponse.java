package com.rede_social_api.message.dto.response;

import java.util.List;

public record MessageListResponse(
        List<MessageResponse> items,
        String nextCursor
) {}
