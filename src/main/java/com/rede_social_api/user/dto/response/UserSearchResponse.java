package com.rede_social_api.user.dto.response;

import java.util.UUID;

public record UserSearchResponse(
        UUID id,
        String username,
        String displayName,
        String avatarUrl
) {}
