package com.rede_social_api.follow.dto.response;

import com.rede_social_api.follow.entity.FollowStatus;

public record FollowActionResponse(
        FollowStatus status,
        String message
) {}
