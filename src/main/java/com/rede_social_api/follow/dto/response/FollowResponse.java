package com.rede_social_api.follow.dto.response;

import com.rede_social_api.follow.entity.FollowStatus;
import com.rede_social_api.user.dto.response.UserSummary;

import java.time.Instant;

public record FollowResponse(
        UserSummary follower,
        UserSummary following,
        FollowStatus status,
        Instant createdAt
) {}
